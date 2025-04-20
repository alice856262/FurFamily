import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("kotlin-android")
    id("dagger.hilt.android.plugin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

android {
    namespace = "com.example.furfamily"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.furfamily"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Load secrets from properties file
        val secrets = Properties()
        val secretsFile = rootProject.file("secrets.properties")
        if (secretsFile.exists()) {
            secrets.load(FileInputStream(secretsFile))
        }

        // Add API keys to BuildConfig
        buildConfigField("String", "OPENAI_API_KEY", "\"${secrets["OPENAI_API_KEY"]}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${secrets["GOOGLE_MAPS_API_KEY"]}\"")

        // Add API key to manifest placeholders
        manifestPlaceholders["googleMapsApiKey"] = secrets["GOOGLE_MAPS_API_KEY"] as String
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    buildToolsVersion = "34.0.0"
}

dependencies {
    // Core Libraries
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material:1.6.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.6")
    implementation("androidx.browser:browser:1.8.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")

    // Hilt dependencies
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // Hilt ViewModel extension
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.coil-kt:coil-compose:2.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage:20.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    implementation("com.google.firebase:firebase-database:21.0.0")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-places:17.0.0")
    implementation("com.google.android.libraries.places:places:3.2.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:1.4.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.api-client:google-api-client-gson:1.32.1")
    implementation("com.google.api-client:google-api-client-android:1.32.1")
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")
    implementation("com.google.http-client:google-http-client-android:1.40.1")
    implementation("com.google.http-client:google-http-client-gson:1.40.1")

    // ML Kit (New ML Kit libraries independent from Firebase)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")  // Example for Barcode Scanning
    implementation("com.google.mlkit:image-labeling:17.0.7")   // Example for Image Labeling
    implementation("com.google.mlkit:face-detection:16.1.7")   // Example for Face Detection

    // Test Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
