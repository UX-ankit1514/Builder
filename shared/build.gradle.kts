import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            // Static, so the Firebase iOS SDK (linked by the Xcode project via
            // Swift Package Manager) resolves the Firebase symbols at app link time.
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.uxankit.stepwise.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServices)
            implementation(libs.googleid)
        }
    }
}

compose.resources {
    packageOfResClass = "com.uxankit.stepwise.resources"
    publicResClass = false
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
