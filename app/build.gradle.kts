import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// Load local.properties
val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(FileInputStream(localPropertiesFile))
}
val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
}

android {
    namespace = "com.example.unimove"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.unimove"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read API key from properties; throw error if not found
        val mapsKey = properties.getProperty("MAPS_API_KEY")
            ?: System.getenv("MAPS_API_KEY")
            ?: if (releaseBuildRequested) {
                error("MAPS_API_KEY is required for release builds.")
            } else {
                "CI_PLACEHOLDER_NOT_FOR_RELEASE"
            }

        // Pass to BuildConfig
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")

        // Pass to AndroidManifest
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey

        val mapboxAccessToken = properties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
        resValue("string", "mapbox_access_token", mapboxAccessToken)

        // Weather API Key
        val weatherKey = properties.getProperty("WEATHER_API_KEY") ?: ""
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherKey\"")

        // Meal API configuration
        val kaloApiKey = properties.getProperty("KALO_API_KEY") ?: ""
        val kaloApiBaseUrl = properties.getProperty("KALO_API_BASE_URL")
            ?.takeIf { it.isNotBlank() }
            ?: "https://api.kalori-api.my"
        val usdaFdcApiKey = properties.getProperty("USDA_FDC_API_KEY") ?: ""
        val usdaFdcApiBaseUrl = properties.getProperty("USDA_FDC_API_BASE_URL")
            ?.takeIf { it.isNotBlank() }
            ?: "https://api.nal.usda.gov/fdc/v1/"
        buildConfigField("String", "KALO_API_KEY", "\"$kaloApiKey\"")
        buildConfigField("String", "KALO_API_BASE_URL", "\"$kaloApiBaseUrl\"")
        buildConfigField("String", "USDA_FDC_API_KEY", "\"$usdaFdcApiKey\"")
        buildConfigField("String", "USDA_FDC_API_BASE_URL", "\"$usdaFdcApiBaseUrl\"")

        // Groq API Key
        val groqKey = properties.getProperty("GROQ_API_KEY") ?: ""
        buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
    }

    // ✅ Release signing config (from local.properties)
    val releaseStoreFile = properties.getProperty("RELEASE_STORE_FILE")
    val releaseStorePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    if (releaseBuildRequested && !releaseSigningConfigured) {
        error("Release signing credentials are required for release builds.")
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            // Use default debug signing for security
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Add SceneView Dependency
    implementation("io.github.sceneview:sceneview:3.0.0")


    //VICO
    implementation("com.patrykandpatrick.vico:compose:2.4.1")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Credential Manager (Google Sign-In)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Retrofit for Weather API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Use BOM for Compose dependencies to ensure version compatibility
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.mapbox.maps:android-ndk27:11.24.3")

    // Wear OS Data Layer
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.play.services.games)
    implementation(libs.cronet.embedded)

    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation("com.airbnb.android:lottie-compose:6.6.0")
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
