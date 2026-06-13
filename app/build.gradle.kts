plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.duisternis.voidgrid"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.duisternis.voidgrid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Retrofit para fazer a conexão com a internet
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Converter do Gson para transformar o texto do Google em código Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Coil para carregar as imagens da internet direto na tela
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Ícones adicionais do Material Design (necessário para o Icons.Default.Search)
    implementation("androidx.compose.material:material-icons-extended")
    // Adicione esta linha nas suas dependencies do build.gradle.kts se ainda não tiver:
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("org.jsoup:jsoup:1.17.2")
}