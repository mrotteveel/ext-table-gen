// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Datatype specialization for integral numbers.
 *
 * @since 2
 */
sealed interface FbIntegralNumberDatatype<U extends Number> extends FbDatatype<U>
        permits FbBigint, FbInt128, FbInteger, FbSmallint {
}
