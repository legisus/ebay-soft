plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Owns the seller's eBay OAuth tokens, the Marketplace Account Deletion notification endpoint, and account lifecycle events."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-security"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    implementation("org.flywaydb:flyway-core:11.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.0")
    runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    runtimeOnly("org.postgresql:postgresql:42.7.4")   // Flyway uses JDBC

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.7.0")

    testImplementation("io.projectreactor:reactor-test")
    testImplementation(project(":libs:common-test"))
}
