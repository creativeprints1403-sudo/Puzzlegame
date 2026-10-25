import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun prop(name: String): String = providers.gradleProperty(name).get()

// Google's official AdMob test IDs, always used by debug builds.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val testRewarded = "ca-app-pub-3940256099942544/5224354917"

android {
    namespace = "com.creativeprints.tilemerge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.creativeprints.tilemerge"
        minSdk = 26
        targetSdk = 36
        // CI sets VERSION_CODE to the run number so every upload to Play has a higher code.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.${versionCode}"
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${prop("PRIVACY_POLICY_URL")}\"")
    }

    // Release signing comes from environment variables (GitHub Actions secrets).
    val keystorePath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"$testInterstitial\"")
            buildConfigField("String", "REWARDED_ID", "\"$testRewarded\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID")
            buildConfigField("String", "BANNER_ID", "\"${prop("ADMOB_BANNER_ID")}\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"${prop("ADMOB_INTERSTITIAL_ID")}\"")
            buildConfigField("String", "REWARDED_ID", "\"${prop("ADMOB_REWARDED_ID")}\"")
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    testImplementation("junit:junit:4.13.2")
}
