import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Stamp the build with the short HEAD commit hash. Reading it through a ValueSource that shells
// out via ExecOperations is the config-cache compatible way to read external state, and because
// the value only changes when HEAD moves, the configuration cache reuses it across no-op builds
// instead of being discarded every build. Falls back to "unknown" when git isn't available (e.g.
// building from a source archive).
abstract class GitCommitHashValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String =
        runCatching {
            val stdout = ByteArrayOutputStream()
            val result =
                execOperations.exec {
                    commandLine("git", "rev-parse", "--short", "HEAD")
                    standardOutput = stdout
                    errorOutput = ByteArrayOutputStream()
                    isIgnoreExitValue = true
                }
            stdout.toString().trim().takeIf { result.exitValue == 0 && it.isNotEmpty() }
        }.getOrNull() ?: "unknown"
}

val gitCommitHash = providers.of(GitCommitHashValueSource::class) {}.get()

val keystoreProperties =
    Properties().apply {
        val propertiesFile = rootProject.file("keystore.properties")
        if (propertiesFile.isFile) {
            propertiesFile.inputStream().use(::load)
        }
    }

fun releaseSigningValue(
    propertyName: String,
    environmentName: String,
): String? =
    keystoreProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val releaseStoreFile = releaseSigningValue("storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("keyPassword", "RELEASE_KEY_PASSWORD")
val hasAnyReleaseSigningConfig =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).any { it != null }
val hasReleaseSigningConfig =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

check(!hasAnyReleaseSigningConfig || hasReleaseSigningConfig) {
    "Release signing requires storeFile, storePassword, keyAlias, and keyPassword in " +
        "keystore.properties or RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, " +
        "and RELEASE_KEY_PASSWORD environment variables."
}

android {
    namespace = "app.tanh.toolsftw"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.tanh.toolsftw"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GIT_COMMIT_HASH", "\"$gitCommitHash\"")
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    compileOnly(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}
