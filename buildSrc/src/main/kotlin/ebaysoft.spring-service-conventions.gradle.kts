// Applied by every Spring Boot service (services/*).
// Inherits ebaysoft.java-conventions for compile/test/style.

plugins {
    id("ebaysoft.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

private val springBootVersion = "3.5.0"
private val springCloudVersion = "2025.0.0"
private val testcontainersVersion = "1.20.4"
private val lombokVersion = "1.18.40"
private val wiremockVersion = "3.9.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    "implementation"("org.springframework.boot:spring-boot-starter-actuator")
    "implementation"("org.springframework.boot:spring-boot-starter-validation")
    "implementation"("io.micrometer:micrometer-tracing-bridge-otel")
    "implementation"("io.opentelemetry:opentelemetry-exporter-otlp")
    // Required so /actuator/prometheus is registered — without this the endpoint
    // returns 404 and Prometheus can't scrape the service. micrometer-tracing is
    // for spans; the registry is what serves the metrics text format.
    "implementation"("io.micrometer:micrometer-registry-prometheus")
    "implementation"("net.logstash.logback:logstash-logback-encoder:8.0")

    "compileOnly"("org.projectlombok:lombok:$lombokVersion")
    "annotationProcessor"("org.projectlombok:lombok:$lombokVersion")
    "testCompileOnly"("org.projectlombok:lombok:$lombokVersion")
    "testAnnotationProcessor"("org.projectlombok:lombok:$lombokVersion")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    "testImplementation"("org.testcontainers:junit-jupiter")
    "testImplementation"("org.testcontainers:postgresql")
    "testImplementation"("org.wiremock:wiremock-standalone:$wiremockVersion")
}

// Every service exposes a runnable jar and a plain jar (for downstream consumers, e.g. tests).
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}
