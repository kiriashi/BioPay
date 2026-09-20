plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Mainstream signing scheme: private key material comes from environment
// variables (CI secrets) or an untracked local.properties file, never from
// the repository. When neither is present, the release build gracefully
// falls back to an unsigned APK instead of failing (CorePatch pattern).
val signProps = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.let { FileInputStream(it).use { fis -> signProps.load(fis) } }

fun signingValue(environmentName: String, propertyName: String, defaultValue: String = ""): String =
    providers.environmentVariable(environmentName).orNull
        ?: signProps.getProperty(propertyName, defaultValue)

android {
    namespace = "io.github.kiriashi.biopay"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.kiriashi.biopay"
        minSdk = 28
        targetSdk = 35
        versionCode = 260920
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(signingValue("BIOPAY_RELEASE_STORE_FILE", "RELEASE_STORE_FILE", "../biopay.keystore"))
            storePassword = signingValue("BIOPAY_RELEASE_STORE_PASSWORD", "RELEASE_STORE_PASSWORD")
            keyAlias = signingValue("BIOPAY_RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS")
            keyPassword = signingValue("BIOPAY_RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // Graceful degradation: without key material the build yields an
            // unsigned APK instead of failing (same pattern as CorePatch).
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile?.exists() == true }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    testImplementation("junit:junit:4.13.2")
}
