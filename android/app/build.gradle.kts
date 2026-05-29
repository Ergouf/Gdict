import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun runGitCommand(command: List<String>): String? = try {
    val out = ByteArrayOutputStream()
    project.exec {
        commandLine(command)
        workingDir = rootProject.projectDir
        standardOutput = out
        isIgnoreExitValue = true
    }
    val result = out.toString("UTF-8").trim()
    result.ifEmpty { null }
} catch (e: Exception) {
    null
}

val gitVersionCode = runGitCommand(listOf("git", "rev-list", "--count", "HEAD"))?.toIntOrNull() ?: 1
val gitVersionName = (runGitCommand(listOf("git", "describe", "--tags", "--always"))
    ?.removePrefix("v") ?: "1.0.0")

android {
    namespace = "io.github.gdict"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile") ?: rootProject.file("release.keystore").path)
            storePassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD") ?: ""
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "gdict"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    defaultConfig {
        applicationId = "io.github.gdict"
        minSdk = 26
        targetSdk = 34
        versionCode = gitVersionCode
        versionName = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation("io.github.gdict:core")
    implementation("io.github.gdict:shared-ui")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // DataStore
    val dataStoreVersion = "1.0.0"
    implementation("androidx.datastore:datastore-preferences:$dataStoreVersion")
    
    // 图标库
    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // MDX 词典解析 (纯 Kotlin 实现，无外部依赖)

    testImplementation("junit:junit:4.13.2")
    testImplementation("app.cash.paparazzi:paparazzi:1.3.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
