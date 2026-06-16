plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    //noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

android {
    namespace = "com.duisternis.voidgrid"
    //noinspection GradleDependency
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duisternis.voidgrid"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

    dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.activity:activity-ktx:1.9.0")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // Compose UI & Material 3
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Paging 3
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.paging:paging-compose:3.3.0")

    // Rede
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Parser HTML
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("org.jsoup:jsoup:1.17.2")

    // Serialização
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coil
    //noinspection UseTomlInstead
    implementation("io.coil-kt:coil-compose:2.7.0")
    //noinspection UseTomlInstead
    implementation("io.coil-kt:coil:2.7.0")

    // Appcompat
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}