plugins {
    id("ebaysoft.lib-conventions")
}

description = "Jackson Money codec, RFC-7807 error model, OpenAPI auto-config, OTel auto-config — shared by every service."

dependencies {
    api(project(":libs:common-domain"))
    // Spring deps added by services that consume this lib — kept off the lib for now.
}
