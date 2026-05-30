plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.seuplayer.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.seuplayer.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")    implementation("androidx.compose.material:material-icons-extended")    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")



    
    testImplementation("junit:junit:4.13.2")
    android    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")    implementation("androidx.compose.material:material-icons-extended")    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


TestImplementation("androidx.test.ext:junit:1.1.5")
    android    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")    implementation("androidx.compose.material:material-icons-extended")    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


TestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    android    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")    implementation("androidx.compose.material:material-icons-extended")    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


TestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    android    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")    implementation("androidx.compose.material:material-icons-extended")    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


TestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

