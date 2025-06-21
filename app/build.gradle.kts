plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.myapp.calingaapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myapp.calingaapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17 // Update to Java 17
        targetCompatibility = JavaVersion.VERSION_17 // Update to Java 17
    }
    kotlinOptions {
        jvmTarget = "17" // Update to match Java version
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("de.hdodenhof:circleimageview:3.1.0") // Add CircleImageView dependency
    
    // Firebase dependencies with fixed BOM version
    implementation(platform("com.google.firebase:firebase-bom:32.7.0")) // Downgrade to a more stable version
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage-ktx") // Add Firebase Storage dependency
    
    // Add Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // OSMDroid for maps
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    
    // Google Play Services Location for location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}