// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;

/**
 * Thrown when the size of one or more columns in the CSV input file does not match the expected size.
 * <p>
 * This is usually thrown if a column is too long, but can also be thrown in cases where a column must have a specific
 * length, and the value is too short.
 * </p>
 */
class InvalidCsvColumnSizeException extends CsvValidationException {

    InvalidCsvColumnSizeException(String message) {
        super(message);
    }

}
