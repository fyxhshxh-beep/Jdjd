plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

val jitsiSdkVersion = providers.gradleProperty("jitsiSdkVersion").getOrElse("11.+")
val jitsiServerUrl = providers.gradleProperty("jitsiServerUrl").getOrElse("https://meet.jit.si")

android {
    namespace = "com.friendcall.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.friendcall.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        buildConfigField("String", "JITSI_SERVER_URL", "\"$jitsiServerUrl\"")
    }

    buildTypes {
        release {
            // Keep minification off until you add the Jitsi SDK's ProGuard rules
            // (see the Jitsi Android SDK docs) and have tested a minified build.
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("org.jitsi.react:jitsi-meet-sdk:$jitsiSdkVersion") { isTransitive = true }

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
