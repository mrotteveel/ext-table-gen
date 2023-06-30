// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * Row processor to write the external table file.
 */
final class ExternalTableWriter extends AbstractRowProcessor implements Closeable {

    private static final int BUFFER_SIZE = 256 * 1024;
    private static final UnaryOperator<OutputStream> ADD_BUFFERED = out -> new BufferedOutputStream(out, BUFFER_SIZE);

    private final ExternalTable externalTable;
    private final OutputResource outputResource;
    private EncoderOutputStream out;

    /**
     * Creates a new external table writer, using the current output resource of the external table.
     *
     * @param externalTable
     *         external table definition
     */
    ExternalTableWriter(ExternalTable externalTable) {
        this(externalTable, requireNonNull(externalTable, "externalTable").outputResource());
    }

    /**
     * Creates a new external table writer.
     *
     * @param externalTable
     *         external table definition
     * @param outputResource
     *         output resource to write external table
     */
    ExternalTableWriter(ExternalTable externalTable, OutputResource outputResource) {
        this.externalTable = requireNonNull(externalTable, "externalTable")
                .withOutputResource(outputResource);
        this.outputResource = OutputResource.decorate(requireNonNull(outputResource, "outputResource"), ADD_BUFFERED);
    }

    @Override
    public ProcessingResult onHeader(Row header) {
        if (out != null) {
            throw new IllegalStateException("onHeader was invoked multiple times");
        }
        try {
            out = EncoderOutputStream.of(externalTable.byteOrder())
                    .with(outputResource.newOutputStream());
            return ProcessingResult.continueProcessing();
        } catch (IOException e) {
            var resultException = e instanceof FileAlreadyExistsException && !outputResource.allowOverwrite()
                    ? new TableFileAlreadyExistsException(
                    ("Could not create external table file '%s' as it already exists, specify --overwrite-table-file "
                     + "or configure overwrite=true on <tableFile> in XML")
                            .formatted(outputResource.path().map(String::valueOf).orElse("(no path specified)")), e)
                    : new InvalidTableException("Could not create external table file %s"
                            .formatted(outputResource.path().map(String::valueOf).orElse("(no path specified)")), e);
            return ProcessingResult.stopWith(resultException);
        }
    }

    @Override
    public ProcessingResult onRow(Row row) {
        if (out == null) {
            throw new IllegalStateException("onHeader must be called before calling onRow to initialise output stream");
        }
        try {
            externalTable.writeRow(row, out);
            return ProcessingResult.continueProcessing();
        } catch (IOException e) {
            try {
                close();
            } catch (IOException e2) {
                e.addSuppressed(e2);
            }
            return ProcessingResult.stopWith(e);
        }
    }

    @Override
    public ProcessingResult.Stop onComplete() {
        try {
            close();
            return ProcessingResult.stopProcessing();
        } catch (IOException e) {
            return ProcessingResult.stopWith(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            OutputStream out = this.out;
            if (out != null) out.close();
        } finally {
            // NOTE: Technically this means that an ExternalTableWriter is reusable given what onHeader does, however
            // we don't recommend doing so and this may change in the future
            out = null;
        }
    }

}
