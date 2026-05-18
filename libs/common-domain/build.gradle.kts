plugins {
    id("ebaysoft.lib-conventions")
}

description =
    """
    Pure-Java value objects shared across services: Money, TenantId, UserId, SkuCode, Currency helpers.
    Zero Spring, zero database, zero web. If it needs a framework, it doesn't belong here.
    """.trimIndent()

// No production dependencies on purpose — keep this jar tiny so every service can pull it in.
dependencies {
    testImplementation(project(":libs:common-test"))
}
