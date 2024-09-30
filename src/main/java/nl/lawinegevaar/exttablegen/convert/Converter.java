// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import nl.lawinegevaar.exttablegen.TargetTypeMismatchException;

import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.function.Function;

/**
 * A converter performs a conversion from a String value to a value of type {@code T}.
 *
 * @param <T>
 *         target type of the converter
 * @since 2
 */
public interface Converter<T> {

    /**
     * Converts {@code sourceValue} to a value of type {@code T}.
     * <p>
     * Failure to convert the value should result in a runtime exception, preferably an
     * {@link IllegalArgumentException} or subclass.
     * </p>
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value of type {@code T}
     */
    T convert(String sourceValue);

    /**
     * @return target type, or the Java type returned by {@link #convert(String)}.
     */
    Class<T> targetType();

    /**
     * Name of this converter, matching the name used in the XML configuration.
     * <p>
     * Names which are not defined in the XML configuration (e.g. for internal converters), should be prefixed
     * with {@code "##custom##"}.
     * </p>
     *
     * @return name of the converter
     */
    String converterName();

    /**
     * Performs a checked cast of this converter to one of {@code requiredType}.
     * <p>
     * This should be used when all you have is a {@code Converter<?>} and need the specific type information.
     * </p>
     *
     * @param requiredType
     *         required type
     * @param <X>
     *         target type of the cast
     * @return explicitly cast converter
     * @throws TargetTypeMismatchException
     *         if {@link #targetType()} is not {@code requiredType}.
     */
    @SuppressWarnings("unchecked")
    default <X> Converter<X> checkedCast(Class<X> requiredType) {
        if (targetType() == requiredType) {
            return (Converter<X>) this;
        }
        throw new TargetTypeMismatchException("Converter has target type %s, required type was %s"
                .formatted(targetType().getName(), requiredType.getName()));
    }

    /**
     * If this is wrapped converter (e.g. {@link IntConverter#wrap(Converter)}), return the unwrapped form.
     * <p>
     * Default implementation: {@code return this}.
     * </p>
     * <p>
     * NOTE: a converter is wrapped if {@link #convert(String)} for both the wrapping and wrapped converter produce
     * the same values. It may provide additional methods. When return values of {@code convert} are modified, it is
     * a decorated/decorating converter.
     * </p>
     *
     * @return the wrapped converter, if this is a wrapper, otherwise this instance
     */
    default Converter<T> unwrap() {
        return this;
    }

    /**
     * Indicates whether some object is equal to this one.
     * <p>
     * Implementations should compare against the unwrapped variant (see {@link #unwrap()}).
     * </p>
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a hash code value for this object.
     * <p>
     * Wrapping implementations should use the hash code of the wrapped instance (see {@link #unwrap()}).
     * </p>
     */
    @Override
    int hashCode();

    /**
     * Wraps {@code function} in a converter with target type {@code X}.
     * <p>
     * The converter will report converter name {@code ##custom##} followed by the {@code toString()} of
     * {@code function}.
     * </p>
     *
     * @param targetType
     *         class of the target type of the converter
     * @param function
     *         function from {@code String} to {@code X}
     * @param <X>
     *         target type of the converter
     * @return custom converter wrapping {@code function}
     */
    static <X> Converter<X> of(Class<X> targetType, Function<String, X> function) {
        return new Converter<>() {

            @Override
            public X convert(String sourceValue) {
                return function.apply(sourceValue);
            }

            @Override
            public Class<X> targetType() {
                return targetType;
            }

            @Override
            public String converterName() {
                return "##custom##" + function;
            }

        };
    }

