plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.pedro.common"
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
    implementation("io.ktor:ktor-network:2.3.13")
    implementation("io.ktor:ktor-network-tls:2.3.13")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}