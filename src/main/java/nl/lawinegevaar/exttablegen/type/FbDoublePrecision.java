// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.DoubleConverter;

import java.io.IOException;

/**
 * A data type representing the Firebird datatype {@code DOUBLE PRECISION}.
 *
 * @since 3
 */
public final class FbDoublePrecision extends AbstractFbDatatype<Double, DoubleConverter>
        implements FbFloatingPointDatatype<Double> {

    private static final DoubleConverter DEFAULT_CONVERTER = new DoubleConverter() {
        @Override
        public double convertToDouble(String sourceValue) {
            return Double.parseDouble(sourceValue);
        }

        @Override
        public Class<Double> targetType() {
            return Double.class;
        }

        @Override
        public String converterName() {
            return "##custom##Double.parseDouble";
        }
    };

    /**
     * Constructs a {@code FbDoublePrecision} using the default conversion.
     */
    public FbDoublePrecision() {
        this(null);
    }

    /**
     * Constructs a {@code FbDoublePrecision} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbDoublePrecision(Converter<Double> converter) {
        super(Double.class, DoubleConverter.wrap(converter), DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("double precision");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeDouble(finalConverter().convert(value), out);
        }
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeDouble(0, out);
    }

    @Override
    protected void writeValueImpl(Double value, EncoderOutputStream out) throws IOException {
        writeDouble(value, out);
    }

    static void writeDouble(double value, EncoderOutputStream out) throws IOException {
        out.align(8);
        out.writeDouble(value);
    }

    @Override
    public FbDatatype<Double> withConverter(Converter<Double> converter) {
        DoubleConverter wrappedConverter = DoubleConverter.wrap(converter);
        if (hasConverter(wrappedConverter)) return this;
        return new FbDoublePrecision(wrappedConverter);
    }

    @Override
    public int hashCode() {
        return 31 * FbDoublePrecision.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbDoublePrecision that
               && this.converter().equals(that.converter());
    }

}
