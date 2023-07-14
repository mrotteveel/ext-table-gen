// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.convert.ParseSmallint;
import nl.lawinegevaar.exttablegen.convert.ShortConverter;
import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;

/**
 * A data type representing the Firebird datatype {@code SMALLINT}.
 *
 * @since 2
 */
public final class FbSmallint extends AbstractFbDatatype<Short, ShortConverter>
        implements FbIntegralNumberDatatype<Short> {

    private static final ParseSmallint DEFAULT_CONVERTER = ParseSmallint.ofRadix(10);

    /**
     * Constructs a {@code FbSmallint} using the default conversion.
     */
    public FbSmallint() {
        this(null);
    }

    /**
     * Constructs a {@code FbSmallint} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     * @since 2
     */
    public FbSmallint(Converter<Short> converter) {
        super(Short.class, ShortConverter.wrap(converter), DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("smallint");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeShort(finalConverter().convertToShort(value), out);
        }
    }

    @Override
    protected void writeValueImpl(Short value, EncoderOutputStream out) throws IOException {
        writeShort(value, out);
    }

    private static void writeShort(short value, EncoderOutputStream out) throws IOException {
        out.align(2);
        out.writeShort(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeShort((short) 0, out);
    }

    @Override
    public FbDatatype<Short> withConverter(Converter<Short> converter) {
        ShortConverter wrappedConverter = ShortConverter.wrap(converter);
        if (hasConverter(wrappedConverter)) return this;
        return new FbSmallint(wrappedConverter);
    }

    @Override
    public int hashCode() {
        return 31 * FbSmallint.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbSmallint that
                && this.converter().equals(that.converter());
    }

}
