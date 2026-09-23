import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
}

val appVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}
val wakeMyWayVersionCode = requireNotNull(appVersion.getProperty("VERSION_CODE")).toInt()
val wakeMyWayVersionName = requireNotNull(appVersion.getProperty("VERSION_NAME"))

android {
    namespace = "com.wakemyway.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wakemyway.app"
        minSdk = 29
        targetSdk = 36
        versionCode = wakeMyWayVersionCode
        versionName = wakeMyWayVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("direct") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"direct\"")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"play\"")
        }
    }

    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        animationsDisabled = true
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(project(":wake-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.kotlinx.serialization.core)

    add("directImplementation", libs.kotlinx.serialization.json)
    add("playImplementation", libs.play.app.update.ktx)
    // Activity Result APIs require Fragment 1.3.0+ when a Play-only dependency brings Fragment
    // onto the runtime graph. Keep the modern Fragment contract scoped to the Play flavor.
    add("playImplementation", libs.androidx.fragment)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // M8 transport spike only. Release builds intentionally have no RTC dependency.
    debugImplementation(libs.webrtc.android)
}
