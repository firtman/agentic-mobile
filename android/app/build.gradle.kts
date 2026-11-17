import java.net.URI

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
}
val debugApiBaseUrl = providers.gradleProperty("lumio.apiBaseUrl")
    .getOrElse("http://10.0.2.2:3000/")
val debugApiUri = URI(debugApiBaseUrl)
require(debugApiUri.scheme in listOf("http", "https") && debugApiUri.host != null && debugApiBaseUrl.endsWith("/")) {
    "lumio.apiBaseUrl must be an absolute HTTP(S) URL ending in /"
}

android {
    namespace = "dev.master.lumio"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.master.lumio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"") }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://movie-api-delta-six.vercel.app/\"")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.datastore)
    implementation(libs.core)
    implementation(libs.activity)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.material3)
    implementation(libs.material3.navigation)
    implementation(libs.navigation)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coroutines.test)
}
ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}
