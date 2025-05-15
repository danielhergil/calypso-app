pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "CalypsoApp"
include(":app")
include(":common")
project(":common").projectDir = file("common")
include(":encoder")
project(":encoder").projectDir = file("encoder")
include(":extra-sources")
project(":extra-sources").projectDir = file("extra-sources")
include(":rtmp")
project(":rtmp").projectDir = file("rtmp")
include(":rtsp")
project(":rtsp").projectDir = file("rtsp")
include(":srt")
project(":srt").projectDir = file("srt")
include(":udp")
project(":udp").projectDir = file("udp")
include(":library")
project(":library").projectDir = file("library")
