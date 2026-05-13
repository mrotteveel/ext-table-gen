// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.nio.file.Path;

/**
 * A dual path reference to a local file and its equivalent path in a Firebird container.
 *
 * @since 3
 */
interface FirebirdTestPath {

    /**
     * @return local path in the local data directory mapped to the Firebird container
     */
    Path localPath();

    /**
     * @return equivalent absolute path in the Firebird container data directory ({@code /var/lib/firebird/data})
     */
    String firebirdPath();

    /**
     * Equivalent relative path, normalized to use {@code /} as separator.
     * <p>
     * NOTE: Use of this path may not always behave as expected (especially if it also contains directory segments),
     * because in some cases Firebird will resolve relative paths in unexpected ways.
     * </p>
     *
     * @return relative path
     */
    String relativePath();

}
