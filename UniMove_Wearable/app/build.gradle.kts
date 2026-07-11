import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Prefer the phone project's signing properties. Data Layer peers must have the
// same application id and signing certificate.
val releasePropertiesFile = sequenceOf(
    rootProject.file("../local.properties"),
    rootProject.file("local.properties"),
).firstOrNull { file -> file.exists() && file.readText().contains("RELEASE_STORE_FILE=") }
    ?: error("Release signing properties were not found in the phone or wearable local.properties")
val releaseProperties = Properties().apply {
    load(FileInputStream(releasePropertiesFile))
}
val releaseSigningKeys = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD",
)
releaseSigningKeys.forEach { key ->
    require(!releaseProperties.getProperty(key).isNullOrBlank()) {
        "$key is required to sign the wearable with the phone release certificate"
    }
}

android {
    namespace = "com.example.unimove_wearable"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.unimove"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(releaseProperties.getProperty("RELEASE_STORE_FILE"))
            storePassword = releaseProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = releaseProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = releaseProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // The phone uses the same standard user debug keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.health.services.client)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.core)
    implementation(libs.androidx.wear)
    implementation(libs.wear.ongoing)
    implementation(libs.androidx.fragment)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
