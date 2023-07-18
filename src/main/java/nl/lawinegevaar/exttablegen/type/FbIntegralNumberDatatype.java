// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

/**
 * Datatype specialization for integral numbers.
 *
 * @since 2
 */
public sealed interface FbIntegralNumberDatatype<T extends Number> extends FbDatatype<T>
        permits FbBigint, FbInt128, FbInteger, FbSmallint {
}
