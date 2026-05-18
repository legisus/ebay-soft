// Applied by every shared library in libs/*. Pure Java, no Spring Boot plugin.

plugins {
    id("ebaysoft.java-conventions")
}

// Libraries stay framework-agnostic where possible; Spring deps belong to services.
