// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharValueTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            stringValue, expectedType,  charValue
            ",           SimpleChar,    "
            |,           SimpleChar,    |
            €,           SimpleChar,    €
            U+0020,      UnicodeEscape, ' '
            u+20aC,      UnicodeEscape, €
            TAB,         CharMnemonic,  '\t'
            space,       CharMnemonic,  ' '
            Quot,        CharMnemonic,  "
            APOS,        CharMnemonic,  ''''
            GRAVE,       CharMnemonic,  `
            """)
    void of_validValues(String stringValue, String expectedType, char charValue) {
        var characterValue = CharValue.of(stringValue);
        assertNotNull(characterValue);

        assertEquals(expectedType, characterValue.getClass().getSimpleName(), "type");
        assertEquals(charValue, characterValue.value(), "value");
        String expectedToString = expectedType.equals("CharMnemonic")
                ? stringValue.toUpperCase(Locale.ROOT)
                : stringValue;
        assertEquals(expectedToString, characterValue.toString(), "toString");
    }

    @Test
    void of_null_returnsNull() {
        assertNull(CharValue.of(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "U+20", "u+020", "U+00020", "XY" })
    void of_invalidValues_throwsIllegalArgumentException(String stringValue) {
        assertThrows(IllegalArgumentException.class, () -> CharValue.of(stringValue));
    }
    
}
