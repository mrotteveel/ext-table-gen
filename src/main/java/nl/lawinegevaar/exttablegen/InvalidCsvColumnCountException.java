// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;

/**
 * Thrown when the column count in the CSV input file does not match the expected number of columns.
 */
final class InvalidCsvColumnCountException extends CsvValidationException {

    InvalidCsvColumnCountException(String message) {
        super(message);
    }

}
