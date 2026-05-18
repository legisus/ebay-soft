package com.ebaysoft.test.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architecture-level safety rails for money math, applied via ArchUnit's @AnalyzeClasses on
 * each service. See docs/BACKEND.md → Money handling.
 */
public final class MoneyArchRules {

  private MoneyArchRules() {}

  /** No {@code float}/{@code double} fields, parameters, or return types in production code. */
  public static final ArchRule NO_FLOATING_POINT_FIELDS =
      noClasses()
          .that()
          .resideInAPackage("com.ebaysoft..")
          .should(haveFloatingPointMembers())
          .because("money is BigDecimal/Money, not floating point — see docs/BACKEND.md");

  /** No {@code new BigDecimal(double)} — silently lossy. */
  public static final ArchRule NO_BIGDECIMAL_DOUBLE_CONSTRUCTOR =
      noClasses()
          .that()
          .resideInAPackage("com.ebaysoft..")
          .should(callBigDecimalDoubleConstructor())
          .because(
              "new BigDecimal(double) inherits IEEE-754 imprecision — use "
                  + "new BigDecimal(String) or BigDecimal.valueOf(double)");

  /** No {@code BigDecimal#divide(BigDecimal)} — every division specifies scale + RoundingMode. */
  public static final ArchRule NO_NAKED_BIGDECIMAL_DIVIDE =
      noClasses()
          .that()
          .resideInAPackage("com.ebaysoft..")
          .should(callNakedBigDecimalDivide())
          .because(
              "BigDecimal#divide(BigDecimal) throws on non-terminating decimals; always pass "
                  + "an explicit (scale, RoundingMode)");

  private static ArchCondition<JavaClass> haveFloatingPointMembers() {
    return new ArchCondition<>("have float/double in their API surface") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        clazz.getFields().forEach(f -> {
          String type = f.getRawType().getName();
          if (isFloating(type)) {
            events.add(SimpleConditionEvent.violated(
                f, "%s.%s has type %s".formatted(clazz.getSimpleName(), f.getName(), type)));
          }
        });
        clazz.getMethods().forEach(m -> {
          String ret = m.getRawReturnType().getName();
          if (isFloating(ret)) {
            events.add(SimpleConditionEvent.violated(
                m, "%s#%s returns %s".formatted(clazz.getSimpleName(), m.getName(), ret)));
          }
          for (JavaParameter p : m.getParameters()) {
            String t = p.getRawType().getName();
            if (isFloating(t)) {
              events.add(SimpleConditionEvent.violated(
                  m, "%s#%s parameter has type %s".formatted(clazz.getSimpleName(), m.getName(), t)));
            }
          }
        });
      }
    };
  }

  private static ArchCondition<JavaClass> callBigDecimalDoubleConstructor() {
    return new ArchCondition<>("call new BigDecimal(double)") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        for (JavaConstructorCall call : clazz.getConstructorCallsFromSelf()) {
          if (!call.getTargetOwner().isEquivalentTo(java.math.BigDecimal.class)) continue;
          var params = call.getTarget().getRawParameterTypes();
          if (params.size() == 1 && params.get(0).getName().equals("double")) {
            events.add(SimpleConditionEvent.violated(
                call, describe(call, clazz, "new BigDecimal(double)")));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> callNakedBigDecimalDivide() {
    return new ArchCondition<>("call BigDecimal#divide(BigDecimal) without RoundingMode") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        for (JavaMethodCall call : clazz.getMethodCallsFromSelf()) {
          if (!call.getTargetOwner().isEquivalentTo(java.math.BigDecimal.class)) continue;
          if (!call.getTarget().getName().equals("divide")) continue;
          var params = call.getTarget().getRawParameterTypes();
          if (params.size() == 1 && params.get(0).isEquivalentTo(java.math.BigDecimal.class)) {
            events.add(SimpleConditionEvent.violated(
                call, describe(call, clazz, "BigDecimal#divide(BigDecimal)")));
          }
        }
      }
    };
  }

  private static String describe(JavaCall<?> call, JavaClass owner, String what) {
    return "%s.%s calls %s at line %d".formatted(
        owner.getSimpleName(),
        call.getOrigin().getName(),
        what,
        call.getLineNumber());
  }

  private static boolean isFloating(String type) {
    return type.equals("float") || type.equals("double");
  }

  // Suppress unused warning for the import that's kept for future rules.
  @SuppressWarnings("unused")
  private static final DescribedPredicate<?> KEEP_IMPORT = DescribedPredicate.alwaysTrue();
}
