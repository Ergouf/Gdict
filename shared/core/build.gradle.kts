plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "io.github.gdict"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        java.setSrcDirs(listOf())  // Explicitly exclude java directory
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.json:json:20231013")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
    val mdxPath = System.getProperty("mdx.file.path")
    if (mdxPath != null) {
        systemProperty("mdx.file.path", mdxPath)
    }
}
