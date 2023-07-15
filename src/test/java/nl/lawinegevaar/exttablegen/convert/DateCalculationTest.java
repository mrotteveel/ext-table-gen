// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test verifies if the calculation of the Modified Julian Date as implemented in Jaybird (as of Jaybird 5)
 * matches the calculation in {@link java.time.temporal.JulianFields#MODIFIED_JULIAN_DAY}.
 */
class DateCalculationTest {

    @ParameterizedTest
    @ValueSource(strings = { "0001-01-01", "9999-12-31", "1600-07-02", "1653-06-12", "1653-06-13", "1970-01-01",
            "1970-01-02", "1980-01-30", "2023-07-10", "2023-10-07", "1979-01-12", "1979-01-13", "1979-12-01",
            "1979-12-02" })
    void compareJaybirdAgainstJavaTime(String dateString) {
        var date = LocalDate.parse(dateString);

        assertEquals(calculateJaybird(date), calculateJavaTime(date));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0001-01-01", "9999-12-31", "1600-07-02", "1653-06-12", "1653-06-13", "1970-01-01",
            "1970-01-02", "1980-01-30", "2023-07-10", "2023-10-07", "1979-01-12", "1979-01-13", "1979-12-01",
            "1979-12-02" })
    void compareJaybirdAgainstJavaTime_variant2(String dateString) {
        var temporalAccessor = DateTimeFormatter.ISO_LOCAL_DATE.parse(dateString);

        assertEquals(calculateJaybird(LocalDate.from(temporalAccessor)), calculateJavaTime(temporalAccessor));
    }

    private int calculateJavaTime(TemporalAccessor date) {
        return (int) JulianFields.MODIFIED_JULIAN_DAY.getFrom(date);
    }

    private int calculateJaybird(LocalDate date) {
        int cpMonth = date.getMonthValue();
        int cpYear = date.getYear();

        if (cpMonth > 2) {
            cpMonth -= 3;
        } else {
            cpMonth += 9;
            cpYear -= 1;
        }

        int c = cpYear / 100;
        int ya = cpYear - 100 * c;

        return ((146097 * c) / 4 +
                (1461 * ya) / 4 +
                (153 * cpMonth + 2) / 5 +
                date.getDayOfMonth() + 1721119 - 2400001);
    }

}
