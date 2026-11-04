import java.net.URI

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.santaceciliatesis"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.santaceciliatesis"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Empty until the real HTTPS API is deployed.
        val apiUrl = providers.gradleProperty("SANTA_CECILIA_API_URL").orElse("").get()
        require(apiUrl.none { it == '"' || it == '\\' || it.isISOControl() })
        if (apiUrl.isNotBlank()) {
            val endpoint = URI.create(apiUrl)
            require(endpoint.scheme.equals("https", ignoreCase = true) && endpoint.host != null && endpoint.userInfo == null && endpoint.query == null && endpoint.fragment == null) { "SANTA_CECILIA_API_URL debe ser una URL base HTTPS sin credenciales" }
        }
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            val debugApi = providers.gradleProperty("SANTA_CECILIA_API_URL").orElse("https://10.0.2.2:8443/api/v1/").get()
            buildConfigField("String", "API_BASE_URL", "\"$debugApi\"")
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
