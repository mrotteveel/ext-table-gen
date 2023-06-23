// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

/**
 * API for processing rows in string form.
 */
interface RowProcessor {

    /**
     * Receives the header of a data file.
     * <p>
     * If a file has no header, this method is called with a {@code Header} which has {@code line == -1} and
     * {@link Row#data()} ()} empty (i.e. {@link Row#noHeader()}). For simplicity, implementations are allowed to assume
     * that an empty header ({@link Row#isEmpty()}) signals no header, so there is no need to check {@code line}.
     * </p>
     *
     * @param header
     *         header of the file (generally, column names), or a <em>not a header</em> value
     * @return processing result
     */
    ProcessingResult onHeader(Row header);

    /**
     * Receives each row of the data file.
     * <p>
     * If exceptions occur during processing, they should be reported with a {@link ProcessingResult.StopWithException}.
     * Exceptions thrown out of this method should be taken to signal a {@code StopWithException}.
     * </p>
     *
     * @param row
     *         content of the row
     * @return processing result
     */
    ProcessingResult onRow(Row row);

    /**
     * Receives exceptions when reading the file.
     * <p>
     * NOTE: The return value is only used for {@code CsvException} and subclasses. Other exceptions are considered
     * fatal and will always terminate processing, even if {@code Continue} is returned.
     * </p>
     * <p>
     * If exceptions occur during processing, they should be reported with a {@link ProcessingResult.StopWithException}.
     * The original {@code exception} should <strong>not</strong> be reported this way. If {@code exception} is a cause
     * to stop, then a {@code Stop} should be returned. Exceptions thrown out of this method should be taken to signal
     * a {@code StopWithException}.
     * </p>
     *
     * @param exception
     *         exception reading the file
     * @return processing result
     */
    ProcessingResult onException(Exception exception);

    /**
     * Receives completion of reading file, or end of reading file when {@code onException} returned {@code Stop}
     * or a fatal exception occurred.
     * <p>
     * If exceptions occur during processing, they should be reported with a {@link ProcessingResult.StopWithException}.
     * Exceptions thrown out of this method should be taken to signal a {@code StopWithException}.
     * </p>
     */
    ProcessingResult.Stop onComplete();

}
