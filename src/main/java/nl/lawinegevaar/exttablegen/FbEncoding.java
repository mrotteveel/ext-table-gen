// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A Firebird character set encoding and its mapping to Java character sets.
 */
final class FbEncoding {

    private static final Map<String, FbEncoding> encodings = new ConcurrentHashMap<>();

    static final FbEncoding ASCII = FbEncoding.forName("ASCII");
    static final FbEncoding ISO8859_1 = FbEncoding.forName("ISO8859_1");
    static final FbEncoding UTF8 = FbEncoding.forName("UTF8");

    private final String firebirdName;
    private final Charset charset;
    private final int maxBytesPerCharacter;

    private FbEncoding(String firebirdName, Charset charset, int maxBytesPerCharacter) {
        this.firebirdName = requireNonNull(firebirdName, "firebirdName");
        this.charset = requireNonNull(charset, "charset");
        if (maxBytesPerCharacter < 1 || maxBytesPerCharacter > 4) {
            // Technically bigger maxBytesPerCharacter should work fine, they just don't occur in practice)
            throw new IllegalArgumentException(
                    "maxBytesPerCharacter out of range, should be between 1 and 4, but was " + maxBytesPerCharacter);
        }
        this.maxBytesPerCharacter = maxBytesPerCharacter;
    }

    private FbEncoding(String firebirdName, String charsetName, int maxBytesPerCharacter) {
        this(firebirdName, Charset.forName(charsetName), maxBytesPerCharacter);
    }

    /**
     * Calculates the maximum byte length for {@code charLength} codepoints in this encoding.
     *
     * @param charLength
     *         character length in Unicode codepoints
     * @return maximum byte length for {@code charLength} codepoints
     */
    int maxByteLength(int charLength) {
        return charLength * maxBytesPerCharacter;
    }

    /**
     * @return Firebird character set name
     */
    String firebirdName() {
        return firebirdName;
    }

    /**
     * @return name of the Java {@code Charset} (see {@link #charset()}
     */
    String charsetName() {
        return charset.name();
    }

    /**
     * @return the Java {@code Charset}
     */
    Charset charset() {
        return charset;
    }

    /**
     * Returns the bytes of the value in this encoding.
     *
     * @param value
     *         value to convert to bytes (not {@code null})
     * @param length
     *         length in {@code char} to encode (starting with the first character of the string)
     * @return string as bytes in this encoding
     */
    byte[] getBytes(String value, int length) {
        return value.substring(0, length).getBytes(charset);
    }

