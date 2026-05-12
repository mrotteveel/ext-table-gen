// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import org.firebirdsql.decimal.OverflowHandling;

/**
 * Overflow behaviour for decfloat
 *
 * @since 3
 */
public enum DecfloatOnOverflow {

    ROUND_TO_INFINITY(OverflowHandling.ROUND_TO_INFINITY),
    THROW_EXCEPTION(OverflowHandling.THROW_EXCEPTION),
    ;

    private final OverflowHandling overflowHandling;

    DecfloatOnOverflow(OverflowHandling overflowHandling) {
        this.overflowHandling = overflowHandling;
    }

    OverflowHandling overflowHandling() {
        return overflowHandling;
    }

}
