plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "io.github.gdict"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

sourceSets {
    named("main") {
        java.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    implementation(project(":core"))
}
