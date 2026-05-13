plugins {
    id("com.android.application")
}

android {
    namespace = "com.frank.vibetap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.frank.vibetap"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
