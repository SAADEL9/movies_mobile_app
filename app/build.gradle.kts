import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.saad.moviessaad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saad.moviessaad"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY") ?: ""}\"")
        buildConfigField("String", "TMDB_BASE_URL", "\"${localProperties.getProperty("TMDB_BASE_URL") ?: ""}\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"${localProperties.getProperty("TMDB_IMAGE_BASE_URL") ?: ""}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${localProperties.getProperty("OPENROUTER_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENROUTER_BASE_URL", "\"${localProperties.getProperty("OPENROUTER_BASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY") ?: ""}\"")
        buildConfigField("String", "OLLAMA_BASE_URL", "\"${localProperties.getProperty("OLLAMA_BASE_URL") ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
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
    // Prevent AAPT from compressing .tflite model so TFLite can load it from assets
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    
    implementation(libs.glide)
    implementation(libs.androidx.camera.core)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.osmdroid)
    implementation(libs.play.services.location)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.gotrue)
    implementation(libs.ktor.client.android)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sceneview)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // TensorFlow Lite
    implementation(libs.tflite)
    implementation(libs.tflite.support) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    // ML Kit
    implementation(libs.mlkit.face.detection)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
