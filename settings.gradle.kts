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
// Listed but not yet scaffolded; uncomment as each ships.
// include("services:api-gateway")
// include("services:auth-api")
// include("services:ebay-conn-api")
// include("services:sync-api")
// include("services:accounting-api")
// include("services:inventory-api")
// include("services:repricer-api")
// include("services:analytics-api")
// include("services:notif-api")
// include("services:billing-api")
// include("services:admin-api")

// Generated OpenAPI clients
// include("clients:auth-api-client")
// include("clients:ebay-conn-api-client")
// ...

rootProject.children.forEach { renameBuildFileToMatchProject(it) }

fun renameBuildFileToMatchProject(project: ProjectDescriptor) {
    project.children.forEach { renameBuildFileToMatchProject(it) }
}
