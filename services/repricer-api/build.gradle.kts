plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Repricer — rule-based price worker that pushes prices via ebay-conn-api. Plan-gated."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
