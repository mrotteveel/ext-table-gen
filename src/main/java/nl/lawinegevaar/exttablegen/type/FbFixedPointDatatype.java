// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Datatype specialization for fixed point numbers.
 *
 * @since 2
 */
public sealed interface FbFixedPointDatatype extends FbDatatype<BigDecimal>
        permits AbstractFbFixedPointDatatype, FbDecimal, FbNumeric {

    /**
     * @return precision in decimal digits
     */
    int precision();

    /**
     * @return scale in decimal digits
     */
    int scale();

    /**
     * @return rounding mode to apply when rounding to the desired scale
     */
    RoundingMode roundingMode();

}
