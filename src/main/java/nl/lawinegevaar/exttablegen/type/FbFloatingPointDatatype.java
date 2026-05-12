// SPDX-FileCopyrightText: Copyright 2024-2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

/**
 * Datatype specialization for floating point numbers.
 *
 * @since 3
 */
public sealed interface FbFloatingPointDatatype<T> extends FbDatatype<T>
        permits FbBinaryFloatingPointDatatype, FbDecimalFloatingPointDatatype {
}
