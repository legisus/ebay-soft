plugins {
    id("ebaysoft.spring-service-conventions")
}

description = "Notifications — email (Postmark/Resend) + in-app feed, fed by stock.low / ebay_account.expired / payout.received events."

dependencies {
    implementation(project(":libs:common-domain"))
    implementation(project(":libs:common-web"))
    implementation(project(":libs:common-events"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation(project(":libs:common-test"))
}
