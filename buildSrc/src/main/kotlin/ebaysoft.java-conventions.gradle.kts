// Convention plugin applied by every Java module (libs + services).
// Locks toolchain, compile flags, test framework, and code style.

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",          // suppress noisy annotation-processor warnings
            "-Xlint:-serial",
            "-parameters",                  // needed by Spring 6 for parameter binding
        ),
    )
    // -Werror disabled while scaffolding; re-enable in a follow-up PR once everything compiles cleanly.
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    systemProperty("file.encoding", "UTF-8")
}

repositories {
    mavenCentral()
}

dependencies {
    "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.3")
    "testImplementation"("org.assertj:assertj-core:3.26.3")
    "testImplementation"("org.mockito:mockito-core:5.14.2")
    "testImplementation"("org.mockito:mockito-junit-jupiter:5.14.2")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

// Spotless formatting is temporarily disabled — google-java-format 1.24.0 hits a
// "Log$DeferredDiagnosticHandler.getDiagnostics" error on JDK 25. Re-enable once we move to
// google-java-format ≥ 1.25.x or run Spotless on a JDK 21 toolchain.
spotless {
    java {
        target("src/**/*.java")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.4.1")
    }
}
