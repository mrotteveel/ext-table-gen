// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Utility methods related to SQL syntax
 */
final class SqlSyntaxUtils {

    private SqlSyntaxUtils() {
        // no instances
    }

    /**
     * Enquotes an identifier.
     * <p>
     * If {@code identifier} starts and ends with a double quote, it is assumed to already be properly quoted.
     * </p>
     *
     * @param identifier
     *         identifier to quote
     * @return quoted identifier
     */
    static String enquoteIdentifier(String identifier) {
        if (identifier.matches("^\".+\"$")) {
            // assume double quotes are already properly escaped within
            return identifier;
        }
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    /**
     * Enquotes a string value as a valid SQL string literal.
     *
     * @param val
     *         value of the literal (any contained single quote is quoted)
     * @return properly quoted SQL literal
     */
    static String enquoteLiteral(String val) {
        return "'" + val.replace("'", "''") + "'";
    }

}
