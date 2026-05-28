plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
}

val downloadJcef by tasks.registering {
    group = "gdict"
    description = "Download JCEF bundle via jcefmaven if not present"
    doLast {
        val jcefDir = file("${System.getProperty("user.home")}/.gdict/jcef-bundle")
        val lockFile = File(jcefDir, "install.lock")
        if (lockFile.exists()) {
            logger.lifecycle("JCEF bundle already exists at $jcefDir")
            return@doLast
        }
        logger.lifecycle("Downloading JCEF bundle...")
        jcefDir.mkdirs()
        val builder = me.friwi.jcefmaven.CefAppBuilder()
        builder.setInstallDir(jcefDir)
        builder.setProgressHandler(me.friwi.jcefmaven.IProgressHandler { state, percent ->
            logger.lifecycle("JCEF setup: $state ($percent%)")
        })
        builder.build().dispose()
        logger.lifecycle("JCEF bundle downloaded successfully")
    }
}

val copyJcefToResources by tasks.registering(Copy::class) {
    group = "gdict"
    description = "Copy JCEF bundle from user dir to app resources directory"
    dependsOn(downloadJcef)
    from("${System.getProperty("user.home")}/.gdict/jcef-bundle")
    into("resources/windows-x64/jcef-bundle")
    onlyIf {
        file("${System.getProperty("user.home")}/.gdict/jcef-bundle/install.lock").exists()
    }
}

val copyJcefToAppImage by tasks.registering(Copy::class) {
    group = "gdict"
    description = "Copy JCEF bundle to packaged app image"
    from("${System.getProperty("user.home")}/.gdict/jcef-bundle")
    into("${layout.buildDirectory.get()}/compose/binaries/main/app/Gdict/jcef-bundle")
    onlyIf {
        file("${System.getProperty("user.home")}/.gdict/jcef-bundle/install.lock").exists()
    }
}

afterEvaluate {
    tasks.findByName("prepareAppResources")?.dependsOn(copyJcefToResources)
    tasks.findByName("packageAppImage")?.finalizedBy(copyJcefToAppImage)
    tasks.findByName("packageExe")?.dependsOn(copyJcefToResources)
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
            packageVersion = "1.0.0"
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
                "--add-opens", "java.desktop/jdk.swing.interop=ALL-UNNAMED",
                "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
                "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED"
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
