// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class CsvParserConfigMatchers {

    private CsvParserConfigMatchers() {
        // no instances
    }

    static Matcher<CsvParserConfig> type(Matcher<CsvType> matcher) {
        return new FeatureMatcher<>(matcher, "csv type", "csv type") {
            @Override
            protected CsvType featureValueOf(CsvParserConfig actual) {
                return actual.type();
            }
        };
    }

    static Matcher<CsvParserConfig> quoteChar(Matcher<CharValue> matcher) {
        return new FeatureMatcher<>(matcher, "quote char", "quote char") {
            @Override
            protected CharValue featureValueOf(CsvParserConfig actual) {
                return actual.quoteChar();
            }
        };
    }

    static Matcher<CsvParserConfig> separator(Matcher<CharValue> matcher) {
        return new FeatureMatcher<>(matcher, "separator", "separator") {
            @Override
            protected CharValue featureValueOf(CsvParserConfig actual) {
                return actual.separator();
            }
        };
    }

    static Matcher<CsvParserConfig> escapeChar(Matcher<CharValue> matcher) {
        return new FeatureMatcher<>(matcher, "escape char", "escape char") {
            @Override
            protected CharValue featureValueOf(CsvParserConfig actual) {
                return actual.escapeChar();
            }
        };
    }

    static Matcher<CsvParserConfig> ignoreLeadingWhiteSpace(Matcher<Boolean> matcher) {
        return new FeatureMatcher<>(matcher, "ignore leading white space", "ignore leading white space") {
            @Override
            protected Boolean featureValueOf(CsvParserConfig actual) {
                return actual.ignoreLeadingWhiteSpace();
            }
        };
    }

    static Matcher<CsvParserConfig> ignoreQuotations(Matcher<Boolean> matcher) {
        return new FeatureMatcher<>(matcher, "ignore quotations", "ignore quotations") {
            @Override
            protected Boolean featureValueOf(CsvParserConfig actual) {
                return actual.ignoreQuotations();
            }
        };
    }

    static Matcher<CsvParserConfig> strictQuotes(Matcher<Boolean> matcher) {
        return new FeatureMatcher<>(matcher, "strict quotes", "strict quotes") {
            @Override
            protected Boolean featureValueOf(CsvParserConfig actual) {
                return actual.strictQuotes();
            }
        };
    }

}
