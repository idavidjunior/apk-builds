// ============================================================
// MP3 PLAYER - Código Kotlin Completo para Compilação via GitHub Actions
// Arquivo: MainActivity.kt
// Package: com.seuplayer.app
// ============================================================

package com.seuplayer.app
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.Serializable

// ============================================================
// MODELO DE DADOS - Serializable para Intent
// ============================================================
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long = 0L
) : Serializable

// ============================================================
// MUSICSERVICE - Serviço de reprodução em foreground
// ============================================================
class MusicService : android.app.Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var isPlaying: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    companion object {
        const val ACTION_PLAY = "com.seuplayer.PLAY"
        const val ACTION_PAUSE = "com.seuplayer.PAUSE"
        const val ACTION_RESUME = "com.seuplayer.RESUME"
        const val ACTION_NEXT = "com.seuplayer.NEXT"
        const val ACTION_PREVIOUS = "com.seuplayer.PREVIOUS"
        const val ACTION_SEEK = "com.seuplayer.SEEK"
        const val EXTRA_PLAYLIST = "playlist"
        const val EXTRA_INDEX = "index"
        const val EXTRA_SEEK_POSITION = "seek_position"
        const val BROADCAST_STATUS = "com.seuplayer.STATUS"
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener { playNext() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                @Suppress("UNCHECKED_CAST")
                val songs = intent.getSerializableExtra(EXTRA_PLAYLIST) as? List<Song>
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                if (songs != null && songs.isNotEmpty()) {
                    playlist = songs
                    playSong(index)
                }
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(pos)
                }
            }
        }
        return START_STICKY
    }

    private fun playSong(index: Int) {
        if (index < 0 || index >= playlist.size) return
        currentIndex = index
        val song = playlist[index]
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(song.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            isPlaying = true
            startProgressUpdates()
            broadcastStatus()
        } catch (e: Exception) {
            Log.e("MusicService", "Erro ao tocar: ${e.message}")
            playNext()
        }
    }

    private fun pause() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            stopProgressUpdates()
            broadcastStatus()
        }
    }

    private fun resume() {
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying && currentIndex != -1) {
            mediaPlayer.start()
            isPlaying = true
            startProgressUpdates()
            broadcastStatus()
        }
    }

    private fun playNext() {
        if (playlist.isEmpty()) return
        val next = (currentIndex + 1) % playlist.size
        playSong(next)
    }

    private fun playPrevious() {
        if (playlist.isEmpty()) return
        val prev = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        playSong(prev)
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressRunnable = object : Runnable {
            override fun run() {
                if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    broadcastStatus()
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun broadcastStatus() {
        if (!::mediaPlayer.isInitialized || currentIndex == -1 || currentIndex >= playlist.size) return
        val intent = Intent(BROADCAST_STATUS).apply {
            putExtra("currentIndex", currentIndex)
            putExtra("isPlaying", isPlaying)
            putExtra("currentPosition", mediaPlayer.currentPosition)
            putExtra("duration", if (mediaPlayer.duration > 0) mediaPlayer.duration else playlist[currentIndex].duration.toInt())
            putExtra("songTitle", playlist[currentIndex].title)
            putExtra("songArtist", playlist[currentIndex].artist)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopProgressUpdates()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// ============================================================
// MAINACTIVITY - Interface com Jetpack Compose
// ============================================================
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            Log.w("MainActivity", "Permissões negadas. O app pode não funcionar.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            MaterialTheme {
                MP3PlayerApp()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            requestPermissionLauncher.launch(permissions)
        }
    }
}

// ============================================================
// UI COMPOSE
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MP3PlayerApp() {
    val context = LocalContext.current
    val songs = remember { loadSongs(context) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var songTitle by remember { mutableStateOf("") }
    var songArtist by remember { mutableStateOf("") }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    currentIndex = it.getIntExtra("currentIndex", -1)
                    isPlaying = it.getBooleanExtra("isPlaying", false)
                    currentPosition = it.getIntExtra("currentPosition", 0).toLong()
                    duration = it.getIntExtra("duration", 0).toLong()
                    songTitle = it.getStringExtra("songTitle") ?: ""
                    songArtist = it.getStringExtra("songArtist") ?: ""
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(MusicService.BROADCAST_STATUS))
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    fun playSong(index: Int) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY
            putExtra(MusicService.EXTRA_PLAYLIST, ArrayList(songs))
            putExtra(MusicService.EXTRA_INDEX, index)
        }
        context.startService(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MP3 Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhuma música encontrada no dispositivo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(songs) { song ->
                        val index = songs.indexOf(song)
                        ListItem(
                            headlineContent = {
                                Text(
                                    song.title.ifEmpty { "Título desconhecido" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(song.artist.ifEmpty { "Artista desconhecido" })
                            },
                            modifier = Modifier.clickable { playSong(index) }
                        )
                    }
                }
            }

            if (currentIndex in songs.indices) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = songTitle.ifEmpty { "Nenhuma música" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = songArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * duration).toInt()
                                Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_SEEK
                                    putExtra(MusicService.EXTRA_SEEK_POSITION, seekTo)
                                }.also { context.startService(it) }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_PREVIOUS
                                }.also { context.startService(it) }
                            }) {
                                Icon(Icons.Default.SkipPrevious, "Anterior")
                            }

                            IconButton(onClick = {
                                Intent(context, MusicService::class.java).apply {
                                    action = if (isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_RESUME
                                }.also { context.startService(it) }
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pausar" else "Tocar"
                                )
                            }

                            IconButton(onClick = {
                                Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_NEXT
                                }.also { context.startService(it) }
                            }) {
                                Icon(Icons.Default.SkipNext, "Próxima")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// FUNÇÃO PARA CARREGAR MÚSICAS DO DISPOSITIVO
// ============================================================
fun loadSongs(context: Context): List<Song> {
    val songs = mutableListOf<Song>()
    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
    val cursor = resolver.query(collection, projection, selection, null, sortOrder)
    cursor?.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val title = it.getString(titleCol) ?: "Desconhecido"
            val artist = it.getString(artistCol) ?: "Artista desconhecido"
            val path = it.getString(dataCol) ?: continue
            val duration = it.getLong(durationCol)
            songs.add(Song(id, title, artist, path, duration))
        }
    }
    return songs
}
