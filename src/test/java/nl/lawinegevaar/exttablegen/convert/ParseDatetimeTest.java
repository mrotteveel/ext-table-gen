// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseDatetimeTest {

    @Test
    void rejectInvalidPattern() {
        assertThrows(IllegalArgumentException.class, () -> new ParseDatetime("INVALID", null));
    }

    @Test
    void testDefaultDateInstance() {
        String input = "2021-12-13";
        TemporalAccessor temporalAccessor = ParseDatetime.getDefaultDateInstance().convert(input);
        var localDate = LocalDate.from(temporalAccessor);

        assertEquals(LocalDate.parse(input), localDate);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,                 languageTag, input,               expectedLocalDate
            ISO_LOCAL_DATE,          ,            2021-12-13,          2021-12-13
            yyyy-MM-dd,              ,            2021-12-13,          2021-12-13
            # ISO 8601 rules with week starting on Monday
            YYYY-'W'ww-e,            nl-NL,       2022-W52-7,          2023-01-01
            # US rules with week starting on Sunday
            YYYY-'W'ww-e,            en-US,       2022-W52-7,          2022-12-24
            E d MMMM yyyy,           nl-NL,       vr 14 april 2023,    2023-04-14
            E d MMMM yyyy,           en-US,       Fri 14 April 2023,   2023-04-14
            yyyy-MM-dd['T'HH:mm:ss], ,            2021-12-13T00:00:00, 2021-12-13
            yyyy-MM-dd['T'HH:mm:ss], ,            2021-12-13,          2021-12-13
            """)
    void testWithPatternAndLocale_localDate(String pattern, @Nullable String languageTag, String input,
            LocalDate expectedLocalDate) {
        Locale locale = languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        var parseDateTime = new ParseDatetime(pattern, locale);
        assertEquals(pattern, parseDateTime.pattern());
        assertThat(parseDateTime.locale(), locale != null ? optionalWithValue(locale) : emptyOptional());

        TemporalAccessor temporalAccessor = parseDateTime.convert(input);
        var localDate = LocalDate.from(temporalAccessor);

        assertEquals(expectedLocalDate, localDate);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,        languageTag, input,         expectedLocalTime
            ISO_LOCAL_TIME, ,            15:57:23.1234, 15:57:23.1234
            h:mm:ss a,      en-US,       3:57:23 PM,    15:57:23
            """)
    void testWithPatternAndLocale_localTime(String pattern, @Nullable String languageTag, String input,
            String expectedLocalTime) {
        Locale locale = languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        var parseDateTime = new ParseDatetime(pattern, locale);
        assertEquals(pattern, parseDateTime.pattern());
        assertThat(parseDateTime.locale(), locale != null ? optionalWithValue(locale) : emptyOptional());

        TemporalAccessor temporalAccessor = parseDateTime.convert(input);
        var localTime = LocalTime.from(temporalAccessor);

        assertEquals(LocalTime.parse(expectedLocalTime), localTime);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,                        languageTag, input,                              expectedLocalDateTime
            ISO_LOCAL_DATE_TIME,            ,            2023-07-17T15:57:23.1234,           2023-07-17T15:57:23.1234
            SQL_TIMESTAMP,                  ,            2023-07-17 15:57:23.1234,           2023-07-17T15:57:23.1234
            yyyy-MM-dd['T'HH:mm:ss],        ,            2023-07-17T15:57:23,                2023-07-17T15:57:23
            E d MMMM yyyy HH:mm:ss.SS,      nl-NL,       ma 17 juli 2023 15:57:23.12,        2023-07-17T15:57:23.12
            'E, MMMM, d yyyy h:mm:ss.SS a', en-US,       'Mon, July, 17 2023 3:57:23.12 PM', 2023-07-17T15:57:23.12
            yyyy-MM-dd['T'HH:mm:ss],        ,            2023-07-17T15:57:23,                2023-07-17T15:57:23
            """)
    void testWithPatternAndLocale_localDateTime(String pattern, @Nullable String languageTag, String input,
            String expectedLocalDateTime) {
        Locale locale = languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        var parseDateTime = new ParseDatetime(pattern, locale);
        assertEquals(pattern, parseDateTime.pattern());
        assertThat(parseDateTime.locale(), locale != null ? optionalWithValue(locale) : emptyOptional());

        TemporalAccessor temporalAccessor = parseDateTime.convert(input);
        var localDateTime = LocalDateTime.from(temporalAccessor);

        assertEquals(LocalDateTime.parse(expectedLocalDateTime), localDateTime);
    }

}