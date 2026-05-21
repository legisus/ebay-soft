plugins {
    id("ebaysoft.lib-conventions")
}

description = "Jackson Money codec, RFC-7807 error model, OpenAPI auto-config, OTel auto-config — shared by every service."

dependencies {
    api(project(":libs:common-domain"))

    // Jackson and Spring are compileOnly: services ship them transitively via spring-boot-starter-*.
    // Keeping them off the lib's runtime classpath keeps common-web pluggable in non-Boot contexts.
    val springBootVersion = "3.5.0"
    val jacksonVersion = "2.18.2"

    compileOnly("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compileOnly("org.springframework:spring-context:6.2.0")
    compileOnly("org.springframework:spring-web:6.2.0")
    compileOnly("org.springframework:spring-jdbc:6.2.0")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}
