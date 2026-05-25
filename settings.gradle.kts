rootProject.name = "ebay-soft"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Shared libraries (zero Spring, infra-only)
include(
    "libs:common-domain",
    "libs:common-web",
    "libs:common-security",
    "libs:common-events",
    "libs:common-test",
)

// Services — Spring Boot apps, one per microservice.
// Each entry is *opt-in* when its directory exists, so a Docker build that copies only
// a single service still configures cleanly.
listOf(
    "services:api-gateway",
    "services:auth-api",
    "services:ebay-conn-api",
    "services:sync-api",
    "services:accounting-api",
    "services:analytics-api",
    "services:inventory-api",
    "services:notif-api",
    "services:billing-api",
    "services:admin-api",
    "services:repricer-api",
).forEach { path ->
    val dir = file(path.replace(":", "/"))
    if (dir.isDirectory) include(path)
}
// "services:ml-api"  // Python (FastAPI), separate from this Gradle build

// Generated OpenAPI clients — same opt-in pattern as services.
listOf(
    "clients:auth-api-client",
    // "clients:ebay-conn-api-client",
).forEach { path ->
    val dir = file(path.replace(":", "/"))
    if (dir.isDirectory) include(path)
}

rootProject.children.forEach { renameBuildFileToMatchProject(it) }

fun renameBuildFileToMatchProject(project: ProjectDescriptor) {
    project.children.forEach { renameBuildFileToMatchProject(it) }
}
