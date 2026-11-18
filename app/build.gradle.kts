plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Imzolash kaliti faqat yig'ish serverida (maxfiy sozlamalardan) beriladi.
val ksPath: String? = System.getenv("KEYSTORE_PATH")
val ksPass: String? = System.getenv("KEYSTORE_PASSWORD")

android {
    namespace = "uz.sozqalqoni"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.sozqalqoni"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.1"
    }

    signingConfigs {
        if (ksPath != null && ksPass != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = "sozqalqoni"
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksPath != null && ksPass != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
