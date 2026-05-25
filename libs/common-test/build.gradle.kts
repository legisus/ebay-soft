plugins {
    id("ebaysoft.lib-conventions")
}

description = "Test fixtures: Testcontainers helpers, WireMock setup, MockEbay payloads, ArchUnit money rules."

dependencies {
    // Expose test-base deps to consumers; integration deps stay test-only of consumers.
    api("org.junit.jupiter:junit-jupiter:6.1.0")
    api("org.assertj:assertj-core:3.27.7")
    api("org.testcontainers:junit-jupiter:1.20.4")
    api("org.testcontainers:postgresql:1.20.4")
    api("org.wiremock:wiremock-standalone:3.13.2")
    api("com.tngtech.archunit:archunit-junit5:1.4.2")
}
