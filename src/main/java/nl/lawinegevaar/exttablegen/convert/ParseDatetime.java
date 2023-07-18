// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import java.lang.reflect.Field;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Converter from string to a temporal accessor, to be used with datetime types.
 * <p>
 * Be aware that depending on the {@code pattern} specified, the resulting value is not suitable for all datetime types;
 * it may result in an error when writing the value through a data type.
 * </p>
 *
 * @since 2
 */
public final class ParseDatetime implements Converter<TemporalAccessor> {

    private static final ParseDatetime DEFAULT_DATE_INSTANCE = new ParseDatetime(DateTimeFormatter.ISO_LOCAL_DATE);
    private static final ParseDatetime DEFAULT_TIME_INSTANCE = new ParseDatetime(DateTimeFormatter.ISO_LOCAL_TIME);
    private static final ParseDatetime DEFAULT_TIMESTAMP_INSTANCE =
            new ParseDatetime(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private final String pattern;
    private final Locale locale;
    private final DateTimeFormatter formatter;

    /**
     * Constructs a {@code ParseDatetime} instance with the specified pattern and locale.
     *
     * @param pattern
     *         date time formatter pattern, or the name of one of the standard formatters, cannot be {@code null}
     * @param locale
     *         locale, uses default locale when {@code null}
     * @throws IllegalArgumentException
     *         if {@code pattern} is invalid
     */
    ParseDatetime(String pattern, Locale locale) {
        this.pattern = requireNonNull(pattern, "pattern");
        this.locale = locale;
        formatter = createDateTimeFormatter(pattern, locale);
    }

    private ParseDatetime(DateTimeFormatter formatter) {
        pattern = null;
        locale = null;
        this.formatter = requireNonNull(formatter, "formatter");
    }

    /**
     * Default instance for parsing dates (uses {@link DateTimeFormatter#ISO_LOCAL_DATE}).
     * <p>
     * The returned instance does not have a locale or pattern set.
     * </p>
     *
     * @return instance for parsing dates
     */
    public static ParseDatetime getDefaultDateInstance() {
        return DEFAULT_DATE_INSTANCE;
    }

    /**
     * Default instance for parsing dates (uses {@link DateTimeFormatter#ISO_LOCAL_TIME}).
     * <p>
     * The returned instance does not have a locale or pattern set.
     * </p>
     *
     * @return instance for parsing times
     */
    public static ParseDatetime getDefaultTimeInstance() {
        return DEFAULT_TIME_INSTANCE;
    }

    /**
     * Default instance for parsing timestamps (uses {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}).
     * <p>
     * The returned instance does not have a locale or pattern set.
     * </p>
     *
     * @return instance for parsing timestamps
     */
    public static ParseDatetime getDefaultTimestampInstance() {
        return DEFAULT_TIMESTAMP_INSTANCE;
    }

    /**
     * @return pattern of this {@code ParseDatetime}, can be {@code null} only for the default instances.
     */
    public String pattern() {
        return pattern;
    }

    /**
     * @return locale of this {@code ParseDatetime}, or empty if no explicit locale was set
     */
    public Optional<Locale> locale() {
        return Optional.ofNullable(locale);
    }

    @Override
    public TemporalAccessor convert(String sourceValue) {
        return formatter.parse(sourceValue);
    }

    @Override
    public Class<TemporalAccessor> targetType() {
        return TemporalAccessor.class;
    }

    @Override
    public String converterName() {
        return "parseDatetime";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof ParseDatetime that
               && Objects.equals(this.pattern, that.pattern)
               && Objects.equals(this.locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, locale);
    }

    @Override
    public String toString() {
        return "parseDatetime{" +
               "pattern='" + pattern + '\'' +
               ", locale=" + (locale != null ? locale.toLanguageTag() : null) +
               '}';
    }

    /**
     * Creates a date time formatter.
     *
     * @param pattern
     *         date time formatter pattern, or the name of one of the standard formatters
     * @param locale
     *         locale, or {@code locale} to use the default
     * @return date time formatter instance
     * @throws IllegalArgumentException
     *         if {@code pattern} is invalid
     */
    private static DateTimeFormatter createDateTimeFormatter(String pattern, Locale locale) {
        DateTimeFormatter formatter = FormatterHolder.getByName(pattern)
                .orElseGet(() -> DateTimeFormatter.ofPattern(pattern));
        if (locale != null) {
            return formatter.localizedBy(locale);
        }
        return formatter;
    }

    /**
     * Holds the map of standard formatters defined in DateTimeFormatter
     */
    private static final class FormatterHolder {

        private static final Map<String, DateTimeFormatter> STANDARD_FORMATTERS;
        static {
            Map<String, DateTimeFormatter> formatters = new HashMap<>();
            // Custom formatters
            // SQL_TIMESTAMP, defined by ADR 2023-09
            formatters.put("SQL_TIMESTAMP", new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT)
                    .withChronology(IsoChronology.INSTANCE));

            // Standard formatters defined in DateTimeFormatter
            Field[] fields = DateTimeFormatter.class.getFields();
            for (Field field : fields) {
                if (field.getType() != DateTimeFormatter.class) continue;
                try {
                    formatters.put(field.getName(), (DateTimeFormatter) field.get(null));
                } catch (IllegalAccessException ignored) {
                }
            }
            STANDARD_FORMATTERS = Map.copyOf(formatters);
        }

        static Optional<DateTimeFormatter> getByName(String name) {
            return Optional.ofNullable(STANDARD_FORMATTERS.get(name));
        }

    }

}
