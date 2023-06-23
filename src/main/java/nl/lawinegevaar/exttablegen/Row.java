// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.List;
import java.util.stream.Stream;

/**
 * A row of data (also used for headers).
 *
 * @param line
 *         line number of the row (if the source is a CSV file, for multi-line rows, it is the last line of the row)
 * @param data
 *         row data
 */
record Row(long line, List<String> data) {

    Row {
        data = List.copyOf(data);
    }

    /**
     * @return {@code true} if {@link #data()} is empty
     */
    boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * @return size of {@link #data()}
     */
    int size() {
        return data.size();
    }

    /**
     * Gets a value from {@link #data()} by {@code index}.
     *
     * @param index
     *         0-based index
     * @return value at {@code index}
     * @throws IndexOutOfBoundsException
     *         if the {@code index} is out of range (less than {@code 0} or equal to or greater than {@link #size()})
     */
    String get(int index) {
        return data.get(index);
    }

    /**
     * @return stream over {@code data()}
     */
    Stream<String> stream() {
        return data.stream();
    }

    /**
     * Row value signalling that there is no header.
     * <p>
     * This can be used to inform {@link RowProcessor#onHeader(Row)} that there is no header.
     * </p>
     * <p>
     * The returned instance has {@code line = -1} and {@code data} empty.
     * </p>
     *
     * @return a <em>not a header</em> value; multiple invocations of this method may return the same instance
     */
    static Row noHeader() {
        return new Row(-1, List.of());
    }

}
