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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Aliyun mirror: needed for com.arthenica:ffmpeg-kit-* which is no longer on Maven Central
    // (the project was retired upstream). Kept last so it never shadows google()/mavenCentral().
    maven { url = uri("https://maven.aliyun.com/repository/public") }
  }
}

rootProject.name = "Dream AI"

include(":app")
