// SPDX-FileCopyrightText: Copyright 2023-2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Checks if {@link IntegrationTestProperties#isCompleteConfiguration()} returns {@code true}, otherwise disables the
 * test.
 */
@NullMarked
public class RequireIntegrationTestConfigurationCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return IntegrationTestProperties.isCompleteConfiguration()
                ? ConditionEvaluationResult.enabled("Test configuration complete")
                : ConditionEvaluationResult.disabled("Test configuration incomplete");
    }
}
