// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseDatetime;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;

/**
 * A data type representing the Firebird datatype {@code DATE}.
 *
 * @since 2
 */
public final class FbDate extends AbstractFbDatatype<TemporalAccessor, Converter<TemporalAccessor>>
        implements FbDatetimeType<TemporalAccessor> {

    // Firebird DATE has a minimum of 0001-01-01
    // Derived from JulianFields.MODIFIED_JULIAN_DAY.getFrom(LocalDate.of(1, 1, 1));
    private static final long MIN_VALUE = -678575;
    // Firebird DATE has a maximum of 9999-12-31
    // Derived from JulianFields.MODIFIED_JULIAN_DAY.getFrom(LocalDate.of(9999, 12, 31));
    private static final long MAX_VALUE = 2973483;

    /**
     * Constructs a {@code FbDate} using the default conversion.
     */
    public FbDate() {
        this(null);
    }

    /**
     * Constructs a {@code FbDate} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbDate(Converter<TemporalAccessor> converter) {
        super(TemporalAccessor.class, converter, ParseDatetime.getDefaultDateInstance());
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("date");
    }

    @Override
    protected void writeValueImpl(TemporalAccessor value, EncoderOutputStream out) throws IOException {
        writeInt(getInRange(value), out);
    }

    private static void writeInt(int modifiedJulianDate, EncoderOutputStream out) throws IOException {
        out.align(4);
        out.writeInt(modifiedJulianDate);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeInt(0, out);
    }

    @Override
    public FbDatatype<TemporalAccessor> withConverter(Converter<TemporalAccessor> converter) {
        if (hasConverter(converter)) return this;
        return new FbDate(converter);
    }

    @Override
    public int hashCode() {
        return 31 * FbDate.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof FbDate that
               && this.converter().equals(that.converter());
    }

    /**
     * Gets the Modified Julian Date and checks if it is in the range supported by Firebird.
     *
     * @param value
     *         temporal accessor to query
     * @return the Modified Julian Date value
     * @throws DateTimeException
     *         if the value is out of range [0001-01-01, 9999-12-31]
     */
    static int getInRange(TemporalAccessor value) {
        long modifiedJulianDate = JulianFields.MODIFIED_JULIAN_DAY.getFrom(value);
        if (MIN_VALUE > modifiedJulianDate || modifiedJulianDate > MAX_VALUE) {
            throw new DateTimeException("Value is out of range, date must be in range [0001-01-01, 9999-12-31]");
        }
        return (int) modifiedJulianDate;
    }

}
