// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

/**
 * Datatype specialization for binary floating point numbers.
 *
 * @since 3
 */
public sealed interface FbBinaryFloatingPointDatatype<T extends Number> extends FbFloatingPointDatatype<T>
        permits FbDoublePrecision, FbFloat{
}
