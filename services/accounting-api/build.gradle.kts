plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "P&L computation, fee reconciliation, exports (CSV/XLSX/PDF). Spring MVC + virtual threads."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-security"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    implementation("org.flywaydb:flyway-core:11.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
}
