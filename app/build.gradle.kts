import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Read TELEGRAM_API_ID / TELEGRAM_API_HASH from local.properties
val localProperties = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.invictus.smarttelegramfilter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.invictus.smarttelegramfilter"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "int", "TELEGRAM_API_ID",
            localProperties.getProperty("TELEGRAM_API_ID", "0")
        )
        buildConfigField(
            "String", "TELEGRAM_API_HASH",
            "\"${localProperties.getProperty("TELEGRAM_API_HASH", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // Required for TDLib native libs
        jniLibs { useLegacyPackaging = true }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Material Components (Required for XML themes using Theme.MaterialComponents)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines + WorkManager
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)

    // TDLib — drop tdlib.jar into app/libs/
    //         and libtdjni.so into app/src/main/jniLibs/<ABI>/
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}

// ── TDLib setup task ──────────────────────────────────────────────────────────
// Run with:  ./gradlew setupTdlib
tasks.register<Exec>("setupTdlib") {
    group = "setup"
    description = "Build TDLib for Android via Docker and install artifacts into app/."
    
    val scriptsDir = rootProject.file("scripts")
    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    
    if (isWindows) {
        commandLine(
            "powershell", "-ExecutionPolicy", "-Bypass",
            "-File", file("$scriptsDir/setup_tdlib.ps1").absolutePath
        )
    } else {
        commandLine("bash", file("$scriptsDir/setup_tdlib.sh").absolutePath)
    }
}
