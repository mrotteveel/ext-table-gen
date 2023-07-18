// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseDatetime;

import java.io.IOException;
import java.time.temporal.TemporalAccessor;

/**
 * A data type representing the Firebird datatype {@code TIMESTAMP [WITHOUT TIME ZONE]}.
 *
 * @since 2
 */
public final class FbTimestamp extends AbstractFbDatatype<TemporalAccessor, Converter<TemporalAccessor>>
        implements FbDatetimeType<TemporalAccessor> {

    /**
     * Constructs a {@code FbTimestamp} using the default conversion.
     */
    public FbTimestamp() {
        this(null);
    }

    /**
     * Constructs a {@code FbTimestamp} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbTimestamp(Converter<TemporalAccessor> converter) {
        super(TemporalAccessor.class, converter, ParseDatetime.getDefaultTimestampInstance());
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("timestamp");
    }

    @Override
    protected void writeValueImpl(TemporalAccessor value, EncoderOutputStream out) throws IOException {
        // We require a date portion, but consider the time portion optional
        writeTimeAndDate(FbDate.getInRange(value),
                FbTime.FB_TIME_FIELD.isSupportedBy(value) ? value.get(FbTime.FB_TIME_FIELD) : 0, out);
    }

    private static void writeTimeAndDate(int date, int time, EncoderOutputStream out) throws IOException {
        out.align(8);
        out.writeInt(date);
        out.writeInt(time);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeTimeAndDate(0, 0, out);
    }

    @Override
    public FbDatatype<TemporalAccessor> withConverter(Converter<TemporalAccessor> converter) {
        if (hasConverter(converter)) return this;
        return new FbTimestamp(converter);
    }

    @Override
    public int hashCode() {
        return 31 * FbTimestamp.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof FbTimestamp that
               && this.converter().equals(that.converter());
    }

}
