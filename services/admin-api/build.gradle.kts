plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Admin / back-office — tenant search, audited impersonation, refunds. Staff-only surface."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-security"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
