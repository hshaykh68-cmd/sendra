plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Coroutines (core only, no Android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // No Android dependencies - pure Kotlin
}
