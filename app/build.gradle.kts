import java.util.Properties
import java.io.FileInputStream

val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")

if (secretsFile.exists()) {
    secretsProperties.load(FileInputStream(secretsFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "id.co.tigabersama.pochuaweistream"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "id.co.tigabersama.pochuaweistream"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"${secretsProperties["BASE_URL"] ?: ""}\"")
        buildConfigField("String", "CENTRIFUGO_WEBSOCKET_URL", "\"${secretsProperties["CENTRIFUGO_WEBSOCKET_URL"] ?: ""}\"")
        buildConfigField("String", "RTMP_URL", "\"${secretsProperties["RTMP_URL"] ?: ""}\"")
        buildConfigField("String", "LIVEKIT_URL", "\"${secretsProperties["LIVEKIT_URL"] ?: ""}\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.1")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Secure Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("io.github.centrifugal:centrifuge-java:0.2.2")

    implementation("com.google.android.gms:play-services-location:21.0.1")

//    implementation("com.github.pedroSG94.RootEncoder:library:2.7.2")
    implementation("com.github.pedroSG94.RootEncoder:library:2.5.0")

    // Icon
    implementation ("androidx.compose.material:material-icons-extended")

    implementation("com.huawei.hms:location:6.12.0.300")

    implementation("dev.chrisbanes.haze:haze:0.7.3")
    implementation("dev.chrisbanes.haze:haze-materials:0.7.3")

    implementation("io.livekit:livekit-android-compose-components:2.3.0")
    implementation("io.livekit:livekit-android:2.3.0")

    // osm droid map
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    //splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")
}