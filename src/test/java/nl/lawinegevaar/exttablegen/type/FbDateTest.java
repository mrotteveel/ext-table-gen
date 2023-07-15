// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbDateTest {

    private static final LocalDate MODIFIED_JULIAN_DATE_EPOCH_ZERO = LocalDate.of(1858, 11, 17);
    
    private static final FbDate dateType = new FbDate();

    @ParameterizedTest
    @MethodSource("inRangeValues")
    void testWriteValue(String localDateString) throws Exception {
        var expectedValue = LocalDate.parse(localDateString);

        assertEquals(expectedValue, writeAndGetValue(localDateString));
    }

    static Stream<String> inRangeValues() {
        return Stream.of(
                "0001-01-01",
                "2023-07-24",
                "9999-12-31");
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void testWriteValue_invalid_throwsDateTimeParseException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(DateTimeParseException.class, () ->
                dateType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    static Stream<String> invalidValues() {
        return Stream.of(
                "NOT_A_DATE",
                "23:23:00",
                "2023-04-07T23:23:00");
    }

    @ParameterizedTest
    @MethodSource("outOfRangeValues")
    void testWriteValue_outOfRange_throwsDateTimeException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        var exception = assertThrows(DateTimeException.class, () ->
                dateType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
        assertEquals("Value is out of range, date must be in range [0001-01-01, 9999-12-31]", exception.getMessage());
    }

    static Stream<String> outOfRangeValues() {
        return Stream.of(
                "0000-12-31",
                "+10000-01-01");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(MODIFIED_JULIAN_DATE_EPOCH_ZERO, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var converter = Converter.parseDatetime("dd-MM-yyyy", (Locale) null);
        assertEquals(LocalDate.of(2013, 12, 1), writeAndGetValue("01-12-2013", converter));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        dateType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        assertEquals(MODIFIED_JULIAN_DATE_EPOCH_ZERO, toLocalDate(baos.toByteArray()));
    }

    @Test
    void testHashCode() {
        assertEquals(dateType.hashCode(), new FbDate().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(dateType, new FbDate());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(dateType, new FbInteger());
    }

    LocalDate writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    LocalDate writeAndGetValue(String valueToWrite, Converter<TemporalAccessor> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        dateType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return toLocalDate(baos.toByteArray());
    }

    LocalDate toLocalDate(byte[] bytes) {
        var buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.nativeOrder());
        int modifiedJulianDate = buf.getInt();
        // NOTE: The validity of using MODIFIED_JULIAN_DAY is verified in DateCalculationTest
        return LocalDate.EPOCH.with(JulianFields.MODIFIED_JULIAN_DAY, modifiedJulianDate);
    }

}