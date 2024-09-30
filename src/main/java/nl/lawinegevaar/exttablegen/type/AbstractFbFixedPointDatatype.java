// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

/**
 * Common implementation for fixed point number data type ({@code NUMERIC} and {@code DECIMAL}).
 *
 * @since 2
 */
public abstract sealed class AbstractFbFixedPointDatatype extends AbstractFbDatatype<BigDecimal, Converter<BigDecimal>>
        implements FbFixedPointDatatype permits FbDecimal, FbNumeric {

    private static final Converter<BigDecimal> DEFAULT_CONVERTER = Converter.of(BigDecimal.class, BigDecimal::new);

    private final BackingType backingType;
    private final int precision;
    private final int scale;
    private final RoundingMode roundingMode;

    AbstractFbFixedPointDatatype(@Nullable Converter<BigDecimal> converter, BackingType backingType,
            int precision, int scale, @Nullable RoundingMode roundingMode) {
        super(BigDecimal.class, converter, DEFAULT_CONVERTER);
        if (precision < 1 || precision > 38) {
            throw new IllegalArgumentException("precision must be between 1 and 38");
        }
        if (scale < 0 || scale > precision) {
            throw new IllegalArgumentException("scale must be between 0 and precision (= %s)".formatted(precision));
        }
        this.backingType = requireNonNull(backingType, "backingType");
        this.precision = precision;
        this.scale = scale;
        this.roundingMode = roundingMode != null ? roundingMode : RoundingMode.HALF_UP;
    }

    @Override
    public final int precision() {
        return precision;
    }

    @Override
    public final int scale() {
        return scale;
    }

    @Override
    public final RoundingMode roundingMode() {
        return roundingMode;
    }

    @Override
    public final void appendTypeDefinition(StringBuilder sb) {
        sb.append(typeName()).append('(').append(precision).append(',').append(scale).append(')');
    }

    /**
     * @return Firebird name of this data type (in lowercase)
     */
    abstract String typeName();

    @Override
    public final void writeEmpty(EncoderOutputStream out) throws IOException {
        backingType.writeValue(BigInteger.ZERO, out);
    }

    @Override
    protected final void writeValueImpl(BigDecimal value, EncoderOutputStream out) throws IOException {
        backingType.writeValue(value.setScale(scale, roundingMode).unscaledValue(), out);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
               "precision=" + precision +
               ", scale=" + scale +
               ", roundingMode=" + roundingMode +
               ", converter=" + converter().orElse(null) +
               '}';
    }

    enum BackingType {
        SMALLINT {
            @Override
            void writeValue(BigInteger unscaledValue, EncoderOutputStream out) throws IOException {
                FbSmallint.writeShort(unscaledValue.shortValueExact(), out);
            }
        },
        INTEGER {
            @Override
            void writeValue(BigInteger unscaledValue, EncoderOutputStream out) throws IOException {
                FbInteger.writeInt(unscaledValue.intValueExact(), out);
            }
        },
        BIGINT {
            @Override
            void writeValue(BigInteger unscaledValue, EncoderOutputStream out) throws IOException {
                FbBigint.writeLong(unscaledValue.longValueExact(), out);
            }
        },
        INT128 {
            @Override
            void writeValue(BigInteger unscaledValue, EncoderOutputStream out) throws IOException {
                FbInt128.writeBigInteger(unscaledValue, out);
            }
        };

        /**
         * Writes an unscaled value as the backing type.
         * <p>
         * NOTE: If the value does not fit the backing data type, this method may throw a runtime exception. This is
         * generally a {@link NumberFormatException} or an {@link ArithmeticException}.
         * </p>
         *
         * @param unscaledValue
         *         unscaled value, after proper scaling and rounding
         * @param out
         *         encoder output stream
         * @throws IOException
         *         for errors writing the value to the stream
         */
        abstract void writeValue(BigInteger unscaledValue, EncoderOutputStream out) throws IOException;

    }

}
