// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
module nl.lawinegevaar.exttablegen {
    requires com.opencsv;
    requires info.picocli;
    requires org.apache.commons.lang3;
    requires jakarta.xml.bind;
    requires java.logging;
    opens nl.lawinegevaar.exttablegen to info.picocli;
    opens nl.lawinegevaar.exttablegen.xmlconfig to jakarta.xml.bind;
}