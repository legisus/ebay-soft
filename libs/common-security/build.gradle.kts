plugins {
    id("ebaysoft.lib-conventions")
}

description = "Service-account JWT issuer/verifier, JWKS client, gateway header parser."

dependencies {
    api(project(":libs:common-domain"))
}
