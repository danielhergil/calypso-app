plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "com.pedro.encoder"
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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  api("androidx.annotation:annotation:1.9.1")
  api(project(":common"))
  testImplementation("junit:junit:4.13.2")
}
