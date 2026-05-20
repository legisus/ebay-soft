plugins {
    id("ebaysoft.lib-conventions")
}

description = "CloudEvents 1.0 envelope, transactional-outbox helpers, LISTEN/NOTIFY consumer skeleton."

dependencies {
    api(project(":libs:common-domain"))
    // Jackson for outbox payload serialization. Kept as implementation since downstream
    // services pull their own Jackson via Spring Boot — we just need it on the lib's classpath.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(project(":libs:common-test"))
    testRuntimeOnly("org.postgresql:postgresql:42.7.11")
}
