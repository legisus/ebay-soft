package com.ebaysoft.domain;

import com.ebaysoft.test.archunit.MoneyArchRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the Money-handling architecture rules against {@code common-domain}. Each service
 * has (or will have) its own mirror of this class so the same constraints apply project-wide.
 * Rules live in {@link MoneyArchRules}.
 */
@AnalyzeClasses(packages = "com.ebaysoft..")
class MoneyArchTest {

  @ArchTest static final ArchRule no_floating_point = MoneyArchRules.NO_FLOATING_POINT_FIELDS;

  @ArchTest static final ArchRule no_bigdecimal_double_ctor = MoneyArchRules.NO_BIGDECIMAL_DOUBLE_CONSTRUCTOR;

  @ArchTest static final ArchRule no_naked_bigdecimal_divide = MoneyArchRules.NO_NAKED_BIGDECIMAL_DIVIDE;
}
