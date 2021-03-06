plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

object Versions {
    private const val versionMajor = 1
    private  const val versionMinor = 1
    private const val versionPatch = 1
    private val versionClassifier = null

    const val minSdk = 24
    const val targetSdk = 30

    fun generateVersionCode(): Int = minSdk * 10000000 + versionMajor * 10000 + versionMinor * 100 + versionPatch

    fun generateVersionName(): String {
        var versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        @Suppress("SENSELESS_COMPARISON")
        if (versionClassifier != null) {
            versionName += "-$versionClassifier"
        }
        return versionName
    }
}

android {
    compileSdkVersion(Versions.targetSdk)

    defaultConfig {
        applicationId = "xyz.kurozero.nekosmoe"
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = Versions.generateVersionCode()
        versionName = Versions.generateVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    @Suppress("COMPATIBILITY_WARNING")
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib"))

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")
    implementation("com.github.stfalcon:stfalcon-imageviewer:1.0.1")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

    implementation("com.google.android.material:material:1.2.1")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.facebook.fresco:fresco:1.9.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.hendraanggrian:pikasso:0.2")

    implementation("de.hdodenhof:circleimageview:3.0.1")

    implementation("org.jetbrains.anko:anko:0.10.8")
    implementation("org.jetbrains.anko:anko-design:0.10.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
