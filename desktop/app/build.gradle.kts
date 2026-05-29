import java.io.ByteArrayOutputStream

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun runGitCommand(command: List<String>): String? = try {
    val out = ByteArrayOutputStream()
    project.exec {
        commandLine(command)
        workingDir = rootProject.projectDir
        standardOutput = out
        isIgnoreExitValue = true
    }
    val result = out.toString("UTF-8").trim()
    result.ifEmpty { null }
} catch (e: Exception) {
    null
}

val gitVersionName = (runGitCommand(listOf("git", "describe", "--tags", "--always"))
    ?.removePrefix("v") ?: "1.0.0")

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.github.gdict:core")
    implementation("io.github.gdict:shared-ui")
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("org.json:json:20231013")
    implementation("me.friwi:jcefmaven:126.2.0")
    implementation("javazoom:jlayer:1.0.1")
}

compose.desktop {
    application {
        mainClass = "io.github.gdict.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )

            packageName = "Gdict"
            packageVersion = gitVersionName
            description = "Gdict - Desktop Dictionary Application"
            vendor = "Gdict"
            copyright = "© 2024 Gdict. All rights reserved."

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                menuGroup = "Gdict"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                dirChooser = true
                perUserInstall = true
                shortcut = true
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }

            jvmArgs += listOf(
                "--add-opens", "java.base/java.net=ALL-UNNAMED",
                "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
                "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
                "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
            )
        }
    }
}

tasks.register<Exec>("packageMsix") {
    group = "gdict"
    description = "Build MSIX package for Microsoft Store"
    dependsOn("packageAppImage")
    workingDir = file("${rootProject.projectDir}/msix")
    commandLine(
        "powershell",
        "-ExecutionPolicy", "Bypass",
        "-File", "package.ps1",
        "-Version", "${compose.desktop.application.nativeDistributions.packageVersion}.0"
    )
}
