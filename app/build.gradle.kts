import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Load signing properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.lotogenerator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lotogenerator"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.register("renameDebugApk") {
    dependsOn("assembleDebug")

    doLast {
        val apkDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val oldApk = apkDir.resolve("app-debug.apk")
        val newApk = apkDir.resolve("Furan-Lotto-Generator.apk")

        if (oldApk.exists()) {
            oldApk.renameTo(newApk)
            println("APK renamed to: ${newApk.absolutePath}")
        } else {
            println("Debug APK not found: ${oldApk.absolutePath}")
        }
    }
}

tasks.register("renameReleaseApk") {
    dependsOn("assembleRelease")

    doLast {
        val apkDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val oldApk = apkDir.resolve("app-release.apk")
        val newApk = apkDir.resolve("Furan-Lotto-Generator-v${android.defaultConfig.versionName}.apk")

        if (oldApk.exists()) {
            oldApk.renameTo(newApk)
            println("APK renamed to: ${newApk.absolutePath}")
        } else {
            println("Release APK not found: ${oldApk.absolutePath}")
        }
    }
}