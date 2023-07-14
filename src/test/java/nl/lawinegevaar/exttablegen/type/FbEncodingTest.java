// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbEncodingTest {

    @ParameterizedTest
    @ValueSource(strings = { "NONE", "OCTETS", "UNICODE_FSS" })
    void forName_explicitlyNotSupported(String unsupportedFirebirdName) {
        assertThrows(IllegalArgumentException.class, () -> FbEncoding.forName(unsupportedFirebirdName));
    }

    @Test
    void forName_unknownFirebirdName() {
        assertThrows(IllegalArgumentException.class, () -> FbEncoding.forName("DOESNOTEXIST"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    void maxByteLength_singleByteEncoding(int charLength) {
        assertEquals(charLength, FbEncoding.ASCII.maxByteLength(charLength));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    void maxByteLength_max4ByteEncoding(int charLength) {
        int expectedByteLength = 4 * charLength;

        assertEquals(expectedByteLength, FbEncoding.UTF8.maxByteLength(charLength));
    }

    @ParameterizedTest
    @ValueSource(strings = { "UTF8", "utf8", "Utf8" })
    void firebirdNameAndCaseInsensitivityOfForName(String utf8Name) {
        var encoding = FbEncoding.forName(utf8Name);

        assertEquals("UTF8", encoding.firebirdName());
    }

    @Test
    void charsetName() {
        var encoding = FbEncoding.forName("ISO8859_2");

        assertEquals("ISO-8859-2", encoding.charsetName());
    }

    @Test
    void charset() {
        assertEquals(StandardCharsets.UTF_8, FbEncoding.UTF8.charset());
    }

}