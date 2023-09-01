// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVParser;
import com.opencsv.RFC4180Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.escapeChar;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.ignoreLeadingWhiteSpace;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.ignoreQuotations;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.quoteChar;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.separator;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.strictQuotes;
import static nl.lawinegevaar.exttablegen.CsvParserConfigMatchers.type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CsvParserConfigTest {

    private static final CsvType ALL_FIELDS_TYPE = CsvType.CUSTOM;
    private static final CharValue ALL_FIELDS_QUOTE_CHAR = CharValue.of("APOS");
    private static final CharValue ALL_FIELDS_SEPARATOR = CharValue.of("TAB");
    private static final CharValue ALL_FIELDS_ESCAPE_CHAR = CharValue.of('\\');
    private static final boolean ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE = true;
    private static final boolean ALL_FIELDS_IGNORE_QUOTATIONS = false;
    private static final boolean ALL_FIELDS_STRICT_QUOTES = true;
    private static final CsvParserConfig ALL_FIELDS_SET =
            new CsvParserConfig(ALL_FIELDS_TYPE, ALL_FIELDS_QUOTE_CHAR, ALL_FIELDS_SEPARATOR, ALL_FIELDS_ESCAPE_CHAR,
                    ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE, ALL_FIELDS_IGNORE_QUOTATIONS, ALL_FIELDS_STRICT_QUOTES);

    @Test
    void createWithTypeNull_usesRFC_4180() {
        assertThat(new CsvParserConfig(null, null, null, null, null, null, null), type(is(CsvType.RFC_4180)));
    }

    @Test
    void defaults() {
        assertThat(CsvParserConfig.of(),
                allOf(type(is(CsvType.RFC_4180)),
                        quoteChar(is(nullValue(CharValue.class))),
                        separator(is(nullValue(CharValue.class))),
                        escapeChar(is(nullValue(CharValue.class))),
                        ignoreLeadingWhiteSpace(is(nullValue(Boolean.class))),
                        ignoreQuotations(is(nullValue(Boolean.class))),
                        strictQuotes(is(nullValue(Boolean.class)))));
    }

    @Test
    void rfc4180() {
        assertThat(CsvParserConfig.rfc4180(CharValue.of("APOS"), CharValue.of("TAB")),
                allOf(type(is(CsvType.RFC_4180)),
                        quoteChar(is(CharValue.of("APOS"))),
                        separator(is(CharValue.of("TAB"))),
                        escapeChar(is(nullValue(CharValue.class))),
                        ignoreLeadingWhiteSpace(is(nullValue(Boolean.class))),
                        ignoreQuotations(is(nullValue(Boolean.class))),
                        strictQuotes(is(nullValue(Boolean.class)))));
    }

    @Test
    void custom() {
        assertThat(
                CsvParserConfig.custom(CharValue.of("APOS"), CharValue.of("TAB"), CharValue.of('\\'), true, false, true),
                allOf(type(is(CsvType.CUSTOM)),
                        quoteChar(is(CharValue.of("APOS"))),
                        separator(is(CharValue.of("TAB"))),
                        escapeChar(is(CharValue.of('\\'))),
                        ignoreLeadingWhiteSpace(is(true)),
                        ignoreQuotations(is(false)),
                        strictQuotes(is(true))));
    }

    @ParameterizedTest
    @EnumSource(CsvType.class)
    @NullSource
    void withType(CsvType type) {
        assertThat(ALL_FIELDS_SET.withType(type),
                allOf(type(is(type != null ? type : CsvType.RFC_4180)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withQuoteChar() {
        assertThat(ALL_FIELDS_SET.withQuoteChar(CharValue.of("GRAVE")),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(CharValue.of("GRAVE"))),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withSeparator() {
        assertThat(ALL_FIELDS_SET.withSeparator(CharValue.of('#')),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(CharValue.of('#'))),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withEscapeChar() {
        assertThat(ALL_FIELDS_SET.withEscapeChar(CharValue.of('$')),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(CharValue.of('$'))),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withIgnoreLeadingWhiteSpace() {
        assertThat(ALL_FIELDS_SET.withIgnoreLeadingWhiteSpace(!ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(!ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withIgnoreQuotations() {
        assertThat(ALL_FIELDS_SET.withIgnoreQuotations(!ALL_FIELDS_IGNORE_QUOTATIONS),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(!ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(ALL_FIELDS_STRICT_QUOTES))));
    }

    @Test
    void withStrictQuotes() {
        assertThat(ALL_FIELDS_SET.withStrictQuotes(!ALL_FIELDS_STRICT_QUOTES),
                allOf(type(is(ALL_FIELDS_TYPE)),
                        quoteChar(is(ALL_FIELDS_QUOTE_CHAR)),
                        separator(is(ALL_FIELDS_SEPARATOR)),
                        escapeChar(is(ALL_FIELDS_ESCAPE_CHAR)),
                        ignoreLeadingWhiteSpace(is(ALL_FIELDS_IGNORE_LEADING_WHITE_SPACE)),
                        ignoreQuotations(is(ALL_FIELDS_IGNORE_QUOTATIONS)),
                        strictQuotes(is(!ALL_FIELDS_STRICT_QUOTES))));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, nullValues = "null", textBlock =
            """
            quoteChar, separator
            null,      null
            APOS,      null
            null,      TAB
            APOS,      TAB
            """)
    void createParser_rcf4180(CharValue quoteChar, CharValue separator) {
        // NOTE: Populating parameters not used for RFC_4180 with non-null values to show they are ignored
        var config = new CsvParserConfig(CsvType.RFC_4180, quoteChar, separator, CharValue.of('#'), false, false, true);
        var parser = assertInstanceOf(RFC4180Parser.class, config.createParser());

        assertEquals(charValueOr(quoteChar, '"'), parser.getQuotechar(), "quoteChar");
        assertEquals(charValueOr(separator, ','), parser.getSeparator(), "separator");
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, nullValues = "null", textBlock =
            """
            quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace, ignoreQuotations, strictQuotes
            null,      null,      null,       null,                    null,             null
            APOS,      null,      #,          null,                    true,             null
            null,      TAB,       null,       false,                   null,             true
            APOS,      TAB,       #,          false,                   true,             true
            """)
    void createParser_custom(CharValue quoteChar, CharValue separator, CharValue escapeChar,
            Boolean ignoreLeadingWhiteSpace, Boolean ignoreQuotations, Boolean strictQuotes) {
        var config = new CsvParserConfig(CsvType.CUSTOM, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
        var parser = assertInstanceOf(CSVParser.class, config.createParser());

        assertEquals(charValueOr(quoteChar, '"'), parser.getQuotechar(), "quoteChar");
        assertEquals(charValueOr(separator, ','), parser.getSeparator(), "separator");
        assertEquals(charValueOr(escapeChar, '\\'), parser.getEscape(), "escapeChar");
        assertEquals(booleanOr(ignoreLeadingWhiteSpace, true), parser.isIgnoreLeadingWhiteSpace(),
                "ignoreLeadingWhiteSpace");
        assertEquals(booleanOr(ignoreQuotations, false), parser.isIgnoreQuotations(), "ignoreQuotations");
        assertEquals(booleanOr(strictQuotes, false), parser.isStrictQuotes(), "strictQuotes");
    }

    private static char charValueOr(CharValue charValue, char defaultValue) {
        return charValue != null ? charValue.value() : defaultValue;
    }

    private static boolean booleanOr(Boolean booleanValue, boolean defaultValue) {
        return booleanValue != null ? booleanValue : defaultValue;
    }

}