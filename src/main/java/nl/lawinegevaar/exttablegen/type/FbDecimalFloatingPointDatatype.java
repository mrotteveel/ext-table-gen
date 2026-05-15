// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import org.firebirdsql.decimal.Decimal;

/**
 * Datatype specialization for decimal floating point numbers.
 * <p>
 * The current API is based on use of {@code DECFLOAT} and {@link Decimal}.
 * </p>
 *
 * @since 3
 */
public sealed interface FbDecimalFloatingPointDatatype<T extends Decimal<T>> extends FbFloatingPointDatatype<T>
        permits AbstractFbDecimalFloatingPointDatatype {

    /**
     * @return precision in decimal digits
     */
    int precision();

    DecfloatOnOverflow onOverflow();

}
