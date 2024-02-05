// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.convert.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A data type representing the Firebird datatype {@code DECIMAL}.
 *
 * @since 2
 */
public final class FbDecimal extends AbstractFbFixedPointDatatype implements FbFixedPointDatatype {

    /**
     * Constructs a {@code FbDecimal} with the default conversion
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
    public FbDecimal(int precision, int scale, RoundingMode roundingMode) {
        this(null, precision, scale, roundingMode);
    }

    /**
     * Constructs a {@code FbDecimal} with the specified converter
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
    public FbDecimal(Converter<BigDecimal> converter, int precision, int scale, RoundingMode roundingMode) {
        super(converter, decimalBackingType(precision), precision, scale, roundingMode);
    }

    @Override
    String typeName() {
        return "decimal";
    }

    @Override
    public FbDecimal withConverter(Converter<BigDecimal> converter) {
        if (hasConverter(converter)) return this;
        return new FbDecimal(converter, precision(), scale(), roundingMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(FbDecimal.class, precision(), scale(), roundingMode(), converter());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbDecimal that
               && this.precision() == that.precision()
               && this.scale() == that.scale()
               && this.roundingMode() == that.roundingMode()
               && this.converter().equals(that.converter());
    }

    private static BackingType decimalBackingType(int precision) {
        if (precision < 1 || precision > 38) {
            throw new IllegalArgumentException("precision must be between 1 and 38");
        } else if (precision <= 9) {
            return BackingType.INTEGER;
        } else if (precision <= 18) {
            return BackingType.BIGINT;
        } else {
            return BackingType.INT128;
        }
    }

}
