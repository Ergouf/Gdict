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

val gitVersionName = run {
    val raw = runGitCommand(listOf("git", "describe", "--tags", "--always"))
        ?.removePrefix("v") ?: "1.0.0"
    val parts = raw.split("-", ".")
    if (parts.size >= 3 && parts.take(3).all { it.toIntOrNull() != null }) {
        parts.take(3).joinToString(".")
    } else {
        "1.0.0"
    }
}

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
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
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
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/javax.swing=ALL-UNNAMED"
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

// Workaround for Compose Desktop stripping java.exe from bundled JDK runtime
// See: https://github.com/JetBrains/compose-multiplatform/issues/2004
// The Compose Desktop jpackage step uses jlink with --strip-native-commands,
// which removes java.exe, javaw.exe and jli.dll from the bundled runtime.
// jpackage's Windows launcher (Gdict.exe) requires java.exe to exist to
// launch the JVM, otherwise the user sees a "Failed to launch JVM" error.
tasks.register<Copy>("fixBundledJavaExe") {
    group = "gdict"
    description = "Restore java.exe / javaw.exe / jli.dll in bundled JDK runtime"
    dependsOn("packageAppImage")

    val jdkHome = File(System.getProperty("java.home"))
    val javaBinDir = if (jdkHome.name == "jre") File(jdkHome, "../bin") else File(jdkHome, "bin")

    from(file("${javaBinDir.absolutePath}/java.exe"))
    from(file("${javaBinDir.absolutePath}/javaw.exe"))
    from(file("${javaBinDir.absolutePath}/jli.dll"))

    // Resolve the actual app dir, which follows Compose Desktop's layout:
    //   compose/binaries/main/app/<packageName>/
    val packageName = (project.findProperty("compose.desktop.packageName") as? String)
        ?: "Gdict"
    val outputDir = layout.buildDirectory.dir("compose/binaries/main/app/$packageName/runtime/bin")
    into(outputDir)
}

// Copy JCEF bundle resources to the packaged application directory
tasks.register<Copy>("copyJcefBundle") {
    group = "gdict"
    description = "Copy JCEF bundle resources to packaged application"
    dependsOn("packageAppImage")

    val jcefSourceDir = file("resources/windows-x64/jcef-bundle")
    val packageName = (project.findProperty("compose.desktop.packageName") as? String)
        ?: "Gdict"
    val appDir = layout.buildDirectory.dir("compose/binaries/main/app/$packageName")

    from(jcefSourceDir) {
        into("jcef-bundle")
    }
    into(appDir)

    // Only copy if source directory exists
    onlyIf { jcefSourceDir.exists() }
}

tasks.named("build") {
    dependsOn("fixBundledJavaExe")
}

// Both packageAppImage and packageExe emit the same runtime/bin layout,
// so both need the java.exe workaround applied.
tasks.matching { it.name == "packageExe" || it.name == "packageMsi" || it.name == "packageDeb" || it.name == "packageDmg" }.configureEach {
    dependsOn("fixBundledJavaExe")
    dependsOn("copyJcefBundle")
}
