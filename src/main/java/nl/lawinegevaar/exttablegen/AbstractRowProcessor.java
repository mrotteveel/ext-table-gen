// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Base implementation of {@link RowProcessor}.
 */
abstract class AbstractRowProcessor implements RowProcessor {

    private @Nullable Exception lastException;

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in this class simply returns {@code Continue}. Subclasses can override this method if they
     * need to process the header.
     * </p>
     */
    @Override
    public ProcessingResult onHeader(Row header) {
        return ProcessingResult.continueProcessing();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation stores the last exception for retrieval with {@link #getLastException()}, and always
     * returns {@code false} (continue with processing).
     * </p>
     * <p>
     * Subclasses can override {@link #onExceptionHandler(Exception)} for further actions on the exception and/or to
     * determine the return value of this method.
     * </p>
     */
    @Override
    public final ProcessingResult onException(Exception exception) {
        lastException = requireNonNull(exception, "exception");
        return onExceptionHandler(exception);
    }

    /**
     * Handler for the exception received by {@link #onException(Exception)}. This method can be overridden by
     * subclasses for further actions on the exception and/or to determine the return value of
     * {@link #onException(Exception)}.
     * <p>
     * The implementation in this class returns {@link ProcessingResult.Continue} to continue processing.
     * </p>
     *
     * @param exception
     *         received exception
     * @return processing result to be returned by {@link #onException(Exception)}.
     */
    ProcessingResult onExceptionHandler(Exception exception) {
        return ProcessingResult.continueProcessing();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in this class simply returns {@code Stop}. Subclasses can override this method if they
     * need to process the complete signal.
     * </p>
     */
    @Override
    public ProcessingResult.Stop onComplete() {
        return ProcessingResult.stopProcessing();
    }

    /**
     * Last exception received.
     *
     * @return the last exception received, or empty if no exception was received
     */
    final Optional<Exception> getLastException() {
        return Optional.ofNullable(lastException);
    }

}
