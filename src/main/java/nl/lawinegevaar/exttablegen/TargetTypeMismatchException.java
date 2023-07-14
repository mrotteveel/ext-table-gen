// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.convert.Converter;

/**
 * Thrown when target types (e.g. between a {@link FbDatatype} and {@link Converter}) do not match (or are
 * incompatible).
 *
 * @since 2
 */
public final class TargetTypeMismatchException extends ExtTableGenException {

    public TargetTypeMismatchException(String message) {
        super(message);
    }

}
