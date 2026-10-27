plugins { id("com.android.application") version "9.1.0" }
android {
    namespace = "com.krplay.client"
    compileSdk = 35
    defaultConfig { applicationId = "com.krplay.client"; minSdk = 29; targetSdk = 32; versionCode = providers.environmentVariable("KRPLAY_VERSION_CODE").orNull?.toInt() ?: 5001; versionName = providers.environmentVariable("KRPLAY_RELEASE_TAG").orNull ?: "0.5.0" }
    val localKey = file(".signing/krplay.p12")
    if (localKey.exists()) {
        signingConfigs.getByName("debug") {
            storeFile = localKey; storePassword = "android"; keyAlias = "krplay"; keyPassword = "android"
        }
    }

    // Public APK updates must reuse the same externally provisioned signing identity.
    providers.environmentVariable("KRPLAY_UPDATE_KEYSTORE_PATH").orNull?.let { path ->
        signingConfigs.getByName("debug") {
            storeFile = file(path)
            check(storeFile!!.isFile) { "APK update signing keystore does not exist" }
            storePassword = providers.environmentVariable("KRPLAY_UPDATE_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("KRPLAY_UPDATE_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("KRPLAY_UPDATE_KEY_PASSWORD").get()
        }
    }
    sourceSets.getByName("main").java.srcDir("../protocol/src/main/java")
    sourceSets.getByName("main").java.srcDir("../android-common/src/main/java")
    sourceSets.getByName("test").java.srcDir("../android-common/src/test/java")
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
}

tasks.register<Copy>("exportKrPlayDebug") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/KrPlay-Client-debug.apk"))
    into(rootProject.file("../dist"))
    rename { "KrPlay-Client-debug.apk" }
}

dependencies {
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
tasks.withType<Test>().configureEach { jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }

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
