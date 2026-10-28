import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Deployment identifiers stay in ignored local configuration or the CI environment.
val clarityLocalProperties = Properties().apply {
    val configuration = rootProject.file("local.properties")
    if (configuration.isFile) configuration.inputStream().use(::load)
}
val clarityProjectId = providers.environmentVariable("CALCMOT_CLARITY_PROJECT_ID")
    .orElse(clarityLocalProperties.getProperty("clarity.projectId", ""))
    .get().trim()
require(clarityProjectId.isEmpty() || clarityProjectId.matches(Regex("[a-z0-9]+"))) {
    "Invalid Clarity project configuration."
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val buildsAppBundle = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("bundle", ignoreCase = true)
}
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.exists()) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "br.com.calcmot"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "br.com.calcmot"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "3.4"
        buildConfigField("String", "CLARITY_PROJECT_ID", "\"$clarityProjectId\"")
        buildConfigField("String", "ACCOUNT_API_BASE_URL", "\"\"")
        buildConfigField("boolean", "LEGACY_INTAKE_ENABLED", "false")

        if (buildsAppBundle) {
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(releaseKeystoreProperties["storeFile"] as String)
                storePassword = releaseKeystoreProperties["storePassword"] as String
                keyAlias = releaseKeystoreProperties["keyAlias"] as String
                keyPassword = releaseKeystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            if (releaseKeystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseKeystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("identityDev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".identitydev"
            matchingFallbacks += listOf("debug")
            val workerUrl = providers.gradleProperty("calcmot.identityDevWorkerUrl").orElse("").get().trim()
            require(workerUrl.isEmpty() || workerUrl.matches(Regex("https://calcmot-api-dev\\.[a-z0-9-]+\\.workers\\.dev/?"))) {
                "identityDev requires a verified development Worker URL, never a production API."
            }
            buildConfigField("String", "IDENTITY_DEV_WORKER_URL", "\"$workerUrl\"")
            buildConfigField("String", "ACCOUNT_API_BASE_URL", "\"$workerUrl\"")
            val intakeEnabled = providers.gradleProperty("calcmot.identityDevLegacyIntake").orNull == "true"
            require(!intakeEnabled || workerUrl.isNotEmpty()) { "DEV intake requires an explicit DEV Worker URL" }
            buildConfigField("boolean", "LEGACY_INTAKE_ENABLED", intakeEnabled.toString())
        }
    }
    sourceSets.getByName("identityDev") {
        java.srcDir("src/debug/java")
        kotlin.directories += "src/debug/java"
        res.srcDir("src/debug/res")
    }
    // Explicit opt-in for device tests on the isolated package; default instrumentation is unchanged.
    if (providers.gradleProperty("calcmot.identityDevInstrumentation").orNull == "true") {
        testBuildType = "identityDev"
    }

    splits {
        abi {
            // Play handles ABI delivery for AABs. APK builds stay split to keep sideloads small.
            isEnable = !buildsAppBundle
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation("com.microsoft.clarity:clarity-compose:3.10.0")
    implementation("androidx.camera:camera-core:1.6.1")
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")
    implementation("androidx.camera:camera-video:1.6.1")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-transformer:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.material3)
    // Account avatar only; no image requests in offer processing.
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Firebase versions remain aligned through the BoM; use main modules, not deprecated KTX artifacts.
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Strict S01 JSON decoding on Android and JVM; no remote configuration service.
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation("org.mockito:mockito-android:5.3.1") // Mocking library
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
