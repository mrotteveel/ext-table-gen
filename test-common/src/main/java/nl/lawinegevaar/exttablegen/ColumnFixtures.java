// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.type.*;

import java.math.RoundingMode;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nl.lawinegevaar.exttablegen.type.FbEncoding.ISO8859_1;

final class ColumnFixtures {

    private static final List<String> CUSTOMERS_COLUMN_NAMES = List.of("Index", "Customer Id", "First Name",
            "Last Name", "Company", "City", "Country", "Phone 1", "Phone 2", "Email", "Subscription Date", "Website");
    private static final List<Integer> CUSTOMERS_10_SIZES = List.of(2, 15, 8, 9, 31, 17, 26, 22, 21, 27, 10, 27);

    private ColumnFixtures() {
        // no instances
    }

    /**
     * Creates a column with {@code name} and data type {@code FbChar} with {@code length} and encoding
     * {@code ISO8859_1}.
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
     * Creates a column with {@code name} and data type {@code FbChar} with {@code length} and {@code encoding}.
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
        return new Column(name, new FbChar(length, encoding));
    }

    static Column integralNumber(String name, String typeName) {
        return integralNumber(name, typeName, null);
    }

    static Column integralNumber(String name, String typeName, Converter<?> converter) {
        return switch (typeName) {
            case "smallint" -> smallint(name, converter);
            case "integer" -> integer(name, converter);
            case "bigint" -> bigint(name, converter);
            case "int128" -> int128(name, converter);
            default -> throw new IllegalArgumentException("Unsupported typeName: " + typeName);
        };
    }

    /**
     * Creates a column with {@code name}, data type {@code FbSmallint} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column smallint(String name, Converter<?> converter) {
        return new Column(name, new FbSmallint().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbInteger} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column integer(String name, Converter<?> converter) {
        return new Column(name, new FbInteger().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbBigint} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column bigint(String name, Converter<?> converter) {
        return new Column(name, new FbBigint().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbInt128} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column int128(String name, Converter<?> converter) {
        return new Column(name, new FbInt128().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbDate} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column date(String name, Converter<?> converter) {
        return new Column(name, new FbDate().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbTime} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column time(String name, Converter<?> converter) {
        return new Column(name, new FbTime().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbTimestamp} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column timestamp(String name, Converter<?> converter) {
        return new Column(name, new FbTimestamp().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbNumeric} with {@code precision} and {@code scale}.
     *
     * @param name
     *         name of the column
     * @param precision
     *         precision
     * @param scale
     *         scale
     * @return column
     */
    static Column numeric(String name, int precision, int scale) {
        return numeric(name, precision, scale, null, null);
    }

    /**
     * Creates a column with {@code name}, data type {@code FbNumeric} with {@code precision}, {@code scale} and
     * {@code roundingMode}, and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param precision
     *         precision
     * @param scale
     *         scale
     * @param roundingMode
     *         rounding mode ({@code null} will apply {@link RoundingMode#HALF_UP})
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column numeric(String name, int precision, int scale, RoundingMode roundingMode, Converter<?> converter) {
        return new Column(name, new FbNumeric(precision, scale, roundingMode).withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbDecimal} with {@code precision} and {@code scale}.
     *
     * @param name
     *         name of the column
     * @param precision
     *         precision
     * @param scale
     *         scale
     * @return column
     */
    static Column decimal(String name, int precision, int scale) {
        return decimal(name, precision, scale, null, null);
    }

    /**
     * Creates a column with {@code name}, data type {@code FbDecimal} with {@code precision}, {@code scale} and
     * {@code roundingMode}, and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param precision
     *         precision
     * @param scale
     *         scale
     * @param roundingMode
     *         rounding mode ({@code null} will apply {@link RoundingMode#HALF_UP})
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column decimal(String name, int precision, int scale, RoundingMode roundingMode, Converter<?> converter) {
        return new Column(name, new FbDecimal(precision, scale, roundingMode).withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbFloat} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column floatCol(String name, Converter<?> converter) {
        return new Column(name, new FbFloat().withConverterChecked(converter));
    }

    /**
     * Creates a column with {@code name}, data type {@code FbDoublePrecision} and {@code converter}.
     *
     * @param name
     *         name of the column
     * @param converter
     *         converter, or {@code null} for default
     * @return column
     */
    static Column doublePrecision(String name, Converter<?> converter) {
        return new Column(name, new FbDoublePrecision().withConverterChecked(converter));
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
