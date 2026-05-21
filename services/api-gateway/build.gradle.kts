plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Spring Cloud Gateway — routes the SPA's traffic to backend services, validates JWTs, applies per-tenant rate limits."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-security"))

    // `spring-cloud-starter-gateway` is deprecated in 2025.0.0 — use the
    // webflux server variant explicitly. Same module set, future-compat.
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    testImplementation(project(":libs:common-test"))
}
