// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

/**
 * Datatype specialization for character data types ({@code CHAR} and {@code VARCHAR}).
 * <p>
 * In the future, this type may also get used for {@code (VAR)BINARY}.
 * </p>
 *
 * @since 3
 */
public sealed interface FbCharacterDataType<T> extends FbDatatype<T> permits AbstractFbStringDataType {

    /**
     * @return length (in Unicode codepoints for string types)
     */
    int length();

    FbEncoding encoding();

}
