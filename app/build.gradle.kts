plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jarvis.wakeword"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.jarvis.wakeword"
        minSdk = 26
        targetSdk = 30
        versionCode = 2
        versionName = "1.1"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("JARVIS_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("JARVIS_KEY_ALIAS").orEmpty()
                keyPassword = System.getenv("JARVIS_KEY_PASSWORD").orEmpty()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
    lint {
        // This build is sideloaded for the Android 11 target device, not distributed through Google Play.
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.github.msnilsen:openwakeword-android:b49f3ab14cf558ac0c2aef73a5e6c83da7a08558")

    testImplementation("junit:junit:4.13.2")
}
