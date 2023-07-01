// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Datatype specialization for integral numbers.
 */
sealed interface FbIntegralNumberDatatype extends FbDatatype permits FbBigint, FbInt128, FbInteger, FbSmallint {
}
