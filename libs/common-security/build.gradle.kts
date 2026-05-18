plugins {
    id("ebaysoft.lib-conventions")
}

description = "Service-account JWT issuer/verifier, JWKS client, gateway header parser."

dependencies {
    api(project(":libs:common-domain"))

    // Nimbus JOSE+JWT for signing/verifying tokens — same lib Spring Security uses under the hood.
    implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
}
