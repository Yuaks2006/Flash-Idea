import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val aiBaseUrl = (localProps["AI_BASE_URL"] ?: "https://api.deepseek.com/v1/")
    .toString()
    .trimEnd('/') + "/"
val aiProvider = (localProps["AI_PROVIDER"] ?: "local").toString()
val aiModel = (localProps["AI_MODEL"] ?: "Doubao-Seed-2.0-mini").toString()
val vivoAppId = (localProps["VIVO_APP_ID"] ?: "").toString()
val aiApiKey = (
    localProps["AI_API_KEY"]
        ?: localProps["VIVO_APP_KEY"]
        ?: localProps["VIVO_AIGC_API_KEY"]
        ?: ""
    ).toString()

android {
    namespace = "com.flashidea.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.flashidea.app"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "AI_BASE_URL", "\"$aiBaseUrl\"")
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")
        buildConfigField("String", "AI_PROVIDER", "\"$aiProvider\"")
        buildConfigField("String", "AI_MODEL", "\"$aiModel\"")
        buildConfigField("String", "VIVO_APP_ID", "\"$vivoAppId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    forkEvery = 0
    maxHeapSize = "256m"
    jvmArgs("-XX:MaxMetaspaceSize=128m")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
