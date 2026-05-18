plugins {
    id("ebaysoft.java-conventions")
    id("org.openapi.generator") version "7.10.0"
}

description = "Generated Java HTTP client for auth-api. Sources are produced from services/auth-api/openapi.yaml at build time."

val specFile = rootProject.file("services/auth-api/openapi.yaml")
val generatedDir = layout.buildDirectory.dir("generated/sources/openapi")

openApiGenerate {
    generatorName.set("java")
    inputSpec.set(specFile.absolutePath)
    outputDir.set(generatedDir.get().asFile.absolutePath)
    apiPackage.set("com.ebaysoft.auth.client.api")
    modelPackage.set("com.ebaysoft.auth.client.model")
    invokerPackage.set("com.ebaysoft.auth.client.invoker")
    library.set("resttemplate")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "openApiNullable" to "false",
            "hideGenerationTimestamp" to "true",
        ),
    )
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    skipOperationExample.set(true)
}

sourceSets.named("main") {
    java.srcDir(generatedDir.map { it.dir("src/main/java") })
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// Several Gradle 9 task chains (sources jar, javadoc jar) also consume the generated
// sources — declare the dependency on all of them so Gradle doesn't complain about
// implicit task graph edges.
listOf("sourcesJar", "javadoc", "compileTestJava", "spotlessJava").forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach { dependsOn("openApiGenerate") }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    api("org.springframework:spring-web:6.2.0")
    api("org.springframework:spring-context:6.2.0")
    api("io.swagger.core.v3:swagger-annotations:2.2.25")
    api("jakarta.annotation:jakarta.annotation-api:3.0.0")
}
