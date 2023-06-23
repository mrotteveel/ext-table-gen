// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * Row processor that multiplexes received events to other row processors.
 * <p>
 * Provides support for subscribed row processor to unsubscribe themselves using
 * the {@link ProcessingResult.Unsubscribe} result.
 * </p>
 * <p>
 * This class is not guaranteed to be thread-safe (especially with subscribing and unsubscribing), and an instance
 * should not be registered with multiple publishers.
 * </p>
 */
class MultiplexRowProcessor implements RowProcessor {

    private final Set<RowProcessor> subscribers = new CopyOnWriteArraySet<>();
    private int completionCount;

    /**
     * Creates a multiplex row processor with no initial subscribers.
     */
    MultiplexRowProcessor() {
    }

    /**
     * Creates a multiplex row processor with initial subscribers.
     *
     * @param subscribers
     *         initial subscribers
     */
    MultiplexRowProcessor(Collection<RowProcessor> subscribers) {
        subscribers.forEach(this::subscribe);
    }

    /**
     * Creates a multiplex row processor with initial subscribers.
     *
     * @param subscribers
     *         initial subscribers
     */
    MultiplexRowProcessor(RowProcessor... subscribers) {
        this(Arrays.asList(subscribers));
    }

    @Override
    public ProcessingResult onHeader(Row header) {
        return publish(rowProcessor -> rowProcessor.onHeader(header));
    }

    /**
     * {@inheritDoc}
     *
     * @return combined result of {@link RowProcessor#onRow(Row)} of all subscribers, or {@code Continue} if there
     * are no subscribers
     */
    @Override
    public final ProcessingResult onRow(Row row) {
        return publish(rowProcessor -> rowProcessor.onRow(row));
    }

    /**
     * {@inheritDoc}
     *
     * @return combined result of {@link RowProcessor#onException(Exception)} of all subscribers, or {@code Continue} if
     * there are no subscribers
     */
    @Override
    public final ProcessingResult onException(Exception exception) {
        return publish(rowProcessor -> rowProcessor.onException(exception));
    }

    /**
     * {@inheritDoc}
     *
     * @return combined result of {@link RowProcessor#onComplete} of all subscribers, or {@code Continue} if there are
     * no subscribers
     */
    @Override
    public final ProcessingResult.Stop onComplete() {
        completionCount++;
        ProcessingResult result = publish(RowProcessor::onComplete);
        if (!(result instanceof ProcessingResult.Stop stop)) {
            // Could happen when there are no subscribers
            return ProcessingResult.stopProcessing();
        }
        return stop;
    }

    /**
     * Completion count (the number of times {@link #onComplete()} has been invoked).
     *
     * @return completion count, {@code 0} if {@link #onComplete()} has not been called yet; given row processors should
     * generally not be reused, the value will normally be either {@code 0} or {@code 1}
     */
    int completionCount() {
        return completionCount;
    }

    /**
     * Subscribe a row processor to this multiplex row processor.
     *
     * @param rowProcessor
     *         row processor to add
     */
    final void subscribe(RowProcessor rowProcessor) {
        subscribers.add(rowProcessor);
    }

    /**
     * Unsubscribe (remove) a row processor from this multiplex row processor.
     *
     * @param rowProcessor
     *         row processor to remove
     */
    final void unsubscribe(RowProcessor rowProcessor) {
        subscribers.remove(rowProcessor);
    }

    /**
     * Publishes to all subscribers.
     * <p>
     * If a subscriber returns the {@code Unsubscribe} signal, it will be unsubscribed.
     * </p>
     *
     * @param publishAction
     *         publish action to run for each subscribed row processor
     * @return the combined processing result
     */
    private ProcessingResult publish(Function<RowProcessor, ProcessingResult> publishAction) {
        ProcessingResult result = ProcessingResult.continueProcessing();
        for (RowProcessor rowProcessor : subscribers) {
            try {
                ProcessingResult processorResult = publishAction.apply(rowProcessor);
                if (processorResult instanceof ProcessingResult.Unsubscribe) {
                    unsubscribe(rowProcessor);
                }
                result = result.combine(processorResult);
            } catch (RuntimeException e) {
                result = result.combine(ProcessingResult.stopWith(e));
            }
        }
        return result;
    }

}
