import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.watchlist.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.watchlist.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // TMDB API Key - reemplazá con tu key de https://www.themoviedb.org/settings/api
        buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY")}\"")
        // NewsAPI Key - reemplazá con tu key de https://newsapi.org/
        buildConfigField("String", "NEWS_API_KEY", "\"${localProperties.getProperty("NEWS_API_KEY")}\"")
        // MAL API Key - remplazá con tu key de https://myanimelist.net/apiconfig
        buildConfigField("String", "MAL_CLIENT_ID", "\"${localProperties.getProperty("MAL_CLIENT_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Retrofit + Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    // Coil (imágenes)
    implementation(libs.coil.compose)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    debugImplementation(libs.androidx.ui.tooling)
}
