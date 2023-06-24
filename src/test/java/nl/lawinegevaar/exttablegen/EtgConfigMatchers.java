// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

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

    static Matcher<EtgConfig> emptyInputConfig() {
        return new InputConfigMatcher(emptyOptional());
    }

    static Matcher<EtgConfig> inputConfig(Matcher<InputConfig> matcher) {
        return new InputConfigMatcher(optionalWithValue(matcher));
    }

    static Matcher<TableConfig> tableName(Matcher<String> matcher) {
        return new FeatureMatcher<>(matcher, "table name", "table name") {
            @Override
            protected String featureValueOf(TableConfig actual) {
                return actual.name();
            }
        };
    }

    static Matcher<TableConfig> tableColumns(Matcher<Collection<Column>> matcher) {
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

    static Matcher<InputConfig> inputConfigPath(Matcher<Path> matcher) {
        return new FeatureMatcher<>(matcher, "input path", "input path") {
            @Override
            protected Path featureValueOf(InputConfig actual) {
                return actual.path();
            }
        };
    }

    static Matcher<InputConfig> inputConfigCharset(Matcher<Charset> matcher) {
        return new FeatureMatcher<>(matcher, "input charset", "input charset") {
            @Override
            protected Charset featureValueOf(InputConfig actual) {
                return actual.charset();
            }
        };
    }

    static Matcher<InputConfig> inputConfigHasHeaderRow(boolean hasHeaderRow) {
        return new FeatureMatcher<>(is(hasHeaderRow), "input has header row", "input has header row") {
            @Override
            protected Boolean featureValueOf(InputConfig actual) {
                return actual.hasHeaderRow();
            }
        };
    }

    private static class InputConfigMatcher extends FeatureMatcher<EtgConfig, Optional<InputConfig>> {

        InputConfigMatcher(Matcher<? super Optional<InputConfig>> subMatcher) {
            super(subMatcher, "input config", "input config");
        }

        @Override
        protected Optional<InputConfig> featureValueOf(EtgConfig actual) {
            return actual.inputConfig();
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
