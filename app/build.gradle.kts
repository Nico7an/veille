plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Fournis par le workflow GitHub (secrets), jamais versionnés.
val version = (System.getenv("VEILLE_VERSION") ?: "0.0.1").removePrefix("v")
val numero = System.getenv("VEILLE_NUMERO")?.toIntOrNull() ?: 1

android {
    namespace = "io.github.nico7an.veille"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.nico7an.veille"
        minSdk = 28
        targetSdk = 35
        versionCode = numero
        versionName = version
    }

    signingConfigs {
        create("release") {
            val fichier = System.getenv("VEILLE_KEYSTORE")
            if (fichier != null) {
                storeFile = file(fichier)
                storePassword = System.getenv("VEILLE_KEYSTORE_MDP")
                keyAlias = "veille"
                keyPassword = System.getenv("VEILLE_KEYSTORE_MDP")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (System.getenv("VEILLE_KEYSTORE") != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
