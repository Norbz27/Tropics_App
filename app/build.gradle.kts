plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.tropics_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tropics_app"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.tasks)
    implementation ("com.google.android.gms:play-services-vision:20.1.1")
    implementation ("com.google.android.gms:play-services-vision-common:19.1.1")
    implementation("com.applandeo:material-calendar-view:1.9.0-rc03")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation(libs.firebase.ml.vision)
    implementation(libs.swiperefreshlayout)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
