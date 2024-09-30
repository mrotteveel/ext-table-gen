// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbTimeTest {

    static final LocalTime TIME_ZERO = LocalTime.MIN;

    private static final FbTime timeType = new FbTime();

    @ParameterizedTest
    @MethodSource("inRangeValues")
    void testWriteValue(String localTimeString) throws Exception {
        var expectedValue = LocalTime.parse(localTimeString);

        assertEquals(expectedValue, writeAndGetValue(localTimeString));
    }

    static Stream<String> inRangeValues() {
        return Stream.of(
                "00:00:00.0000",
                "11:23:15.1234",
                "15:45:53.9",
                "23:59:59.9999");
    }

    @ParameterizedTest
    @MethodSource("outOfRangeOrInvalidValues")
    void testWriteValue_outOfRangeOrInvalid_throwsDateTimeParseException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(DateTimeParseException.class, () ->
                timeType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    static Stream<String> outOfRangeOrInvalidValues() {
        return Stream.of(
                "NOT_A_Time",
                "23:60:00",
                "24:00:00",
                "2023-07-24",
                "2023-04-07T23:23:00");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(TIME_ZERO, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        timeType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        assertEquals(TIME_ZERO, toLocalTime(baos.toByteArray()));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var converter = Converter.parseDatetime("h:mm:ss a", Locale.US);
        assertEquals(LocalTime.of(16, 0, 14), writeAndGetValue("4:00:14 PM", converter));
    }

    @Test
    void testHashCode() {
        assertEquals(timeType.hashCode(), new FbTime().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(timeType, new FbTime());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(timeType, new FbInteger());
    }

    LocalTime writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    LocalTime writeAndGetValue(String valueToWrite, @Nullable Converter<TemporalAccessor> converter)
            throws IOException {
        var baos = new ByteArrayOutputStream();
        timeType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return toLocalTime(baos.toByteArray());
    }

    LocalTime toLocalTime(byte[] bytes) {
        var buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.nativeOrder());
        long nanos = buf.getInt() * FbTime.NANOS_PER_UNIT;
        return LocalTime.ofNanoOfDay(nanos);
    }

}