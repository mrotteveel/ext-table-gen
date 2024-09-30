// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

/**
 * Datatype specialization for floating point numbers.
 *
 * @since 3
 */
public sealed interface FbFloatingPointDatatype<T extends Number> extends FbDatatype<T>
        permits FbDoublePrecision, FbFloat {
}
