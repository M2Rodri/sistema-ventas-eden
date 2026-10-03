import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Llave de firma de la app (ver android/key.properties, que no se sube a git). Una
// actualizacion solo se instala sobre la app ya instalada si esta firmada con la
// MISMA llave: si se pierde el archivo .jks hay que desinstalar y volver a instalar.
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("key.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
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

    signingConfigs {
        if (propiedadesFirma.containsKey("storeFile")) {
            create("release") {
                storeFile = file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Con key.properties firma con la llave propia; sin el, cae a la de depuracion
            // para que compilar en otra computadora no falle (esa version NO sirve para publicar).
            signingConfig = if (propiedadesFirma.containsKey("storeFile")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
