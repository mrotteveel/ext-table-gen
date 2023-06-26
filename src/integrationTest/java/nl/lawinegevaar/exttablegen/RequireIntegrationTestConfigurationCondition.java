// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Checks if {@link IntegrationTestProperties#isCompleteConfiguration()} returns {@code true}, otherwise disables the
 * test.
 */
public class RequireIntegrationTestConfigurationCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return IntegrationTestProperties.isCompleteConfiguration()
                ? ConditionEvaluationResult.enabled("Test configuration complete")
                : ConditionEvaluationResult.disabled("Test configuration incomplete");
    }
}
