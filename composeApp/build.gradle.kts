import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                
                // KMM Libraries
                implementation(libs.okio)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.multiplatform.settings)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)

                // Android-specific Koin Compose helpers
                implementation(libs.koin.androidx.compose)

                // Platform-specific KMM dependencies
                implementation(libs.sqldelight.android.driver)
                implementation(libs.ktor.client.okhttp)

                // Navigation - Android only
                implementation(libs.androidx.navigation.compose)

                // Media3/ExoPlayer for audio playback (following V2 proven architecture)
                implementation("androidx.media3:media3-exoplayer:1.3.1")
                implementation("androidx.media3:media3-session:1.3.1")
                implementation("androidx.media3:media3-ui:1.3.1")
            }
        }
        
        val iosMain by creating {
            dependencies {
                // Platform-specific KMM dependencies
                implementation(libs.sqldelight.native.driver)
                implementation(libs.ktor.client.darwin)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.grateful.deadly"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Load version properties - single source of truth for Android and iOS versions
    val versionPropsFile = rootProject.file("version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { versionProps.load(it) }
    }

    // Load keystore properties for release signing
    val keystorePropertiesFile = rootProject.file(".secrets/keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    defaultConfig {
        applicationId = "com.grateful.deadly"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionProps.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = versionProps.getProperty("VERSION_NAME") ?: "1.0.0"

        // Phone-only targeting: prevent other libraries from declaring tablet support
        manifestPlaceholders["supportsTablet"] = "false"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

sqldelight {
    databases {
        create("Database") {
            packageName = "com.grateful.deadly.database"
        }
    }
}

