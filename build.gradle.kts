plugins {
    base
}

allprojects {
    group = "com.ebaysoft"
    version = providers.gradleProperty("ebaySoftVersion").orElse("0.1.0-SNAPSHOT").get()
}

tasks.register("printProjects") {
    description = "List every Gradle subproject — sanity check the monorepo wiring."
    group = "help"
    doLast {
        subprojects.forEach { println("  • ${it.path}  (${it.projectDir.relativeTo(rootDir)})") }
    }
}
