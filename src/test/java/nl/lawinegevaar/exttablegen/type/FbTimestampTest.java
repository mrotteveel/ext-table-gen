// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbTimestampTest {

    static final LocalDateTime TIMESTAMP_ZERO = LocalDateTime.of(FbDateTest.DATE_ZERO, FbTimeTest.TIME_ZERO);

    private static final FbTimestamp timestampType = new FbTimestamp();

    @ParameterizedTest
    @MethodSource("inRangeValues")
    void testWriteValue(String localDateTimeString) throws Exception {
        var expectedValue = LocalDateTime.parse(localDateTimeString);

        assertEquals(expectedValue, writeAndGetValue(localDateTimeString));
    }

    static Stream<String> inRangeValues() {
        return Stream.of(
                "0001-01-01T00:00:00.0000",
                "2023-07-24T11:23:15.1234",
                "1980-01-01T15:45:53.9",
                "9999-12-31T23:59:59.9999");
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void testWriteValue_invalid_throwsDateTimeParseException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(DateTimeParseException.class, () ->
                timestampType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    static Stream<String> invalidValues() {
        return Stream.of(
                "NOT_A_DATE",
                "23:23:00",
                "2023-04-07",
                "2023-04-07T24:00:00");
    }

    @ParameterizedTest
    @MethodSource("outOfRangeValues")
    void testWriteValue_outOfRange_throwsDateTimeException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        var exception = assertThrows(DateTimeException.class, () ->
                timestampType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
        assertEquals("Value is out of range, date must be in range [0001-01-01, 9999-12-31]", exception.getMessage());
    }

    static Stream<String> outOfRangeValues() {
        return Stream.of(
                "0000-12-31T23:59:59.9999",
                "+10000-01-01T00:00");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(TIMESTAMP_ZERO, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        timestampType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        assertEquals(TIMESTAMP_ZERO, toLocalDateTime(baos.toByteArray()));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,             locale, value,                 expectedLocalDateTime
            # Time part is not required
            dd-MM-yyyy,          ,       18-07-2023,            2023-07-18T00:00
            SQL_TIMESTAMP,       ,       2023-07-18 11:47,      2023-07-18T11:47
            M-dd-yyyy h:mm:ss a, en-US,  7-18-2023 11:47:00 AM, 2023-07-18T11:47
            """)
    void testWriteValue_callsNonNullConverter(String pattern, @Nullable String localeString, String value,
            LocalDateTime expectedLocalDateTime) throws Exception {
        var converter =
                Converter.parseDatetime(pattern, localeString != null ? Locale.forLanguageTag(localeString) : null);
        assertEquals(expectedLocalDateTime, writeAndGetValue(value, converter));
    }

    @Test
    void testWriteValue_requiresDatePart() {
        var converter = Converter.parseDatetime("ISO_LOCAL_TIME", (Locale) null);
        assertThrows(DateTimeException.class, () -> writeAndGetValue("15:34:45", converter));
    }

    @Test
    void testHashCode() {
        assertEquals(timestampType.hashCode(), new FbTimestamp().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(timestampType, new FbTimestamp());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(timestampType, new FbInteger());
    }

    LocalDateTime writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    LocalDateTime writeAndGetValue(String valueToWrite, @Nullable Converter<TemporalAccessor> converter)
            throws IOException {
        var baos = new ByteArrayOutputStream();
        timestampType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return toLocalDateTime(baos.toByteArray());
    }

    LocalDateTime toLocalDateTime(byte[] bytes) {
        var buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.nativeOrder());
        int modifiedJulianDate = buf.getInt();
        int timeFractions = buf.getInt();
        return TIMESTAMP_ZERO
                .with(JulianFields.MODIFIED_JULIAN_DAY, modifiedJulianDate)
                .with(FbTime.FB_TIME_FIELD, timeFractions);
    }

}