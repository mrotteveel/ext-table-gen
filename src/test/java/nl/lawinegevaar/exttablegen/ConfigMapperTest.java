// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static nl.lawinegevaar.exttablegen.ResourceHelper.getResourceString;
import static nl.lawinegevaar.exttablegen.ResourceHelper.requireResourceStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigMapperTest {

    private final ConfigMapper configMapper = new ConfigMapper();

    @Test
    void testUsingRoundTrip() throws Exception{
        // The content of happypath-config.xml was (originally) derived from ExtTableGenTest.fromFileToFile_happyPath
        // TODO Consider generating the content on the fly using similar test code instead of using fixed input
        try (InputStream in = requireResourceStream("/testdata/happypath-config.xml")) {
            EtgConfig etgConfig = configMapper.read(in);
            var baos = new ByteArrayOutputStream();
            configMapper.write(etgConfig, baos);

            assertEquals(getResourceString("/testdata/happypath-config.xml"), baos.toString(StandardCharsets.UTF_8));
        }
    }

}