// library/build.gradle.kts

plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "com.pedro.library"
  compileSdk = 35

  defaultConfig {
    minSdk = 16
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
  // Core sub-modules
  api(project(":encoder"))
  api(project(":rtmp"))
  api(project(":rtsp"))
  api(project(":srt"))
  api(project(":udp"))
  api(project(":common"))
}
