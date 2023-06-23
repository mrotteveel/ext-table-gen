// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.col;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.customers10Columns;
import static nl.lawinegevaar.exttablegen.ResourceHelper.copyResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("HttpUrlsUsage")
class ExtTableGenMainTest {

    private static final String DUMMY = "DUMMY";

    private static final ConfigMapper configMapper = new ConfigMapper();

    @TempDir
    private Path tempDir;

    private Path inputFile;
    private Path outputFile;
    private Path outConfigFile;

    @BeforeEach
    void initFilePaths() {
        inputFile = tempDir.resolve("input.csv");
        outputFile = tempDir.resolve("output.dat");
        outConfigFile = tempDir.resolve("config.xml");
    }

    @Test
    void fromFileToFile_happyPath() throws Exception {
        copyResource("/testdata/customers-10.csv", inputFile);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--config-out=" + outConfigFile));

        assertConfigFile(
                new EtgConfig(
                        new TableConfig("DEFAULT_EXTERNAL_TABLE_NAME", customers10Columns(EndColumn.Type.LF, FbEncoding.ISO8859_1),
                                new OutputConfig(outputFile, false)),
                        TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER),
                        new InputConfig(inputFile, UTF_8, true)),
                outConfigFile);
        assertEquals("""
                1 DD37Cf93aecA6DcSheryl  Baxter   Rasmussen Group                East Leonard     Chile                     229.077.5154          397.884.0519x718     zunigavanessa@smith.info   2020-08-24http://www.stephenson.com/\s
                2 1Ef7b82A4CAAD10Preston Lozano   Vega-Gentry                    East JimmychesterDjibouti                  5153435776            686-620-1820x944     vmata@colon.com            2021-04-23http://www.hobbs.com/     \s
                3 6F94879bDAfE5a6Roy     Berry    Murillo-Perry                  Isabelborough    Antigua and Barbuda       +1-539-402-0259       (496)978-3969x58947  beckycarr@hogan.com        2020-03-25http://www.lawrence.com/  \s
                4 5Cef8BFA16c5e3cLinda   Olsen    Dominguez, Mcmillan and DonovanBensonview       Dominican Republic        001-808-617-6467x12895+1-813-324-8756      stanleyblackwell@benson.org2020-06-02http://www.good-lyons.com/\s
                5 053d585Ab6b3159Joanna  Bender   Martin, Lang and Andrade       West Priscilla   Slovakia (Slovak Republic)001-234-203-0635x76146001-199-446-3860x3486colinalvarado@miles.net    2021-04-17https://goodwin-ingram.com/
                6 2d08FB17EE273F4Aimee   Downs    Steele Group                   Chavezborough    Bosnia and Herzegovina    (283)437-3886x88321   999-728-1637         louis27@gilbert.com        2020-02-25http://www.berger.net/    \s
                7 EA4d384DfDbBf77Darren  Peck     Lester, Woodard and Mitchell   Lake Ana         Pitcairn Islands          (496)452-6181x3291    +1-247-266-0963x4995 tgates@cantrell.com        2021-08-24https://www.le.com/       \s
                8 0e04AFde9f225dEBrett   Mullen   Sanford, Davenport and Giles   Kimport          Bulgaria                  001-583-352-7197x297  001-333-145-0369     asnow@colon.com            2021-04-12https://hammond-ramsey.com/
                9 C2dE4dEEc489ae0Sheryl  Meyers   Browning-Simon                 Robersonstad     Cyprus                    854-138-4911x5772     +1-448-910-2276x729  mariokhan@ryan-pope.org    2020-01-13https://www.bullock.net/  \s
                108C2811a503C7c5aMichelleGallagherBeck-Hendrix                   Elaineberg       Timor-Leste               739.218.2516x459      001-054-401-0347x617 mdyer@escobar.net          2021-11-08https://arias.com/        \s
                """, Files.readString(outputFile, ISO_8859_1));
    }

    @Test
    void fromFileToFile_happyPath_usingConfigFile() throws Exception {
        copyResource("/testdata/customers-10.csv", inputFile);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--config-out=" + outConfigFile));

        Files.delete(outputFile);
        Path configFile = outConfigFile;
        String originalConfigFile = Files.readString(configFile);
        var outConfigFile = tempDir.resolve("newconfig.xml");

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--config-in=" + configFile, "--config-out=" + outConfigFile));

        assertConfigFile(
                new EtgConfig(
                        new TableConfig("DEFAULT_EXTERNAL_TABLE_NAME", customers10Columns(EndColumn.Type.LF, FbEncoding.ISO8859_1),
                                new OutputConfig(outputFile, false)),
                        TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER),
                        new InputConfig(inputFile, UTF_8, true)),
                outConfigFile);
        String newConfigFile = Files.readString(outConfigFile);
        System.out.println(newConfigFile);
        assertEquals(originalConfigFile, newConfigFile);

        assertEquals("""
                1 DD37Cf93aecA6DcSheryl  Baxter   Rasmussen Group                East Leonard     Chile                     229.077.5154          397.884.0519x718     zunigavanessa@smith.info   2020-08-24http://www.stephenson.com/\s
                2 1Ef7b82A4CAAD10Preston Lozano   Vega-Gentry                    East JimmychesterDjibouti                  5153435776            686-620-1820x944     vmata@colon.com            2021-04-23http://www.hobbs.com/     \s
                3 6F94879bDAfE5a6Roy     Berry    Murillo-Perry                  Isabelborough    Antigua and Barbuda       +1-539-402-0259       (496)978-3969x58947  beckycarr@hogan.com        2020-03-25http://www.lawrence.com/  \s
                4 5Cef8BFA16c5e3cLinda   Olsen    Dominguez, Mcmillan and DonovanBensonview       Dominican Republic        001-808-617-6467x12895+1-813-324-8756      stanleyblackwell@benson.org2020-06-02http://www.good-lyons.com/\s
                5 053d585Ab6b3159Joanna  Bender   Martin, Lang and Andrade       West Priscilla   Slovakia (Slovak Republic)001-234-203-0635x76146001-199-446-3860x3486colinalvarado@miles.net    2021-04-17https://goodwin-ingram.com/
                6 2d08FB17EE273F4Aimee   Downs    Steele Group                   Chavezborough    Bosnia and Herzegovina    (283)437-3886x88321   999-728-1637         louis27@gilbert.com        2020-02-25http://www.berger.net/    \s
                7 EA4d384DfDbBf77Darren  Peck     Lester, Woodard and Mitchell   Lake Ana         Pitcairn Islands          (496)452-6181x3291    +1-247-266-0963x4995 tgates@cantrell.com        2021-08-24https://www.le.com/       \s
                8 0e04AFde9f225dEBrett   Mullen   Sanford, Davenport and Giles   Kimport          Bulgaria                  001-583-352-7197x297  001-333-145-0369     asnow@colon.com            2021-04-12https://hammond-ramsey.com/
                9 C2dE4dEEc489ae0Sheryl  Meyers   Browning-Simon                 Robersonstad     Cyprus                    854-138-4911x5772     +1-448-910-2276x729  mariokhan@ryan-pope.org    2020-01-13https://www.bullock.net/  \s
                108C2811a503C7c5aMichelleGallagherBeck-Hendrix                   Elaineberg       Timor-Leste               739.218.2516x459      001-054-401-0347x617 mdyer@escobar.net          2021-11-08https://arias.com/        \s
                """, Files.readString(outputFile, ISO_8859_1));
    }

    @Test
    void defaultDisallowsOverwriteOfOutput() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);
        Files.writeString(outputFile, DUMMY);

        assertEquals(1,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--config-out=" + outConfigFile));

        assertConfigFile(
                new EtgConfig(
                        new TableConfig("DEFAULT_EXTERNAL_TABLE_NAME",
                                List.of(col("column1", 3), col("column2", 2),
                                        EndColumn.Type.LF.getEndColumn().orElseThrow()),
                                new OutputConfig(outputFile, false)),
                        TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER),
                        new InputConfig(inputFile, UTF_8, true)),
                outConfigFile);
        assertEquals(DUMMY, Files.readString(outputFile, ISO_8859_1), "Expected output file not overwritten");
    }

    @Test
    void allowsOverwriteOfOutput() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);
        Files.writeString(outputFile, DUMMY);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--overwrite-output"));

        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1));
    }

    @Test
    void defaultDisallowOverwriteConfig() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);
        Files.writeString(outConfigFile, DUMMY);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--config-out=" + outConfigFile));

        assertEquals(DUMMY, Files.readString(outConfigFile), "Expected config file not overwritten");
        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void allowOverwriteConfig() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);
        Files.writeString(outConfigFile, DUMMY);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--config-out=" + outConfigFile, "--overwrite-config"));

        assertNotEquals(DUMMY, Files.readString(outConfigFile), "Expected config file overwritten");
        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void endColumnLF() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--end-column=LF"));

        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void endColumnCRLF() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--end-column=CRLF"));

        assertEquals("""
                ab cd\r
                efgh \r
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void endColumnNONE() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--end-column=NONE"));

        assertEquals("ab cdefgh ", Files.readString(outputFile, ISO_8859_1),
                "Expected output file to have been written");
    }

    @Test
    void noInputHeader() throws Exception {
        Files.writeString(inputFile, """
                ab,cd
                efg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--no-input-header"));

        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void inputHeader() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                efg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--input-header"));

        assertEquals("""
                ab cd
                efgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void inputCharset() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                éfg,h
                """, ISO_8859_1);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--input-charset=iso-8859-1"));

        assertEquals("""
                ab cd
                éfgh\s
                """, Files.readString(outputFile, ISO_8859_1), "Expected output file to have been written");
    }

    @Test
    void columnEncoding() throws Exception {
        Files.writeString(inputFile, """
                column1,column2
                ab,cd
                éfg,h
                """);

        assertEquals(0,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--table-name=TABLE1", "--column-encoding=UTF8", "--config-out=" + outConfigFile));

        assertEquals("""
                ab          cd     \s
                éfg        h      \s
                """, Files.readString(outputFile, UTF_8), "Expected output file to have been written");
        assertConfigFile(
                new EtgConfig(
                        new TableConfig("TABLE1",
                                List.of(
                                        col("column1", 3, FbEncoding.UTF8),
                                        col("column2", 2, FbEncoding.UTF8),
                                        EndColumn.require(EndColumn.Type.LF)),
                                new OutputConfig(outputFile, false)),
                        new TableDerivationConfig(FbEncoding.UTF8, EndColumn.Type.LF, TableDerivationMode.NEVER),
                        new InputConfig(inputFile, UTF_8, true)),
                outConfigFile);
    }

    @Test
    void failOnColumnCountOverflow() throws Exception {
        Files.writeString(inputFile, """
                column1, column2
                ab,cd
                ab,cd,ef
                """);

        assertEquals(1,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--column-encoding=UTF8"));

        assertFalse(Files.exists(outputFile), "Expected output file to not exist");
    }

    @Test
    void failOnColumnCountUnderflow() throws Exception {
        Files.writeString(inputFile, """
                column1, column2, column3
                ab,cd,ef
                ab,cd
                """);

        assertEquals(1,
                ExtTableGenMain.parseAndExecute("--input-file=" + inputFile, "--output-file=" + outputFile,
                        "--column-encoding=UTF8"));

        assertFalse(Files.exists(outputFile), "Expected output file to not exist");
    }

    static EtgConfig readConfig(Path configFile) throws IOException, JAXBException {
        try (var in = Files.newInputStream(configFile)) {
            return configMapper.read(in);
        }
    }

    /**
     * Asserts the contents of {@code configFile} against {@code expectedConfig}.
     * <p>
     * Be aware that the {@link TableDerivationConfig#mode()} of {@link EtgConfig#tableDerivationConfig()} from a file
     * is always {@link TableDerivationMode#NEVER}.
     * </p>
     *
     * @param expectedConfig
     *         expected config
     * @param configFile
     *         configuration file to read
     * @throws IOException
     *         for errors reading the file
     * @throws JAXBException
     *         for errors parsing the file
     */
    static void assertConfigFile(EtgConfig expectedConfig, Path configFile) throws IOException, JAXBException {
        assertTrue(Files.exists(configFile), "config file '%s' does not exist".formatted(configFile));
        EtgConfig configFromFile = readConfig(configFile);

        assertEquals(expectedConfig, configFromFile, "Configuration does not match");
    }

}