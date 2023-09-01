// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Represents the value of a character for CSV parser configuration.
 * 
 * @since 2
 */
sealed interface CharValue permits CharMnemonic, SimpleChar, UnicodeEscape {

    /**
     * @return the character value
     */
    char value();

    /**
     * @return string representation of this character (for a single character, that character, for a mnemonic,
     * the mnemonic, and for a Unicode escape, the escape).
     */
    String toString();

    /**
     * Converts {@code value} to the appropriate instance of {@link CharValue}.
     * <p>
     * Valid value are:
     * </p>
     * <ul>
     * <li>A string containing a single character</li>
     * <li>A string representing a Unicode escape of the form {@code U+hhhh} with {@code h} a hexadecimal digit
     * (case-insensitive)</li>
     * <li>A string representing one of the supported character mnemonics (case-insensitive): {@code TAB},
     * {@code SPACE}, {@code QUOT}, {@code APOS}, and {@code GRAVE}</li>
     * </ul>
     *
     * @param value
     *         value, {@code null} will return {@code null}
     * @return an instance of {@link CharValue} representing the character, or {@code null} if {@code value}
     * is {@code null}
     * @throws IllegalArgumentException
     *         if {@code value} is not {@code null}, and is not a valid value
     */
    static CharValue of(String value) {
        if (value == null) return null;
        if (value.length() == 1) {
            return new SimpleChar(value.charAt(0));
        }
        if (value.startsWith("U+") || value.startsWith("u+")) {
            return UnicodeEscape.of(value);
        }
        return CharMnemonic.of(value);
    }

    /**
     * Creates a {@link CharValue} for a character.
     *
     * @param value
     *         character value
     * @return instance of {@link SimpleChar}
     */
    static CharValue of(char value) {
        return new SimpleChar(value);
    }

}

/**
 * A CSV config character represented as a single character.
 *
 * @param value
 *         character value
 * @since 2
 */
record SimpleChar(char value) implements CharValue {

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}

/**
 * Represents a character mnemonic (named character value).
 *
 * @since 2
 */
final class CharMnemonic implements CharValue {

    private static final Map<String, CharMnemonic> MNEMONIC_MAP;
    static {
        var mnemonicMap = new TreeMap<String, CharMnemonic>(String.CASE_INSENSITIVE_ORDER);
        Stream.of(
                        new CharMnemonic("TAB", '\t'),
                        new CharMnemonic("SPACE", ' '),
                        new CharMnemonic("QUOT", '"'),
                        new CharMnemonic("APOS", '\''),
                        new CharMnemonic("GRAVE", '`'))
                .forEach(m -> mnemonicMap.put(m.mnemonic, m));
        MNEMONIC_MAP = unmodifiableMap(mnemonicMap);
    }

    private final String mnemonic;
    private final char value;

    private CharMnemonic(String mnemonic, char value) {
        if (requireNonNull(mnemonic, "mnemonic").length() < 2) {
            throw new IllegalArgumentException("A mnemonic must be at least 2 characters");
        }
        this.mnemonic = mnemonic;
        this.value = value;
    }

    /**
     * Get the character mnemonic.
     *
     * @param mnemonic
     *         mnemonic name
     * @return character mnemonic
     * @throws IllegalArgumentException
     *         if there is no mnemonic by the name {@code mnemonic}
     */
    static CharMnemonic of(String mnemonic) {
        CharMnemonic characterMnemonic = MNEMONIC_MAP.get(mnemonic);
        if (characterMnemonic == null) {
            throw new IllegalArgumentException("There is no mnemonic defined for '%s'".formatted(mnemonic));
        }
        return characterMnemonic;
    }

    @Override
    public char value() {
        return value;
    }

    @Override
    public String toString() {
        return mnemonic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof CharMnemonic that
               && this.mnemonic.equals(that.mnemonic)
               && this.value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mnemonic, value);
    }

}

/**
 * A Unicode escape for a character value.
 *
 * @since 2
 */
final class UnicodeEscape implements CharValue {

    private static final Pattern UNICODE_ESCAPE_PATTERN =
            Pattern.compile("U\\+([0-9A-F]{4})", Pattern.CASE_INSENSITIVE);

    private final String unicodeEscape;
    private final char value;

    private UnicodeEscape(String unicodeEscape, char value) {
        this.unicodeEscape = unicodeEscape;
        this.value = value;
    }

    /**
     * Creates an instance of {@code UnicodeEscape} from a string.
     *
     * @param unicodeEscape
     *         string representation of unicode escape
     * @return unicode escape instance
     * @throws NullPointerException
     *         if {@code unicodeEscape} is {@code null}
     * @throws IllegalArgumentException
     *         if {@code unicodeEscape} does not match the expected pattern
     */
    static UnicodeEscape of(String unicodeEscape) {
        Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(requireNonNull(unicodeEscape, "unicodeEscape"));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Unicode escape '%s' does not match pattern '%s'".formatted(unicodeEscape, UNICODE_ESCAPE_PATTERN));
        }
        return new UnicodeEscape(unicodeEscape, (char) Integer.parseInt(matcher.group(1), 16));
    }

    @Override
    public char value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof UnicodeEscape that
               && Objects.equals(this.unicodeEscape, that.unicodeEscape)
               && this.value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unicodeEscape, value);
    }

    @Override
    public String toString() {
        return unicodeEscape;
    }

}
