plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler) // replaces composeOptions block
    kotlin("kapt")
}

android {
    namespace = "com.example.testflowbank"
    compileSdk = 36   // required for latest core-ktx 1.17.0, etc.  [oai_citation:15‡mvnrepository.com](https://mvnrepository.com/artifact/androidx.core/core-ktx)

    defaultConfig {
        applicationId = "com.example.testflowbank"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    // With Kotlin 2.x + compose compiler plugin, you **don’t** need composeOptions
    // composeOptions { ... }  <-- remove old kotlinCompilerExtensionVersion

    kotlinOptions {
        jvmTarget = "1.8"        // replace deprecated jvmTarget=1.8
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.material.icons)

    // Activity + Navigation Compose
    implementation(libs.activity.compose)
    implementation(libs.nav.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Network (Retrofit 3 + Moshi + OkHttp 5)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)

    // AI Agents and Media pipe
    implementation(libs.localagents.rag)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.kotlinx.coroutines.guava)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}