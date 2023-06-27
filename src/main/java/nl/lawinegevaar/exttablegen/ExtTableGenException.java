// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Root for exceptions specific to ExtTableGen
 */
class ExtTableGenException extends RuntimeException {

    ExtTableGenException(String message) {
        super(message);
    }

    ExtTableGenException(String message, Throwable cause) {
        super(message, cause);
    }

}

/**
 * Signals an exception which is considered fatal when reading or writing files.
 */
final class FatalRowProcessingException extends ExtTableGenException {

    FatalRowProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}

final class InvalidConfigurationException extends ExtTableGenException {

    InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}

/**
 * Thrown if an external table definition cannot be read, or for problems writing the external table data.
 */
sealed class InvalidTableException extends ExtTableGenException permits TableFileAlreadyExistsException {

    InvalidTableException(String message, Throwable cause) {
        super(message, cause);
    }

}

final class TableFileAlreadyExistsException extends InvalidTableException {

    TableFileAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

/**
 * Thrown to signal that there is no input resource when one is required.
 */
final class MissingInputResourceException extends ExtTableGenException {

    MissingInputResourceException(String message) {
        super(message);
    }

}

/**
 * Thrown if it was not possible to find column names, or where column names are required, but none are available
 */
final class NoColumnNamesException extends ExtTableGenException {

    NoColumnNamesException(String message) {
        super(message);
    }

    NoColumnNamesException(String message, Throwable cause) {
        super(message, cause);
    }

}