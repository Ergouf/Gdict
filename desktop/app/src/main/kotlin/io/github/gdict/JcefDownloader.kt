package io.github.gdict

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.IProgressHandler
import java.io.File

fun main(args: Array<String>) {
    val jcefDir = File(System.getProperty("user.home"), ".gdict/jcef-bundle")
    val lockFile = File(jcefDir, "install.lock")
    if (lockFile.exists()) {
        println("JCEF bundle already exists at $jcefDir")
        return
    }
    println("Downloading JCEF bundle...")
    jcefDir.mkdirs()
    val builder = CefAppBuilder()
    builder.setInstallDir(jcefDir)
    builder.setProgressHandler(IProgressHandler { state, percent ->
        println("JCEF setup: $state ($percent%)")
    })
    builder.build().dispose()
    println("JCEF bundle downloaded successfully")
}
