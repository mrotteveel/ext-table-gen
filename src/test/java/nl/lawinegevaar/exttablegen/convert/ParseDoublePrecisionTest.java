// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseDoublePrecisionTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            inputString,             languageTag, expectedValue
            1.23,                    en-US,       1.23
            '1,234.56',              en-US,       1234.56
            '1,23',                  nl-NL,       1.23
            '1.234,56',              nl-NL,       1234.56
            1234567890123456,        en-US,       1234567890123456
            1.234567890123456,       en-US,       1.234567890123456
            -1234567890123456,       en-US,       -1234567890123456
            -1.234567890123456,      en-US,       -1.234567890123456
            '1,234,567,890,123,456', en-US,       1234567890123456
            """)
    void testConvert_validValues(String inputString, String languageTag, double expectedValue) {
        var locale = Locale.forLanguageTag(languageTag);
        var parseDoublePrecision = new ParseDoublePrecision(locale);

        assertEquals(expectedValue, parseDoublePrecision.convert(inputString));
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
        var parseDoublePrecision = new ParseDoublePrecision(locale);

        assertThrows(NumberFormatException.class, () -> parseDoublePrecision.convert(inputString));
    }

    @Test
    void constructWithNullLocale_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ParseDoublePrecision(null));
    }

}
