// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.firebirdsql.decimal.Decimal;
import org.firebirdsql.decimal.Decimal128;
import org.firebirdsql.decimal.Decimal64;
import org.firebirdsql.decimal.OverflowHandling;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/**
 * Abstract decimal floating point datatype.
 *
 * @param <T>
 *         decimal datatype
 * @since 3
 */
public abstract sealed class AbstractFbDecimalFloatingPointDatatype<T extends Decimal<T>>
        extends AbstractFbDatatype<T, Converter<T>> implements FbDecimalFloatingPointDatatype<T>
        permits FbDecfloat16, FbDecfloat34 {

    private final BackingType<T> backingType;

    AbstractFbDecimalFloatingPointDatatype(BackingType<T> backingType,
            @Nullable Converter<BigDecimal> bigDecimalConverter) {
        this(backingType.adaptConverter(bigDecimalConverter), backingType);
    }

    // NOTE: The parameter order is different because of the different converter type parameter
    AbstractFbDecimalFloatingPointDatatype(@Nullable Converter<T> converter, BackingType<T> backingType) {
        super(backingType.targetType(), converter, backingType.defaultConverter());
        this.backingType = backingType;
    }

    @Override
    public final int precision() {
        return backingType.precision();
    }

    @Override
    public final DecfloatOnOverflow onOverflow() {
        return backingType.onOverflow();
    }

    BackingType<T> backingType() {
        return backingType;
    }

    @Override
    public final void writeEmpty(EncoderOutputStream out) throws IOException {
        backingType.writeEmpty(out);
    }

    @Override
    protected void writeValueImpl(T value, EncoderOutputStream out) throws IOException {
        backingType.writeValue(value, out);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("decfloat(").append(precision()).append(')');
    }

    @Override
    public FbDatatype<T> withConverterChecked(@Nullable Converter<?> converter) {
        if (converter != null && converter.targetType() == BigDecimal.class) {
            return withConverter(backingType.adaptConverter(converter.checkedCast(BigDecimal.class)));
        }
        return super.withConverterChecked(converter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), backingType, converter());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this || obj instanceof AbstractFbDecimalFloatingPointDatatype<?> that
                && this.getClass() == that.getClass()
                && this.backingType.equals(that.backingType)
                && this.converter().equals(that.converter());
    }

    static abstract sealed class BackingType<T extends Decimal<T>> {

        private final Class<T> targetType;
        private final int precision;
        private final DecfloatOnOverflow onOverflow;

        BackingType(Class<T> targetType, int precision, @Nullable DecfloatOnOverflow onOverflow) {
            this.targetType = requireNonNull(targetType, "targetType");
            this.precision = precision;
            this.onOverflow = requireNonNullElse(onOverflow, DecfloatOnOverflow.ROUND_TO_INFINITY);
        }

        final Class<T> targetType() {
            return targetType;
        }

        final int precision() {
            return precision;
        }

        final DecfloatOnOverflow onOverflow() {
            return onOverflow;
        }

        final OverflowHandling overflowHandling() {
            return onOverflow.overflowHandling();
        }

        /**
         * Converts a {@link BigDecimal} to {@link Decimal} ({@code T}) applying {@code onOverflow}.
         *
         * @param value
         *         value to convert
         * @return equivalent value in {@code T}
         * @throws ArithmeticException
         *         on overflow
         */
        abstract T valueOf(BigDecimal value);

        /**
         * @return default converter for {@code T}
         */
        abstract Converter<T> defaultConverter();

        /**
         * Adapts a {@link BigDecimal} converter instance to produce values of type {@code T}.
         * <p>
         * The returned converter reports the name of the wrapped (adapted) converter.
         * </p>
         *
         * @param bigDecimalConverter
         *         {@code BigDecimal} converter, or {@code null}
         * @return adapted converter, or {@code null}
         */
        @Nullable
        final Converter<T> adaptConverter(@Nullable Converter<BigDecimal> bigDecimalConverter) {
            if (bigDecimalConverter == null) return null;
            return new BigDecimalToDecimalConverter<>(this, bigDecimalConverter);
        }

        void writeValue(T value, EncoderOutputStream out) throws IOException {
            out.align(8);
            out.writeFromNetworkOrder(value.toBytes());
        }

        abstract void writeEmpty(EncoderOutputStream out) throws IOException;

        @Override
        public int hashCode() {
            // In current implementation precision implies target type (and vice versa), so no need to consider both
            return Objects.hash(precision, onOverflow);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            // In current implementation precision implies target type (and vice versa), so no need to consider both
            return this == obj || obj instanceof BackingType<?> that
                    && this.precision == that.precision
                    && this.onOverflow == that.onOverflow;
        }
    }

    /**
     * {@code Decimal64} (or {@code DECFLOAT(16)}) backing type and overflow handling.
     */
    static final class Decimal64Type extends BackingType<Decimal64> {

        private static final Decimal64 ZERO = Decimal64.valueOf(BigDecimal.ZERO);

        Decimal64Type(@Nullable DecfloatOnOverflow onOverflow) {
            super(Decimal64.class, 16, onOverflow);
        }

        @Override
        Converter<Decimal64> defaultConverter() {
            return Converter.of(Decimal64.class, v -> Decimal64.valueOf(v, overflowHandling()));
        }

        @Override
        Decimal64 valueOf(BigDecimal value) {
            return Decimal64.valueOf(value, overflowHandling());
        }

        void writeEmpty(EncoderOutputStream out) throws IOException {
            writeValue(ZERO, out);
        }

    }

    /**
     * {@code Decimal128} (or {@code DECFLOAT(34)}) backing type and overflow handling.
     */
    static final class Decimal128Type extends BackingType<Decimal128> {

        private static final Decimal128 ZERO = Decimal128.valueOf(BigDecimal.ZERO);

        Decimal128Type(@Nullable DecfloatOnOverflow onOverflow) {
            super(Decimal128.class, 34, onOverflow);
        }

        @Override
        Converter<Decimal128> defaultConverter() {
            return Converter.of(Decimal128.class, v -> Decimal128.valueOf(v, overflowHandling()));
        }

        @Override
        Decimal128 valueOf(BigDecimal value) {
            return Decimal128.valueOf(value, overflowHandling());
        }

        void writeEmpty(EncoderOutputStream out) throws IOException {
            writeValue(ZERO, out);
        }

    }

    private record BigDecimalToDecimalConverter<T extends Decimal<T>>(
            BackingType<T> backingType, Converter<BigDecimal> bigDecimalConverter) implements Converter<T> {

        private BigDecimalToDecimalConverter(BackingType<T> backingType, Converter<BigDecimal> bigDecimalConverter) {
            this.backingType = backingType;
            this.bigDecimalConverter = requireNonNull(bigDecimalConverter, "bigDecimalConverter");
        }

        @Override
        public T convert(String sourceValue) {
            return backingType.valueOf(bigDecimalConverter.convert(sourceValue));
        }

        @Override
        public Class<T> targetType() {
            return backingType.targetType();
        }

        @Override
        public String converterName() {
            return bigDecimalConverter.converterName();
        }

        @Override
        public <X extends Converter<?>> Optional<X> unwrap(Class<X> type) {
            if (type.isInstance(bigDecimalConverter)) {
                return Optional.of(type.cast(bigDecimalConverter));
            }
            return Converter.super.unwrap(type);
        }

    }

}
