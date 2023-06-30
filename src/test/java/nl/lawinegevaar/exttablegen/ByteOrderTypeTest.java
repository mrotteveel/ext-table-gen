// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class ByteOrderTypeTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            byteOrderType, expectedName
            LITTLE_ENDIAN, LITTLE_ENDIAN
            BIG_ENDIAN,    BIG_ENDIAN
            AUTO,
            """)
    void testEffectiveValue(ByteOrderType byteOrderType, String expectedName) {
        if (byteOrderType == ByteOrderType.AUTO) {
            expectedName = ByteOrder.nativeOrder().toString();
        }
        assertEquals(expectedName, byteOrderType.effectiveValue().name());
    }

}