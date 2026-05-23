plugins { id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false }
repositories { mavenCentral() }

val export by tasks.registering(JavaExec::class) {
    dependsOn(":core:classes")
    classpath(configurations.create("exportClasspath").apply {
        extendsFrom(configurations.getByName("implementation"))
        extendsFrom(configurations.getByName("runtimeOnly"))
    })
    mainClass.set("MdxExportKt")
    args = listOf(
        project.findProperty("mdxPath")?.toString() ?: "D:\\workspace\\Gdict\\Cambridge_English_Pronouncing_Dictionary_18th.mdx",
        project.findProperty("outputDir")?.toString() ?: "D:\\workspace\\Gdict\\test_export"
    )
}
