// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * Result of handling an individual {@link RowProcessor} event or processing in its entirety.
 * <p>
 * Row processors or processing drivers emit these events to inform their publisher or other callers of the result of
 * processing an event, or the processing in its entirety, and how to proceed. This includes things like Stop processing
 * (due to exception or other conditions), Continue processing, Unsubscribe as a processor (though publishers may ignore
 * that), or Done (successful completion of processing).
 * </p>
 */
sealed interface ProcessingResult
        permits ProcessingResult.Continue, ProcessingResult.Stop, ProcessingResult.Unsubscribe {

    /**
     * Combine processing results in a way that reflects the correct priority.
     *
     * @param other
     *         other processing result
     * @return combined result (which may be a new instance, <em>this</em> instance or {@code other}).
     */
    ProcessingResult combine(ProcessingResult other);

    /**
     * Signal to continue processing.
     *
     * @return an instance of {@code Continue} (may return the same instance on each invocation)
     */
    static Continue continueProcessing() {
        return Continue.INSTANCE;
    }

    /**
     * Signal to stop processing.
     *
     * @return an instance of {@code Stop} (may return the same instance on each invocation)
     */
    static Stop stopProcessing() {
        return Stop.INSTANCE;
    }

    /**
     * Signal to stop processing due to an exception.
     *
     * @param exception exception
     * @return an instance of {@code StopWithException}
     */
    static StopWithException stopWith(Exception exception) {
        return new StopWithException(exception);
    }

    /**
     * Signal to unsubscribe.
     *
     * @return an instance of {@code Unsubscribe} (may return the same instance on each invocation)
     */
    static Unsubscribe unsubscribe() {
        return Unsubscribe.INSTANCE;
    }

    /**
     * Signals completion of processing.
     * <p>
     * This signal should only be used by the outer publisher (the driver of events), like {@link CsvFile}. Row
     * processors and intermediate publishers should use {@link #stopProcessing()}.
     * </p>
     *
     * @return an instance of {@code Done} (may return the same instance on each invocation)
     */
    static Done done() {
        return Done.INSTANCE;
    }

    /**
     * Class to signal continuation.
     * <p>
     * In cases where multiple processors are processing the same event, this result has lower priority than other
     * results, except for {@link Unsubscribe}.
     * </p>
     */
    final class Continue implements ProcessingResult {

        private static final Continue INSTANCE = new Continue();

        private Continue() {
        }

        @Override
        public ProcessingResult combine(ProcessingResult other) {
            if (other instanceof Unsubscribe) {
                return this;
            }
            return other;
        }

    }

    /**
     * Class to signal continuation, but that the processor wants to unsubscribe because it is no longer interested.
     * <p>
     * Publishers can choose to ignore unsubscribe requests, remove the subscription, or &mdash; if this is a
     * delegating (or republishing) processor, and this was their last or only subscriber &mdash; forward
     * the unsubscribe to their publisher.
     * </p>
     * <p>
     * An {@code Unsubscribe} has the lowest priority.
     * </p>
     */
    final class Unsubscribe implements ProcessingResult {

        private static final Unsubscribe INSTANCE = new Unsubscribe();

        private Unsubscribe() {
        }

        @Override
        public ProcessingResult combine(ProcessingResult other) {
            return other;
        }

    }

    /**
     * Signals to stop the processing.
     * <p>
     * A {@code Stop} has the third highest priority, after {@code StopWithException} and {@code Done}.
     * </p>
     */
    sealed class Stop implements ProcessingResult permits StopWithException, Done {

        private static final Stop INSTANCE = new Stop();

        private Stop() {
        }

        @Override
        public ProcessingResult combine(ProcessingResult other) {
            if (other instanceof Stop) {
                return other;
            }
            return this;
        }

    }

    /**
     * Special case of Stop which signals a successful completion of processing.
     * <p>
     * In general, this should only be returned by outer publisher (the driver of events), like {@link CsvFile}.
     * </p>
     * <p>
     * A {@code Done} has the highest priority.
     * </p>
     */
    final class Done extends Stop {

        private static final Done INSTANCE = new Done();

        private Done() {
        }

        /**
         * Combines this Done signal with another processing result.
         * <p>
         * The other processing result is effectively ignored, and combining with {@code Done} results in this instance.
         * </p>
         *
         * @param other
         *         other processing result
         * @return always this instance
         */
        @Override
        public ProcessingResult combine(ProcessingResult other) {
            return this;
        }

    }

    /**
     * Signals to stop the processing, with an exception.
     * <p>
     * If multiple processors reported an exception, they are chained as suppressed exceptions.
     * </p>
     * <p>
     * A {@code StopWithException} has the second highest priority, after {@code Done}.
     * </p>
     */
    final class StopWithException extends Stop {

        private final Exception exception;

        private StopWithException(Exception exception) {
            this.exception = exception;
        }

        /**
         * @return processing result exception
         */
        Exception exception() {
            return exception;
        }

        @Override
        public ProcessingResult combine(ProcessingResult other) {
            if (other instanceof StopWithException swe) {
                exception.addSuppressed(swe.exception());
                return this;
            }
            return super.combine(other);
        }

    }

}