plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Owns eBay sync state — backfill, incremental notifications, normalized orders/listings/fees, status SSE."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-security"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Resilience4j for circuit breaker + rate limiter + retry on the outbound eBay calls.
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")

    implementation("org.flywaydb:flyway-core:11.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.0")
    runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.7.0")

    testImplementation("io.projectreactor:reactor-test")
    testImplementation(project(":libs:common-test"))
}
