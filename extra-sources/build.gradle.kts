// extra-sources/build.gradle.kts

plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "com.pedro.extrasources"
  compileSdk = 35

  defaultConfig {
    minSdk = 21
    lint.targetSdk = 35
  }
  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  // Kotlin coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  // AndroidX AppCompat
  implementation("androidx.appcompat:appcompat:1.6.1")
  // CameraX (core, camera2, lifecycle)
  implementation("androidx.camera:camera-core:1.4.2")
  implementation("androidx.camera:camera-camera2:1.4.2")
  implementation("androidx.camera:camera-lifecycle:1.4.2")
  // UVC Android (com.herohan:UVCAndroid)
  implementation("com.herohan:UVCAndroid:1.0.9")
  // ExoPlayer via Media3
  implementation("androidx.media3:media3-exoplayer:1.6.1")
  // Our local encoder module
  api(project(":encoder"))
  // Testing
  testImplementation("junit:junit:4.13.2")
}
