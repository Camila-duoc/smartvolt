plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "cl.smartvolt.asistencia"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.smartvolt.asistencia"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    // demo = sensores simulados (GPS/BLE) para probar sin hardware; campo = sensores reales.
    flavorDimensions += "modo"
    productFlavors {
        create("demo") { dimension = "modo"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "USE_FAKE_SENSORS", "true") }
        create("campo") { dimension = "modo"; buildConfigField("boolean", "USE_FAKE_SENSORS", "false") }
    }
    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"") } // 10.0.2.2 = PC host desde el emulador
        release { isMinifyEnabled = false; buildConfigField("String", "API_BASE_URL", "\"https://api.smartvolt.invalid/\"") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui); implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3); implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose); implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose); implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
    implementation(libs.hilt.work); ksp(libs.hilt.androidx.compiler); implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime); implementation(libs.room.ktx); ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.retrofit); implementation(libs.retrofit.kotlinx); implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization)
    implementation(libs.coroutines.android); implementation(libs.coroutines.play)
    implementation(libs.play.location)
    testImplementation(libs.junit); testImplementation(libs.coroutines.test)
}
