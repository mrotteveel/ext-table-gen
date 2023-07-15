// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
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
            YYYY-'W'ww-e,            ,            2022-W52-7,          2023-01-01
            E d MMMM yyyy,           nl-NL,       vr 14 april 2023,    2023-04-14
            E d MMMM yyyy,           en-US,       Fri 14 April 2023,   2023-04-14
            yyyy-MM-dd['T'HH:mm:ss], ,            2021-12-13T00:00:00, 2021-12-13
            yyyy-MM-dd['T'HH:mm:ss], ,            2021-12-13,          2021-12-13
            """)
    void testWithPatternAndLocale_localDate(String pattern, String languageTag, String input, String expectedLocalDate) {
        Locale locale = languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        var parseDateTime = new ParseDatetime(pattern, locale);
        assertEquals(pattern, parseDateTime.pattern());
        assertThat(parseDateTime.locale(), locale != null ? optionalWithValue(locale) : emptyOptional());

        TemporalAccessor temporalAccessor = parseDateTime.convert(input);
        var localDate = LocalDate.from(temporalAccessor);

        assertEquals(LocalDate.parse(expectedLocalDate), localDate);
    }

}