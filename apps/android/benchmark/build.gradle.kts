plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.wakemyway.benchmark"
    compileSdk = 37
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
        }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
}
