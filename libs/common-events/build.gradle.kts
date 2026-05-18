plugins {
    id("ebaysoft.lib-conventions")
}

description = "CloudEvents 1.0 envelope, transactional-outbox helpers, LISTEN/NOTIFY consumer skeleton."

dependencies {
    api(project(":libs:common-domain"))
}
