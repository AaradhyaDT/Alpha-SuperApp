import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gradle.semantic.build.versioning)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.alpha"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alpha"
        minSdk = 26
        targetSdk = 36
        versionCode = semanticBuildVersioning.versionCode
        versionName = semanticBuildVersioning.versionName

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "your_gemini_api_key_here")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "task"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
}

semanticBuildVersioning {
    // Starting version if no tags exist; otherwise uses latest tag
    startingVersion = "1.1.1"
    // Other configurations can be added here
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    debugImplementation(libs.androidx.ui.tooling)
}
