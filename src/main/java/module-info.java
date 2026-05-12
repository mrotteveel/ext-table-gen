// SPDX-FileCopyrightText: Copyright 2023-2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
module nl.lawinegevaar.exttablegen {
    requires com.opencsv;
    requires info.picocli;
    requires org.apache.commons.lang3;
    requires jakarta.xml.bind;
    requires java.logging;
    requires org.jspecify;
    requires org.firebirdsql.decimal;
    opens nl.lawinegevaar.exttablegen to info.picocli;
    opens nl.lawinegevaar.exttablegen.xmlconfig to jakarta.xml.bind;
}