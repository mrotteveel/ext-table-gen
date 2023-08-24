// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.convert.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A data type representing the Firebird datatype {@code NUMERIC}.
 *
 * @since 2
 */
public final class FbNumeric extends AbstractFbFixedPointDatatype implements FbFixedPointDatatype {

    /**
     * Constructs a {@code FbNumeric} with the default conversion
     *
     * @param precision
     *         precision (must be between 1 and 38)
     * @param scale
     *         scale (must be between 0 and {@code precision})
     * @param roundingMode
     *         rounding mode ({@code null} will apply {@link RoundingMode#HALF_UP})
     * @throws IllegalArgumentException
     *         if {@code precision} or {@code scale} are out of range
     */
    public FbNumeric(int precision, int scale, RoundingMode roundingMode) {
        this(null, precision, scale, roundingMode);
    }

    /**
     * Constructs a {@code FbNumeric} with the specified converter
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     * @param precision
     *         precision (must be between 1 and 38)
     * @param scale
     *         scale (must be between 0 and {@code precision})
     * @param roundingMode
     *         rounding mode ({@code null} will apply {@link RoundingMode#HALF_UP})
     * @throws IllegalArgumentException
     *         if {@code precision} or {@code scale} are out of range
     */
    public FbNumeric(Converter<BigDecimal> converter, int precision, int scale, RoundingMode roundingMode) {
        super(converter, numericBackingType(precision), precision, scale, roundingMode);
    }

    @Override
    String typeName() {
        return "numeric";
    }

    @Override
    public FbNumeric withConverter(Converter<BigDecimal> converter) {
        if (hasConverter(converter)) return this;
        return new FbNumeric(converter, precision(), scale(), roundingMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(FbNumeric.class, precision(), scale(), roundingMode(), converter());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbNumeric that
               && this.precision() == that.precision()
               && this.scale() == that.scale()
               && this.roundingMode() == that.roundingMode()
               && this.converter().equals(that.converter());
    }

    private static BackingType numericBackingType(int precision) {
        if (precision < 1 || precision > 38) {
            throw new IllegalArgumentException("precision must be between 1 and 38");
        } else if (precision <= 4) {
            return BackingType.SMALLINT;
        } else if (precision <= 9) {
            return BackingType.INTEGER;
        } else if (precision <= 18) {
            return BackingType.BIGINT;
        } else {
            return BackingType.INT128;
        }
    }

}
