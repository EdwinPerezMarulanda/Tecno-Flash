plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.tecnoflash.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tecnoflash.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        )
    }
}

dependencies {

    // Componentes Android
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")

    // Autenticación biométrica
    implementation("androidx.biometric:biometric:1.1.0")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Base de datos Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Almacenamiento de imágenes
    implementation("com.google.firebase:firebase-storage")

    // Obtener ubicación del dispositivo
    implementation("com.google.android.gms:play-services-location:21.4.0")
}
