// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Processor which signals stop when the exception received by {@link #onException(Exception)} has a specified type (or
 * one of its subclasses).
 */
final class StopOnExceptionProcessor extends AbstractRowProcessor {

    private final Class<? extends Exception> rootException;

    StopOnExceptionProcessor(Class<? extends Exception> rootException) {
        this.rootException = rootException;
    }

    @Override
    public ProcessingResult onRow(Row row) {
        return ProcessingResult.continueProcessing();
    }

    @Override
    ProcessingResult onExceptionHandler(Exception exception) {
        return rootException.isInstance(exception)
                ? ProcessingResult.stopProcessing()
                : ProcessingResult.continueProcessing();
    }
}
