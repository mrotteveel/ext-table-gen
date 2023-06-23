// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvParsingException;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StopOnExceptionProcessorTest {

    @ParameterizedTest
    @MethodSource
    void stopsOnAnyException_rootIsException(Exception e) {
        var processor = new StopOnExceptionProcessor(Exception.class);
        assertInstanceOf(ProcessingResult.Stop.class, processor.onException(e));
    }

    @Test
    void stopsOnSpecificException() {
        var processor = new StopOnExceptionProcessor(CsvValidationException.class);

        assertInstanceOf(ProcessingResult.Stop.class, processor.onException(new CsvValidationException("Test")));
        assertInstanceOf(ProcessingResult.Continue.class, processor.onException(new RuntimeException("Test")));
    }

    static Stream<Exception> stopsOnAnyException_rootIsException() {
        return Stream.of(new Exception("test"), new RuntimeException("test"), new IOException("test"),
                new CsvValidationException("test"), new CsvParsingException("test"));
    }

}