    /**
     * Constructs a converter to parse integral numbers with a radix, based on the type name in the XML config.
     *
     * @param xmlTypeName
     *         type name, as used in the XML
     * @param radix
     *         radix
     * @return instance for {@code xmlTypeName} parsing with {@code radix} (possibly cached instance)
     * @throws IllegalArgumentException
     *         if {@code xmlTypeName} is not supported or recognized
     */
    static Converter<? extends Number> parseIntegralNumber(String xmlTypeName, int radix) {
        return switch (xmlTypeName) {
            case "bigint" -> ParseBigint.ofRadix(radix);
            case "int128" -> ParseInt128.ofRadix(radix);
            case "integer" -> ParseInteger.ofRadix(radix);
            case "smallint" -> ParseSmallint.ofRadix(radix);
            default -> throw new IllegalArgumentException("Unsupported type name: " + xmlTypeName);
        };
    }

    /**
     * Constructs a converter to parse datetime values to a {@link TemporalAccessor}.
     *
     * @param pattern
     *         pattern for {@link java.time.format.DateTimeFormatter}, or the name of one of the pre-defined patterns
     * @param locale
     *         locale in a BCP 47 language tag, or {@code null} to use the default locale
     * @return converter to parse datetime values
     */
    static ParseDatetime parseDatetime(String pattern, String locale) {
        return parseDatetime(pattern, toLocale(locale));
    }

    /**
     * Constructs a converter to parse datetime values to a {@link TemporalAccessor}.
     *
     * @param pattern
     *         pattern for {@link java.time.format.DateTimeFormatter}, or the name of one of the pre-defined patterns
     * @param locale
     *         locale, or {@code null} to use the default locale
     * @return converter to parse datetime values
     */
    static ParseDatetime parseDatetime(String pattern, Locale locale) {
        return new ParseDatetime(pattern, locale);
    }

    /**
     * Constructs a converter to parse to {@link java.math.BigDecimal} values.
     *
     * @param locale
     *         locale in a BCP 47 language tag (cannot be {@code null})
     * @return converter to parse big decimal values
     */
    static ParseBigDecimal parseBigDecimal(String locale) {
        return parseBigDecimal(toLocale(locale));
    }

    /**
     * Constructs a converter to parse to {@link java.math.BigDecimal} values.
     *
     * @param locale
     *         locale (cannot be {@code null})
     * @return converter to parse big decimal values
     */
    static ParseBigDecimal parseBigDecimal(Locale locale) {
        return new ParseBigDecimal(locale);
    }

    /**
     * Constructs a converter to parse to floating point number values, based on the type name in the XML config.
     *
     * @param xmlTypeName
     *         type name, as used in the XML
     * @param locale
     *         locale in a BCP 47 language tag (cannot be {@code null})
     * @return converter to parse big decimal values
     * @throws IllegalArgumentException
     *         if {@code xmlTypeName} is not supported or recognized
     * @since 3
     */
    static Converter<? extends Number> parseFloatingPointNumber(String xmlTypeName, String locale) {
        return parseFloatingPointNumber(xmlTypeName, toLocale(locale));
    }

    /**
     * Constructs a converter to parse to floating point number values, based on the type name in the XML config.
     *
     * @param xmlTypeName
     *         type name, as used in the XML
     * @param locale
     *         locale (cannot be {@code null})
     * @return converter to parse big decimal values
     * @throws IllegalArgumentException
     *         if {@code xmlTypeName} is not supported or recognized
     * @since 3
     */
    static Converter<? extends Number> parseFloatingPointNumber(String xmlTypeName, Locale locale) {
        return switch (xmlTypeName) {
            case "float" -> new ParseFloat(locale);
            case "doublePrecision" -> new ParseDoublePrecision(locale);
            default -> throw new IllegalArgumentException("Unsupported type name: " + xmlTypeName);
        };
    }

    /**
     * Converts a BCP 47 language tag to a {@link Locale}.
     *
     * @param languageTag
     *         BCP 47 language tag, or {@code null}
     * @return local instance, or {@code null} if {@code languageTag} is {@code null}
     * @since 3
     */
    private static Locale toLocale(String languageTag) {
        return languageTag != null ? Locale.forLanguageTag(languageTag) : null;
    }

}
