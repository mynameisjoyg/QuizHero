plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    //id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.joyg.quizhero"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.joyg.quizhero"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

// 導入 Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // 新增所需的 Firebase 產品（例如 Analytics），無需指定版本號
    implementation("com.google.firebase:firebase-analytics")


    // Use whichever versions of these dependencies suit your application.
    // The versions shown here were the latest versions as of March 14, 2025.
    //implementation("com.google.firebase:firebase-dataconnect:16.0.0-beta04")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")

    // These dependencies are not strictly required, but will very likely be used
    // when writing modern Android applications.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.firebase:firebase-firestore")
}