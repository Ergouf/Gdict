plugins { kotlin("jvm") version "1.9.22" }
repositories { mavenCentral() }
dependencies { implementation(kotlin("stdlib")) }

tasks.register<JavaExec>("exportWords") {
    val mdxPath = project.findProperty("mdxPath")?.toString()
        ?: "D:\\workspace\\Gdict\\Cambridge_English_Pronouncing_Dictionary_18th.mdx"
    val outputDir = project.findProperty("outputDir")?.toString()
        ?: "D:\\workspace\\Gdict\\test_export"
    dependsOn("compileKotlin")
    classpath = files("${buildDir}/classes/kotlin/main", configurations.runtimeClasspath)
    mainClass.set("MdxExportKt")
    args = listOf(mdxPath, outputDir)
}
