// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseFloatTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            inputString,  languageTag, expectedValue
            1.23,         en-US,       1.23
            '1,234.56',   en-US,       1234.56
            '1,23',       nl-NL,       1.23
            '1.234,56',   nl-NL,       1234.56
            12345678,     en-US,       12345678
            1.2345678,    en-US,       1.2345678
            -12345678,    en-US,       -12345678
            -1.2345678,   en-US,       -1.2345678
            '12,345,678', en-US,       12345678
            """)
    void testConvert_validValues(String inputString, String languageTag, float expectedValue) {
        var locale = Locale.forLanguageTag(languageTag);
        var parseFloat = new ParseFloat(locale);

        assertEquals(expectedValue, parseFloat.convert(inputString));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            inputString,  languageTag
            not a number, en-US
            # Scientific notation is not supported
            1.23e-2,      en-US
            """)
    void testConvert_invalidValues_throwNumberFormatException(String inputString, String languageTag) {
        var locale = Locale.forLanguageTag(languageTag);
        var parseFloat = new ParseFloat(locale);

        assertThrows(NumberFormatException.class, () -> parseFloat.convert(inputString));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void constructWithNullLocale_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ParseFloat(null));
    }

}