    @Override
    public String toString() {
        return firebirdName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FbEncoding that)) return false;
        // Given the method of construction, comparing firebirdName should be sufficient
        return firebirdName.equals(that.firebirdName);
    }

    @Override
    public int hashCode() {
        return firebirdName.hashCode();
    }

    /**
     * Retrieves or creates a Firebird encoding of the specified name.
     *
     * @param firebirdName
     *         name of the Firebird character set encoding (case-insensitive, the name of the returned encoding is
     *         always uppercase)
     * @return encoding instance
     * @throws IllegalArgumentException
     *         if {@code firebirdName} is an unknown encoding (at least, not known to this program), explicitly not
     *         supported (i.e. NONE, OCTETS, UNICODE_FSS), or the associated Java character set could not be loaded
     */
    static FbEncoding forName(String firebirdName) {
        // create encodings lazily, on some platforms loading character sets can be slow (see Jaybird history)
        return encodings.computeIfAbsent(firebirdName, FbEncoding::createForName);
    }

    private static FbEncoding createForName(String firebirdName) {
        return switch (firebirdName.toUpperCase(Locale.ROOT)) {
            case "NONE" -> throw new IllegalArgumentException(
                    "Character set NONE not supported, use a specific character set");
            case "OCTETS" -> throw new IllegalArgumentException("Character set OCTETS not supported");
            case "ASCII" -> new FbEncoding("ASCII", StandardCharsets.US_ASCII, 1);
            // We don't want to implement writing only max 3 bytes and correct UNICODE_FSS
            case "UNICODE_FSS" ->
                    throw new IllegalArgumentException("Character set UNICODE_FSS not supported, use UTF8 instead");
            case "UTF8" -> new FbEncoding("UTF8", StandardCharsets.UTF_8, 4);
            // TODO Need to know how to handle spaces
            // case "SJIS_0208" -> new FbEncoding("SJIS_0208", "MS932", 2);
            // case "EUCJ_0208" -> new FbEncoding("EUCJ_0208", "EUC_JP", 2);
            case "DOS737" -> new FbEncoding("DOS737", "Cp737", 1);
            case "DOS437" -> new FbEncoding("DOS437", "Cp437", 1);
            case "DOS850" -> new FbEncoding("DOS850", "Cp850", 1);
            case "DOS865" -> new FbEncoding("DOS865", "Cp865", 1);
            case "DOS860" -> new FbEncoding("DOS860", "Cp860", 1);
            case "DOS863" -> new FbEncoding("DOS863", "Cp863", 1);
            case "DOS775" -> new FbEncoding("DOS775", "Cp775", 1);
            case "DOS858" -> new FbEncoding("DOS858", "Cp858", 1);
            case "DOS862" -> new FbEncoding("DOS862", "Cp862", 1);
            case "DOS864" -> new FbEncoding("DOS864", "Cp864", 1);
            case "ISO8859_1" -> new FbEncoding("ISO8859_1", StandardCharsets.ISO_8859_1, 1);
            case "ISO8859_2" -> new FbEncoding("ISO8859_2", "ISO-8859-2", 1);
            case "ISO8859_3" -> new FbEncoding("ISO8859_3", "ISO-8859-3", 1);
            case "ISO8859_4" -> new FbEncoding("ISO8859_4", "ISO-8859-4", 1);
            case "ISO8859_5" -> new FbEncoding("ISO8859_5", "ISO-8859-5", 1);
            case "ISO8859_6" -> new FbEncoding("ISO8859_6", "ISO-8859-6", 1);
            case "ISO8859_7" -> new FbEncoding("ISO8859_7", "ISO-8859-7", 1);
            case "ISO8859_8" -> new FbEncoding("ISO8859_8", "ISO-8859-8", 1);
            case "ISO8859_9" -> new FbEncoding("ISO8859_9", "ISO-8859-9", 1);
            case "ISO8859_13" -> new FbEncoding("ISO8859_13", "ISO-8859-13", 1);
            // TODO Need to know how to handle spaces
            // case "KSC_5601" -> new FbEncoding("KSC_5601", "MS949", 2);
            case "DOS852" -> new FbEncoding("DOS852", "Cp852", 1);
            case "DOS857" -> new FbEncoding("DOS857", "Cp857", 1);
            case "DOS861" -> new FbEncoding("DOS861", "Cp861", 1);
            case "DOS866" -> new FbEncoding("DOS866", "Cp866", 1);
            case "DOS869" -> new FbEncoding("DOS869", "Cp869", 1);
            case "WIN1250" -> new FbEncoding("WIN1250", "Cp1250", 1);
            case "WIN1251" -> new FbEncoding("WIN1251", "Cp1251", 1);
            case "WIN1252" -> new FbEncoding("WIN1252", "Cp1252", 1);
            case "WIN1253" -> new FbEncoding("WIN1253", "Cp1253", 1);
            case "WIN1254" -> new FbEncoding("WIN1254", "Cp1254", 1);
            // TODO Need to know how to handle spaces
            // case "BIG_5" -> new FbEncoding("BIG_5", "Big5", 2);
            // case "GB_2312" -> new FbEncoding("GB_2312", "EUC_CN", 2);
            case "WIN1255" -> new FbEncoding("WIN1255", "Cp1255", 1);
            case "WIN1256" -> new FbEncoding("WIN1256", "Cp1256", 1);
            case "WIN1257" -> new FbEncoding("WIN1257", "Cp1257", 1);
            case "KOI8R" -> new FbEncoding("KOI8R", "KOI8_R", 1);
            case "KOI8U" -> new FbEncoding("KOI8U", "KOI8_U", 1);
            case "WIN1258" -> new FbEncoding("WIN1258", "Cp1258", 1);
            case "TIS620" -> new FbEncoding("TIS620", "TIS620", 1);
            // TODO Need to know how to handle spaces
            // case "GBK" -> new FbEncoding("GBK", "GBK", 2);
            // case "CP943C" -> new FbEncoding("CP943C", "Cp943C", 2);
            // case "GB18030" -> new FbEncoding("GB18030", "GB18030", 3);
            default -> throw new IllegalArgumentException("Unknown or unsupported Firebird encoding: " + firebirdName);
        };
    }

}
