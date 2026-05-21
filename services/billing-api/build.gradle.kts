plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Billing — Stripe Checkout, Customer Portal, webhook receiver, 14-day Pro trial, plan-limit enforcement."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
