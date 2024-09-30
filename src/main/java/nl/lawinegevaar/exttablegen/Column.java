// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbDatatype;

import java.io.IOException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a column with a name and datatype.
 */
sealed class Column permits EndColumn {

    private final String name;
    private final FbDatatype<?> datatype;

    /**
     * Creates a column.
     *
     * @param name
     *         name of the column (must not be {@code null} or blank), value is trimmed
     * @param datatype
     *         datatype of the column
     * @throws IllegalArgumentException
     *         if {@code name} is {@code null} or blank
     */
    @SuppressWarnings("ConstantValue")
    Column(String name, FbDatatype<?> datatype) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null, empty or blank");
        }
        this.name = name.trim();
        this.datatype = requireNonNull(datatype, "datatype");
    }

    /**
     * @return column name
     */
    final String name() {
        return name;
    }

    /**
     * @return quoted column name (if {@link #name()} is already quoted, it is returned as-is)
     * @see SqlSyntaxUtils#enquoteIdentifier(String)
     */
    final String quotedName() {
        return SqlSyntaxUtils.enquoteIdentifier(name());
    }

    /**
     * @return column datatype
     */
    final FbDatatype<?> datatype() {
        return datatype;
    }

    /**
     * Write value, taking into account maximum column size.
     *
     * @param value
     *         value to write
     * @param out
     *         output stream to write to
     */
    void writeValue(String value, EncoderOutputStream out) throws IOException {
        datatype.writeValue(value, out);
    }

    /**
     * Writes an empty value to {@code out}.
     *
     * @param out
     *         output stream to write to
     */
    void writeEmpty(EncoderOutputStream out) throws IOException {
        datatype.writeEmpty(out);
    }

    /**
     * Appends the column definition to a string builder.
     * <p>
     * This implementation appends:
     * </p>
     * <ul>
     * <li>{@link #quotedName()}</li>
     * <li>&lt;SPACE&gt;</li>
     * <li>{@link FbDatatype#appendTypeDefinition(StringBuilder)} of {@link #datatype()}</li>
     * </ul>
     *
     * @param sb
     *         string builder
     */
    void appendColumnDefinition(StringBuilder sb) {
        sb.append(quotedName()).append(' ');
        datatype.appendTypeDefinition(sb);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Column) obj;
        return this.name.equals(that.name) &&
               this.datatype.equals(that.datatype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, datatype);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + '{' +
               "name=" + name + ", " +
               "datatype=" + datatype + '}';
    }

}
