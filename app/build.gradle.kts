plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mouajmd.fluxcore"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mouajmd.fluxcore"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "12.0"
    }

    // Do not remove or modify these signingConfigs. They are necessary for building, installing,
    // and updating Android apps in AI Studio and GitHub Actions.
    signingConfigs {
        create("release") {
            val keystorePath =
                System.getenv("SIGNING_KEYSTORE_PATH")
                    ?: System.getenv("KEYSTORE_FILE")
                    ?: System.getenv("KEYSTORE_PATH")
                    ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword =
                System.getenv("SIGNING_STORE_PASSWORD")
                    ?: System.getenv("KEYSTORE_PASSWORD")
                    ?: System.getenv("STORE_PASSWORD")
            keyAlias =
                System.getenv("SIGNING_KEY_ALIAS")?.ifBlank { null }
                    ?: System.getenv("KEY_ALIAS")?.ifBlank { null }
                    ?: "upload"
            keyPassword =
                System.getenv("SIGNING_KEY_PASSWORD")
                    ?: System.getenv("KEY_PASSWORD")
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // Do not remove or modify this signingConfig assignment.
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Do not remove or modify this signingConfig assignment.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    // Unused in production builds: implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}

if (System.getenv("GITHUB_ACTIONS") == "true") {
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
    val ciVersionCode = 10 + runNumber
    val ciVersionName = System.getenv("RELEASE_VERSION")
        ?.trim()
        ?.removePrefix("v")
        ?.takeIf { it.isNotBlank() }
        ?: "dev-${System.getenv("GITHUB_SHA")?.take(7) ?: "local"}"

    androidComponents.onVariants { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(ciVersionCode)
            output.versionName.set(ciVersionName)
        }
    }
}
