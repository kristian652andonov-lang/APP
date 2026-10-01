import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Secrets/config are read from local.properties (git-ignored) or environment variables,
// never from source code. See README.md -> "Configuring the news API".
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun config(propertyName: String, envName: String, default: String = ""): String =
    localProperties.getProperty(propertyName)
        ?: System.getenv(envName)
        ?: providers.gradleProperty(propertyName).orNull
        ?: default

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "com.aura.dashboard"
    compileSdk = 37

    defaultConfig {
        applicationId = providers.gradleProperty("aura.applicationId").get()
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("aura.versionCode").get().toInt()
        versionName = providers.gradleProperty("aura.versionName").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "NEWS_API_BASE_URL",
            config("aura.newsApiBaseUrl", "AURA_NEWS_API_BASE_URL").asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "NEWS_API_KEY",
            config("aura.newsApiKey", "AURA_NEWS_API_KEY").asBuildConfigString(),
        )
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.reorderable)
    implementation(libs.haze)
    implementation(libs.haze.blur)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kxml2)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}

tasks.withType<Test>().configureEach {
    // UI screenshot tests load live news, so they only run when explicitly requested:
    // ./gradlew testDebugUnitTest -Paura.screenshots=true
    systemProperty("aura.screenshots", providers.gradleProperty("aura.screenshots").getOrElse("false"))
    systemProperty("aura.screenshotDir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    maxHeapSize = "3g"
}
