plugins {
    id("com.android.application")
}

apply(from = rootProject.file("signing.gradle.kts"))

android {
    namespace = "io.github.phutrung08.tidyo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.phutrung08.tidyo"
        minSdk = 26
        // targetSdk = 29 là CHỦ ĐÍCH. Tidyo TV chỉ phát hành qua GitHub (cài thủ công, không lên CH Play)
        // và cần quét/xoá file rác (.tmp, .log, thumbnail, .apk) trong bộ nhớ dùng chung. Từ target 30 trở lên
        // Android áp scoped storage nên ứng dụng không còn duyệt/xoá được file của ứng dụng khác; ở target 29
        // cờ requestLegacyExternalStorage trong manifest giữ nguyên quyền truy cập kiểu cũ trên Android 10.
        // Không nâng số này nếu chưa thiết kế lại phần quét file.
        targetSdk = 29
        versionCode = 1
        versionName = "1.0.0"
    }

    @Suppress("UNCHECKED_CAST")
    val releaseSigning = project.extra["releaseSigning"] as Map<String, String?>?
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

    lint {
        // CHỈ tắt hai kiểm tra phàn nàn về targetSdk cũ (xem lý do ở defaultConfig: bản GitHub cần
        // legacy storage nên cố ý giữ target 29). Mọi kiểm tra khác vẫn bật.
        disable += setOf("ExpiredTargetSdkVersion", "OldTargetApi")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":common"))
    testImplementation("junit:junit:4.13.2")
}
