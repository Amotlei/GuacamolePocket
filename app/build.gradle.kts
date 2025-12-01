plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Aplica el plugin KAPT de forma explícita para Kotlin DSL:
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.guacamolepocket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.guacamolepocket"
        minSdk = 34
        targetSdk = 36
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    // ---------- CORE ANDROID / COMPOSE ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ---------- LIFECYCLE ----------
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // ViewModel + coroutines support
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // ---------- ROOM (persistencia local) ----------
    implementation("androidx.room:room-runtime:2.6.1") // VERSION UNIFICADA
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // REQUIERE plugin 'org.jetbrains.kotlin.kapt' (aplicado arriba)

    // ---------- COROUTINES ----------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ---------- PARSE (Back4App) y LIVEQUERY ----------
    // Parse SDK (módulo 'parse' del repo Parse-SDK-Android en JitPack)
    implementation("com.github.parse-community.Parse-SDK-Android:parse:4.1.0")
    // Cliente LiveQuery (usa la release 1.2.2 que existe en el repo)
    implementation("com.github.parse-community:ParseLiveQuery-Android:1.2.2")



    // ---------- TESTS ----------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
