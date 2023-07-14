// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.is;

/**
 * Hamcrest matchers for {@link EtgConfig} and configuration classes used in {@code EtgConfig}.
 */
final class EtgConfigMatchers {

    private EtgConfigMatchers() {
        // no instances
    }

    static Matcher<EtgConfig> tableConfig(Matcher<TableConfig> matcher) {
        return new FeatureMatcher<>(matcher, "table config", "table config") {
            @Override
            protected TableConfig featureValueOf(EtgConfig actual) {
                return actual.tableConfig();
            }
        };
    }

    static Matcher<EtgConfig> tableDerivationConfig(Matcher<TableDerivationConfig> matcher) {
        return new FeatureMatcher<>(matcher, "table derivation config", "table derivation config") {
            @Override
            protected TableDerivationConfig featureValueOf(EtgConfig actual) {
                return actual.tableDerivationConfig();
            }
        };
    }

    static Matcher<EtgConfig> emptyCavFileConfig() {
        return new CsvFileConfigMatcher(emptyOptional());
    }

    static Matcher<EtgConfig> csvFileConfig(Matcher<CsvFileConfig> matcher) {
        return new CsvFileConfigMatcher(optionalWithValue(matcher));
    }

    static Matcher<TableConfig> tableName(Matcher<String> matcher) {
        return new FeatureMatcher<>(matcher, "table name", "table name") {
            @Override
            protected String featureValueOf(TableConfig actual) {
                return actual.name();
            }
        };
    }

    static Matcher<TableConfig> tableColumns(Matcher<? super Collection<Column>> matcher) {
        return new FeatureMatcher<TableConfig, List<Column>>(matcher, "table columns", "table columns") {
            @Override
            protected List<Column> featureValueOf(TableConfig actual) {
                return actual.columns();
            }
        };
    }

    static Matcher<TableConfig> emptyTableFile() {
        return new TableFileMatcher(emptyOptional());
    }

    static Matcher<TableConfig> tableFile(Matcher<TableFile> matcher) {
        return new TableFileMatcher(optionalWithValue(matcher));
    }

    static Matcher<TableFile> tableFilePath(Matcher<Path> matcher) {
        return new FeatureMatcher<>(matcher, "table file path", "table file path") {
            @Override
            protected Path featureValueOf(TableFile actual) {
                return actual.path();
            }
        };
    }

    static Matcher<TableFile> tableFileOverwrite(boolean overwrite) {
        return new FeatureMatcher<>(is(overwrite), "table file overwrite", "table file overwrite") {
            @Override
            protected Boolean featureValueOf(TableFile actual) {
                return actual.overwrite();
            }
        };
    }

    static Matcher<TableDerivationConfig> tableDerivationColumnEncoding(Matcher<FbEncoding> matcher) {
        return new FeatureMatcher<>(matcher, "encoding", "encoding") {
            @Override
            protected FbEncoding featureValueOf(TableDerivationConfig actual) {
                return actual.columnEncoding();
            }
        };
    }

    static Matcher<TableDerivationConfig> tableDerivationEndColumnType(Matcher<EndColumn.Type> matcher) {
        return new FeatureMatcher<>(matcher, "end column type", "end column type") {
            @Override
            protected EndColumn.Type featureValueOf(TableDerivationConfig actual) {
                return actual.endColumnType();
            }
        };
    }

    static Matcher<TableDerivationConfig> tableDerivationMode(Matcher<TableDerivationMode> matcher) {
        return new FeatureMatcher<>(matcher, "table derivation mode", "table derivation mode") {
            @Override
            protected TableDerivationMode featureValueOf(TableDerivationConfig actual) {
                return actual.mode();
            }
        };
    }

    static Matcher<CsvFileConfig> csvFilePath(Matcher<Path> matcher) {
        return new FeatureMatcher<>(matcher, "CSV file path", "CSV file path") {
            @Override
            protected Path featureValueOf(CsvFileConfig actual) {
                return actual.path();
            }
        };
    }

    static Matcher<CsvFileConfig> csvCharset(Matcher<Charset> matcher) {
        return new FeatureMatcher<>(matcher, "CSV charset", "CSV charset") {
            @Override
            protected Charset featureValueOf(CsvFileConfig actual) {
                return actual.charset();
            }
        };
    }

    static Matcher<CsvFileConfig> csvHeaderRow(boolean headerRow) {
        return new FeatureMatcher<>(is(headerRow), "CSV header row", "CSV header row") {
            @Override
            protected Boolean featureValueOf(CsvFileConfig actual) {
                return actual.headerRow();
            }
        };
    }

    private static class CsvFileConfigMatcher extends FeatureMatcher<EtgConfig, Optional<CsvFileConfig>> {

        CsvFileConfigMatcher(Matcher<? super Optional<CsvFileConfig>> subMatcher) {
            super(subMatcher, "CSV file config", "CSV file config");
        }

        @Override
        protected Optional<CsvFileConfig> featureValueOf(EtgConfig actual) {
            return actual.csvFileConfig();
        }

    }

    private static class TableFileMatcher extends FeatureMatcher<TableConfig, Optional<TableFile>> {

        public TableFileMatcher(Matcher<? super Optional<TableFile>> subMatcher) {
            super(subMatcher, "table file", "table file");
        }

        @Override
        protected Optional<TableFile> featureValueOf(TableConfig actual) {
            return actual.tableFile();
        }
    }
}
