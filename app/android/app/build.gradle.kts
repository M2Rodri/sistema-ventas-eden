plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.muebleriaeden"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.muebleriaeden"
        // flutter_secure_storage necesita API 23+ para EncryptedSharedPreferences.
        // No hace falta fijarlo a mano: el Flutter instalado ya exige minSdk 24
        // como mínimo (lo impone flutter_tools, ver
        // MinSdkVersionMigration/minSdkVersionInt), así que flutter.minSdkVersion
        // ya cumple de sobra. Fijarlo más bajo (ej. 23) no sirve: el propio
        // Flutter lo pisa de vuelta en cada `flutter run`/`flutter build`.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
