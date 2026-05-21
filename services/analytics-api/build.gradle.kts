plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Analytics — chart-ready aggregates over orders/finance, plus the SSE stream that feeds the dashboard."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
