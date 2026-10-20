plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shilapi.xcertplay.host"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild { ndkBuild { targets += "hev-socks5-tunnel"; arguments += "NDK_APPLICATION_MK=${rootProject.file("native/hev-socks5-tunnel/Application.mk")}" } }
    }

    sourceSets.getByName("main").java.srcDir("../../protocol/src/main/java")
    ndkVersion = "28.2.13676358"
    externalNativeBuild { ndkBuild { path = file("src/main/cpp/Android.mk") } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets.getByName("main").java.srcDir("../../android-common/src/main/java")
    sourceSets.getByName("test").java.srcDir("../../android-common/src/test/java")
    testOptions { unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
    implementation("com.google.android.material:material:1.13.0")
    androidTestImplementation(libs.bouncycastle)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    api(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.1")
    testImplementation(libs.junit)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("org.robolectric:robolectric:4.17")
}

// Android 16 Robolectric ApplicationSharedMemory uses the JDK FileDescriptor accessor.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

// Always refresh revision provenance: local dirty builds must never claim a published commit.
abstract class PrepareDistributionAssets : Exec() {
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
}
val prepareDistributionAssets by tasks.registering(PrepareDistributionAssets::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/distributionAssets"))
    outputs.upToDateWhen { false }
    commandLine("python3", rootProject.file("../scripts/distribution.py"),
        "--assets", outputDirectory.get().asFile)
}
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(prepareDistributionAssets, PrepareDistributionAssets::outputDirectory)
}
