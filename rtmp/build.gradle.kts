plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "com.pedro.rtmp"
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
  // Testing
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  // Core common module
  api(project(":common"))
}
