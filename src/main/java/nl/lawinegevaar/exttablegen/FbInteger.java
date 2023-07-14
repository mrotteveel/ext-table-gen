// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.convert.IntConverter;
import nl.lawinegevaar.exttablegen.convert.ParseInteger;
import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;

/**
 * A data type representing the Firebird datatype {@code INTEGER}.
 *
 * @since 2
 */
public final class FbInteger extends AbstractFbDatatype<Integer, IntConverter>
        implements FbIntegralNumberDatatype<Integer> {

    private static final ParseInteger DEFAULT_CONVERTER = ParseInteger.ofRadix(10);

    /**
     * Constructs a {@code FbInteger} using the default conversion.
     */
    public FbInteger() {
        this(null);
    }

    /**
     * Constructs a {@code FbInteger} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     * @since 2
     */
    public FbInteger(Converter<Integer> converter) {
        super(Integer.class, IntConverter.wrap(converter), DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("integer");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeInt(finalConverter().convertToInt(value), out);
        }
    }

    @Override
    protected void writeValueImpl(Integer value, EncoderOutputStream out) throws IOException {
        writeInt(value, out);
    }

    private static void writeInt(int value, EncoderOutputStream out) throws IOException {
        out.align(4);
        out.writeInt(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeInt(0, out);
    }

    @Override
    public FbInteger withConverter(Converter<Integer> converter) {
        IntConverter wrappedConverter = IntConverter.wrap(converter);
        if (hasConverter(wrappedConverter)) return this;
        return new FbInteger(wrappedConverter);
    }

    @Override
    public int hashCode() {
        return 31 * FbInteger.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbInteger that
               && this.converter().equals(that.converter());
    }

}
