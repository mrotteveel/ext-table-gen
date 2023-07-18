// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.LongConverter;
import nl.lawinegevaar.exttablegen.convert.ParseBigint;
import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;

/**
 * A data type representing the Firebird datatype {@code BIGINT}.
 *
 * @since 2
 */
public final class FbBigint extends AbstractFbDatatype<Long, LongConverter> implements FbIntegralNumberDatatype<Long> {

    private static final ParseBigint DEFAULT_CONVERTER = ParseBigint.ofRadix(10);

    /**
     * Constructs a {@code FbBigint} using the default conversion.
     */
    public FbBigint() {
        this(null);
    }

    /**
     * Constructs a {@code FbBigint} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbBigint(Converter<Long> converter) {
        super(Long.class, LongConverter.wrap(converter), DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("bigint");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeLong(finalConverter().convertToLong(value), out);
        }
    }

    @Override
    protected void writeValueImpl(Long value, EncoderOutputStream out) throws IOException {
        writeLong(value, out);
    }

    private static void writeLong(long value, EncoderOutputStream out) throws IOException {
        out.align(8);
        out.writeLong(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeLong(0, out);
    }

    @Override
    public FbDatatype<Long> withConverter(Converter<Long> converter) {
        LongConverter wrappedConverter = LongConverter.wrap(converter);
        if (hasConverter(wrappedConverter)) return this;
        return new FbBigint(wrappedConverter);
    }

    @Override
    public int hashCode() {
        return 31 * FbBigint.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbBigint that
               && this.converter().equals(that.converter());
    }

}
