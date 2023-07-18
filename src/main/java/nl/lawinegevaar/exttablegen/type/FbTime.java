// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseDatetime;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

/**
 * A data type representing the Firebird datatype {@code TIME [WITHOUT TIME ZONE]}.
 *
 * @since 2
 */
public final class FbTime extends AbstractFbDatatype<TemporalAccessor, Converter<TemporalAccessor>>
        implements FbDatetimeType<TemporalAccessor> {

    public static final TemporalUnit FB_TIME_UNIT = new FbTimeUnit();
    public static final TemporalField FB_TIME_FIELD = new FbTimeField();

    static final long MICROS_PER_UNIT = 100L;
    static final long NANOS_PER_UNIT = 100_000L;

    /**
     * Constructs a {@code FbTime} using the default conversion.
     */
    public FbTime() {
        this(null);
    }

    /**
     * Constructs a {@code FbTime} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbTime(Converter<TemporalAccessor> converter) {
        super(TemporalAccessor.class, converter, ParseDatetime.getDefaultTimeInstance());
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("time");
    }

    @Override
    protected void writeValueImpl(TemporalAccessor value, EncoderOutputStream out) throws IOException {
        writeInt(value.get(FB_TIME_FIELD), out);
    }

    private static void writeInt(int fractions, EncoderOutputStream out) throws IOException {
        out.align(4);
        out.writeInt(fractions);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeInt(0, out);
    }

    @Override
    public FbDatatype<TemporalAccessor> withConverter(Converter<TemporalAccessor> converter) {
        if (hasConverter(converter)) return this;
        return new FbTime(converter);
    }

    @Override
    public int hashCode() {
        return 31 * FbTime.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof FbTime that
               && this.converter().equals(that.converter());
    }

    /**
     * A {@link TemporalField} to query {@code java.time} objects for time in {@link FbTimeUnit}.
     */
    static final class FbTimeField implements TemporalField {

        // max is number of 100 microseconds in a day
        private static final ValueRange RANGE = ValueRange.of(0, 24 * 60 * 60 * 10_000 - 1);

        private FbTimeField() {
            // effectively singleton
        }

        @Override
        public TemporalUnit getBaseUnit() {
            return FB_TIME_UNIT;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return ChronoUnit.DAYS;
        }

        @Override
        public ValueRange range() {
            return RANGE;
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            // We could also use MICRO_OF_DAY, but generally that is implemented in terms of NANO_OF_DAY
            return temporal.isSupported(ChronoField.NANO_OF_DAY);
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (!isSupportedBy(temporal)) {
                throw new DateTimeException("Unsupported field: " + this);
            }
            return range();
        }

        @Override
        public long getFrom(TemporalAccessor temporal) {
            // We could also use MICRO_OF_DAY, but generally that is implemented in terms of NANO_OF_DAY
            return temporal.getLong(ChronoField.NANO_OF_DAY) / NANOS_PER_UNIT;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            // We could also use MICRO_OF_DAY, but generally that is implemented in terms of NANO_OF_DAY
            // No need for Math.multiplyExact, as the range check makes sure there won't be an overflow
            return (R) temporal.with(ChronoField.NANO_OF_DAY, RANGE.checkValidValue(newValue, this) * NANOS_PER_UNIT);
        }

        @Override
        public String toString() {
            return "FbTimeField";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FbTimeField;
        }

        @Override
        public int hashCode() {
            return FbTimeField.class.hashCode();
        }

    }

    /**
     * A {@link TemporalUnit} representing 100 microseconds, the unit of time used by Firebird.
     */
    static final class FbTimeUnit implements TemporalUnit {

        // A Firebird time unit is expressed in "fractions", which are 100 microseconds
        private static final Duration UNIT_DURATION = Duration.ofNanos(NANOS_PER_UNIT);

        private FbTimeUnit() {
            // effectively singleton
        }

        @Override
        public Duration getDuration() {
            return UNIT_DURATION;
        }

        @Override
        public boolean isDurationEstimated() {
            return false;
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return (R) temporal.plus(Math.multiplyExact(amount, MICROS_PER_UNIT), ChronoUnit.MICROS);
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.MICROS) / MICROS_PER_UNIT;
        }

        @Override
        public String toString() {
            return "FbTimeUnit";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FbTimeUnit;
        }

        @Override
        public int hashCode() {
            return FbTimeUnit.class.hashCode();
        }

    }

}
