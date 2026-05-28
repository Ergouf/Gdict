plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0" apply false
}

group = "io.github.gdict"
version = "1.0.0"

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
