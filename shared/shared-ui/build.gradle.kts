plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "io.github.gdict"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.json:json:20231013")
}
