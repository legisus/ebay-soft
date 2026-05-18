plugins {
    id("ebaysoft.lib-conventions")
}

description = "CloudEvents envelope, transactional-outbox publisher, LISTEN/NOTIFY consumer skeleton."

dependencies {
    api(project(":libs:common-domain"))
}
