// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlSyntaxUtilsTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            a,    "a"
            abc,  "abc"
            "a",  "a"
            a"b,  "a""b"
            "ab,  "\""ab"
            ab",  "ab"\""
            a""b, "a"\"""b"
            """)
    void enquoteIdentifier(String input, String expectedOutput) {
        assertEquals(expectedOutput, SqlSyntaxUtils.enquoteIdentifier(input));
    }

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', textBlock = """
            a,      'a'
            a'b,    'a''b'
            'a''b', '''a''''b'''
            """)
    void enquoteLiteral(String input, String expectedOutput) {
        assertEquals(expectedOutput, SqlSyntaxUtils.enquoteLiteral(input));
    }
}