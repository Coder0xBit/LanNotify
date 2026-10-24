import com.google.protobuf.gradle.proto
import org.gradle.kotlin.dsl.sourceSets

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.protobuf")
}

android {
    namespace = "dev.soham.lannotify"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.soham.lannotify"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {

        getByName("main") {
            // Shared protocol contract from repository root.
            proto {
                srcDir("../../proto")
            }
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.grpc:grpc-okhttp:1.72.0")
    implementation("io.grpc:grpc-protobuf-lite:1.72.0")
    implementation("io.grpc:grpc-stub:1.72.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.30.2" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.72.0" }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc") { option("lite") }
            }
            builtins { create("java") { option("lite") } }
        }
    }
}

