// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.FloatConverter;

import java.io.IOException;

/**
 * A data type representing the Firebird datatype {@code FLOAT} (a.k.a. {@code REAL}).
 *
 * @since 3
 */
public final class FbFloat extends AbstractFbDatatype<Float, FloatConverter> implements FbFloatingPointDatatype<Float> {

    private static final FloatConverter DEFAULT_CONVERTER = new FloatConverter() {
        @Override
        public float convertToFloat(String sourceValue) {
            return Float.parseFloat(sourceValue);
        }

        @Override
        public Class<Float> targetType() {
            return Float.class;
        }

        @Override
        public String converterName() {
            return "##custom##Float.parseFloat";
        }
    };

    /**
     * Constructs a {@code FbFloat} using the default conversion.
     */
    public FbFloat() {
        this(null);
    }

    /**
     * Constructs a {@code FbFloat} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbFloat(Converter<Float> converter) {
        super(Float.class, FloatConverter.wrap(converter), DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("float");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeFloat(finalConverter().convertToFloat(value), out);
        }
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeFloat(0, out);
    }

    @Override
    protected void writeValueImpl(Float value, EncoderOutputStream out) throws IOException {
        writeFloat(value, out);
    }

    static void writeFloat(float value, EncoderOutputStream out) throws IOException {
        out.align(4);
        out.writeFloat(value);
    }

    @Override
    public FbDatatype<Float> withConverter(Converter<Float> converter) {
        FloatConverter wrappedConverter = FloatConverter.wrap(converter);
        if (hasConverter(wrappedConverter)) return this;
        return new FbFloat(wrappedConverter);
    }

    @Override
    public int hashCode() {
        return 31 * FbFloat.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbFloat that
               && this.converter().equals(that.converter());
    }

}
