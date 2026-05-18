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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
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
).forEach { path ->
    val dir = file(path.replace(":", "/"))
    if (dir.isDirectory) include(path)
}
// include("services:ebay-conn-api")
// include("services:sync-api")
// include("services:accounting-api")
// include("services:inventory-api")
// include("services:repricer-api")
// include("services:analytics-api")
// include("services:notif-api")
// include("services:billing-api")
// include("services:admin-api")

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
