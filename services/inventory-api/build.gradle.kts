plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Inventory — owns SKU master after the Phase-3 cutover (#60), multi-warehouse stock, low-stock alerts, dead-stock reports."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
