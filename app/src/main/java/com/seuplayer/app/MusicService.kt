# ============================================================
# MP3 PLAYER - Aplicativo Android Completo em Kotlin
# Salvar como: MP3Player.kt
# Compilar com: Compilador APK v6.0
# ============================================================
# Este código implementa um reprodutor MP3 completo para Android
# com os seguintes recursos:
# - Listagem de músicas do dispositivo
# - Play, Pause, Next, Previous
# - Barra de progresso (seek)
# - Reprodução em segundo plano (Service)
# - Interface com Jetpack Compose
# - Permissões de leitura de mídia
# ============================================================

package com.seuplayer.app
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val albumArtUri: String? = null
) : Serializable

class MusicService : android.app.Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private var currentSongIndex = -1
    private var playlist: List<Song> = emptyList()
    private var isPlaying = false
    private var handler: Handler? = null
    private var updateSeekBarRunnable: Runnable? = null

    companion object {
        const val ACTION_PLAY = "com.seuplayer.PLAY"
        const val ACTION_PAUSE = "com.seuplayer.PAUSE"
        const val ACTION_RESUME = "com.seuplayer.RESUME"
        const val ACTION_NEXT = "com.seuplayer.NEXT"
        const val ACTION_PREVIOUS = "com.seuplayer.PREVIOUS"
        const val ACTION_SEEK = "com.seuplayer.SEEK"
        const val EXTRA_SEEK_POSITION = "seek_position"
        const val EXTRA_PLAYLIST = "playlist"
        const val EXTRA_INDEX = "index"
        const val BROADCAST_STATUS = "com.seuplayer.STATUS"
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener { playNext() }
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                @Suppress("UNCHECKED_CAST")
                val songs = intent.getSerializableExtra(EXTRA_PLAYLIST) as? List<Song> ?: return START_NOT_STICKY
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                playlist = songs
                playSong(index)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                mediaPlayer.seekTo(pos)
                broadcastStatus()
            }
        }
        return START_STICKY
    }

    private fun playSong(index: Int) {
        if (index < 0 || index >= playlist.size) return
        currentSongIndex = index
        val song = playlist[index]
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(song.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            isPlaying = true
            startSeekBarUpdates()
            broadcastStatus()
        } catch (e: Exception) {
            Log.e("MusicService", "Erro ao tocar: ${e.message}")
            playNext()
        }
    }

    private fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            stopSeekBarUpdates()
            broadcastStatus()
        }
    }

    private fun resume() {
        if (!mediaPlayer.isPlaying && currentSongIndex != -1) {
            mediaPlayer.start()
            isPlaying = true
            startSeekBarUpdates()
            broadcastStatus()
        }
    }

    private fun playNext() {
        if (playlist.isEmpty()) return
        val next = (currentSongIndex + 1) % playlist.size
        playSong(next)
    }

    private fun playPrevious() {
        if (playlist.isEmpty()) return
        val prev = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
        playSong(prev)
    }

    private fun startSeekBarUpdates() {
        stopSeekBarUpdates()
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying) {
                    broadcastStatus()
                    handler?.postDelayed(this, 500)
                }
            }
        }
        handler?.post(updateSeekBarRunnable!!)
    }

    private fun stopSeekBarUpdates() {
        updateSeekBarRunnable?.let { handler?.removeCallbacks(it) }
    }

    private fun broadcastStatus() {
        if (currentSongIndex == -1) return
        val intent = Intent(BROADCAST_STATUS).apply {
            putExtra("isPlaying", isPlaying)
            putExtra("currentIndex", currentSongIndex)
            putExtra("currentPosition", mediaPlayer.currentPosition)
            putExtra("duration", mediaPlayer.duration)
            putExtra("songTitle", playlist[currentSongIndex].title)
            putExtra("artist", playlist[currentSongIndex].artist)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopSeekBarUpdates()
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        setContent {
            MP3PlayerApp()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MP3PlayerApp() {
    val context = LocalContext.current
    var songs by remember { mutableStateOf(loadSongs(context)) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentIndex = intent.getIntExtra("currentIndex", -1)
                isPlaying = intent.getBooleanExtra("isPlaying", false)
                currentPosition = intent.getIntExtra("currentPosition", 0).toLong()
                duration = intent.getIntExtra("duration", 0).toLong()
                title = intent.getStringExtra("songTitle") ?: ""
                artist = intent.getStringExtra("artist") ?: ""
            }
        }
        val filter = IntentFilter(MusicService.BROADCAST_STATUS)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("MP3 Player") }) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(Modifier.weight(1f)) {
                items(songs) { song ->
                    val index = songs.indexOf(song)
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(song.artist) },
                        modifier = Modifier.clickable {
                            val intent = Intent(context, MusicService::class.java).apply {
                                action = MusicService.ACTION_PLAY
                                putExtra(MusicService.EXTRA_PLAYLIST, ArrayList(songs))
                                putExtra(MusicService.EXTRA_INDEX, index)
                            }
                            context.startService(intent)
                        }
                    )
                }
            }

            if (currentIndex != -1) {
                Card(Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(artist, style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * duration).toInt()
                                val seekIntent = Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_SEEK
                                    putExtra(MusicService.EXTRA_SEEK_POSITION, seekTo)
                                }
                                context.startService(seekIntent)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Row(horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = {
                                context.startService(Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_PREVIOUS
                                })
                            }) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior") }

                            IconButton(onClick = {
                                context.startService(Intent(context, MusicService::class.java).apply {
                                    action = if (isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_RESUME
                                })
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Tocar"
                                )
                            }

                            IconButton(onClick = {
                                context.startService(Intent(context, MusicService::class.java).apply {
                                    action = MusicService.ACTION_NEXT
                                })
                            }) { Icon(Icons.Default.SkipNext, contentDescription = "Próxima") }
                        }
                    }
                }
            }
        }
    }
}

fun loadSongs(context: Context): List<Song> {
    val songs = mutableListOf<Song>()
    val resolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val cursor = resolver.query(uri, projection, selection, null, null)
    cursor?.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val title = it.getString(titleCol) ?: "Desconhecido"
            val artist = it.getString(artistCol) ?: "Artista desconhecido"
            val path = it.getString(dataCol) ?: continue
            songs.add(Song(id, title, artist, path))
        }
    }
    return songs.sortedBy { it.title }
}






















