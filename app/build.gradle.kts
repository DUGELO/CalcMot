import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("androidx.room")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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
        versionCode = 10
        versionName = "3.3"

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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.material3)

    // Firebase versions remain aligned through the BoM; use main modules, not deprecated KTX artifacts.
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    testImplementation(libs.junit)
    testImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("org.mockito:mockito-android:5.3.1") // Mocking library
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val verifyComposeClasspathAlignment by tasks.registering {
    group = "verification"
    description = "Checks that Compose modules use identical versions at compile time and runtime."

    doLast {
        fun composeVersions(configurationName: String): Map<String, String> =
            configurations.getByName(configurationName)
                .incoming
                .resolutionResult
                .allComponents
                .mapNotNull { component ->
                    val id = component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier
                        ?: return@mapNotNull null
                    if (!id.group.startsWith("androidx.compose")) return@mapNotNull null
                    "${id.group}:${id.module}" to id.version
                }
                .toMap()

        listOf("debug", "release").forEach { variant ->
            val compile = composeVersions("${variant}CompileClasspath")
            val runtime = composeVersions("${variant}RuntimeClasspath")
            val mismatches = compile.keys.intersect(runtime.keys).mapNotNull { module ->
                val compileVersion = compile.getValue(module)
                val runtimeVersion = runtime.getValue(module)
                if (compileVersion == runtimeVersion) null
                else "$module compile=$compileVersion runtime=$runtimeVersion"
            }
            check(mismatches.isEmpty()) {
                "Compose classpath mismatch for $variant:\n${mismatches.joinToString("\n")}"
            }
        }
    }
}

tasks.named("check").configure {
    dependsOn(verifyComposeClasspathAlignment)
}
