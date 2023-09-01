// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;

import java.util.Objects;

/**
 * Configration for the CSV parser.
 *
 * @param type
 *         CSV type, if {@code null}, {@link CsvType#RFC_4180} is used
 * @param quoteChar
 *         quote character, {@code null} applies OpenCSV default
 * @param separator
 *         separator, {@code null} applies OpenCSV default
 * @param escapeChar
 *         escape character, {@code null} applies OpenCSV default; only applies to {@code CUSTOM}
 * @param ignoreLeadingWhiteSpace
 *         ignore leading white space, {@code null} applies OpenCSV default; only applies to {@code CUSTOM}
 * @param ignoreQuotations
 *         ignore quotations, {@code null} applies OpenCSV default; only applies to {@code CUSTOM}
 * @param strictQuotes
 *         strict quotes, {@code null} applies OpenCSV default; only applies to {@code CUSTOM}
 * @since 2
 */
record CsvParserConfig(CsvType type, CharValue quoteChar, CharValue separator, CharValue escapeChar,
        Boolean ignoreLeadingWhiteSpace, Boolean ignoreQuotations, Boolean strictQuotes) {

    private static final CsvParserConfig DEFAULT_CONFIG = CsvParserConfig.rfc4180(null, null);

    CsvParserConfig {
        if (type == null) {
            type = CsvType.RFC_4180;
        }
    }

    ICSVParser createParser() {
        return switch (type) {
            case RFC_4180 -> createRfc4180Parser();
            case CUSTOM -> createCustomParser();
        };
    }

    private RFC4180Parser createRfc4180Parser() {
        var builder = new RFC4180ParserBuilder();
        if (quoteChar != null) {
            builder.withQuoteChar(quoteChar.value());
        }
        if (separator != null) {
            builder.withSeparator(separator.value());
        }
        return builder.build();
    }

    private CSVParser createCustomParser() {
        var builder = new CSVParserBuilder();
        if (quoteChar != null) {
            builder.withQuoteChar(quoteChar.value());
        }
        if (separator != null) {
            builder.withSeparator(separator.value());
        }
        if (escapeChar != null) {
            builder.withEscapeChar(escapeChar.value());
        }
        if (ignoreLeadingWhiteSpace != null) {
            builder.withIgnoreLeadingWhiteSpace(ignoreLeadingWhiteSpace);
        }
        if (ignoreQuotations != null) {
            builder.withIgnoreQuotations(ignoreQuotations);
        }
        if (strictQuotes != null) {
            builder.withStrictQuotes(strictQuotes);
        }
        return builder.build();
    }

    static CsvParserConfig of() {
        return DEFAULT_CONFIG;
    }

    static CsvParserConfig rfc4180(CharValue quoteChar, CharValue separator) {
        return new CsvParserConfig(CsvType.RFC_4180, quoteChar, separator, null, null, null, null);
    }

    static CsvParserConfig custom(CharValue quoteChar, CharValue separator, CharValue escapeChar,
            Boolean ignoreLeadingWhiteSpace, Boolean ignoreQuotations, Boolean strictQuotes) {
        return new CsvParserConfig(CsvType.CUSTOM, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withType(CsvType type) {
        if (this.type == type || type == null && this.type == CsvType.RFC_4180) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withQuoteChar(CharValue quoteChar) {
        if (Objects.equals(this.quoteChar, quoteChar)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withSeparator(CharValue separator) {
        if (Objects.equals(this.separator, separator)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withEscapeChar(CharValue escapeChar) {
        if (Objects.equals(this.escapeChar, escapeChar)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withIgnoreLeadingWhiteSpace(Boolean ignoreLeadingWhiteSpace) {
        if (Objects.equals(this.ignoreLeadingWhiteSpace, ignoreLeadingWhiteSpace)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withIgnoreQuotations(Boolean ignoreQuotations) {
        if (Objects.equals(this.ignoreQuotations, ignoreQuotations)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

    CsvParserConfig withStrictQuotes(Boolean strictQuotes) {
        if (Objects.equals(this.strictQuotes, strictQuotes)) return this;
        return new CsvParserConfig(type, quoteChar, separator, escapeChar, ignoreLeadingWhiteSpace,
                ignoreQuotations, strictQuotes);
    }

}
