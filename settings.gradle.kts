pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Glassdeck"

include(":common")      // Giao diện kính và tiện ích dùng chung
include(":glassdeck")   // Glassdeck Launcher TV (phát hành CH Play)
include(":tidyo")       // Tidyo TV (dọn dẹp, phát hành GitHub)
