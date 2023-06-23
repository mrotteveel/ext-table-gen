// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nl.lawinegevaar.exttablegen.FbEncoding.ISO8859_1;

final class ColumnFixtures {

    private static final List<String> CUSTOMERS_COLUMN_NAMES = List.of("Index", "Customer Id", "First Name",
            "Last Name", "Company", "City", "Country", "Phone 1", "Phone 2", "Email", "Subscription Date", "Website");
    private static final List<Integer> CUSTOMERS_10_SIZES = List.of(2, 15, 8, 9, 31, 17, 26, 22, 21, 27, 10, 27);

    private ColumnFixtures() {
        // no instances
    }

    /**
     * Creates a column with {@code name} and data type {@code Char} with {@code length} and encoding {@code ISO8859_1}.
     *
     * @param name
     *         name of the column
     * @param length
     *         length in Unicode code points
     * @return column
     */
    static Column col(String name, int length) {
        return col(name, length, ISO8859_1);
    }

    /**
     * Creates a column with {@code name} and data type {@code Char} with {@code length} and {@code encoding}.
     *
     * @param name
     *         name of the column
     * @param length
     *         length in Unicode code points
     * @param encoding
     *         encoding of the column
     * @return column
     */
    static Column col(String name, int length, FbEncoding encoding) {
        return new Column(name, new Char(length, encoding));
    }

    /**
     * Columns of the {@code customers-10.csv} file with the specified end column type and encoding.
     * <p>
     * The returned columns use encoding {@code ISO8859_1}.
     * </p>
     *
     * @param endColumnType
     *         end column type
     * @param encoding
     *         encoding of the columns
     * @return list of columns
     */
    static List<Column> customers10Columns(EndColumn.Type endColumnType, FbEncoding encoding) {
        BiFunction<String, Integer, Column> columnFactory = (name, length) -> col(name, length, encoding);
        return Stream.concat(
                        zipped(CUSTOMERS_COLUMN_NAMES, CUSTOMERS_10_SIZES, columnFactory),
                        endColumnType.getEndColumn().stream())
                .toList();
    }

    // Based on https://stackoverflow.com/a/42787326/466862 by Rafael (https://stackoverflow.com/users/7707798/rafael)
    private static <A, B, C> Stream<C> zipped(List<A> listA, List<B> listB, BiFunction<A, B, C> zipper) {
        int shortestLength = Math.min(listA.size(), listB.size());
        return IntStream.range(0, shortestLength)
                .mapToObj(i -> zipper.apply(listA.get(i), listB.get(i)));
    }

}
