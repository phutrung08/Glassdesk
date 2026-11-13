plugins {
    id("com.android.application")
}

apply(from = rootProject.file("signing.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val releaseSigning = project.extra["releaseSigning"] as Map<String, String?>?

android {
    namespace = "io.github.phutrung08.glassdeck"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.phutrung08.glassdeck"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    if (releaseSigning != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseSigning["storeFile"]!!)
                storePassword = releaseSigning["storePassword"]
                keyAlias = releaseSigning["keyAlias"]
                keyPassword = releaseSigning["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseSigning != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":common"))
    testImplementation("junit:junit:4.13.2")
}
