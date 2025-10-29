plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktlint)
    id("org.jetbrains.kotlin.kapt")
}
val packageName = "com.naminfo.cdot_vc"

android {
    namespace = "com.naminfo.cdot_vc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.naminfo.cdot_vc"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "APP_BRANCH", "\"main\"")
        buildConfigField("String", "SDK_BRANCH", "\"dev\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //resValue("string", "linphone_app_branch", gitBranch.toString().trim())
            resValue("string", "sync_account_type", "${packageName}.sync")
            resValue("string", "file_provider", "${packageName}.fileprovider")
            resValue(
                "string",
                "linphone_address_mime_type",
                "vnd.android.cursor.item/vnd.${packageName}.provider.sip_address"
            )

            buildConfigField("String", "APP_BRANCH", "\"release-branch\"")
            buildConfigField("String", "SDK_BRANCH", "\"release-sdk\"")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        dataBinding = true
        //viewBinding = true
    }
}
val coilVersion = "2.4.0"
val nav_version = "2.7.5"
dependencies {
    implementation (libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    implementation(libs.linphone)
    implementation ("androidx.slidingpanelayout:slidingpanelayout:1.2.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6") // latest stable as of Sept 2025
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-svg:$coilVersion")
    implementation("io.coil-kt:coil-video:$coilVersion")
    implementation ("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation ("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation(libs.androidx.emoji2)
    //implementation(libs.androidx.media.common.ktx)
    implementation ("androidx.media:media:1.7.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(files("libs/ksoap2-android-assembly-2.6.2-jar-with-dependencies.jar"))
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.window)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
