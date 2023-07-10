// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

final class ResourceHelper {

    private ResourceHelper() {
        // no instances
    }

    /**
     * Gets the content of {@code resourceName} decoded using UTF-8 as a string.
     *
     * @param resourceName
     *         resource name, either relative to {@link ResourceHelper}, or an absolute resource name
     * @return content of {@code resourceName} as a string
     * @throws IOException
     *         for errors reading from the stream
     * @throws NullPointerException
     *         if the resource was not found
     */
    static String getResourceString(String resourceName) throws IOException {
        try (InputStream in = requireResourceStream(resourceName)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets a resource as input stream.
     *
     * @param resourceName
     *         resource name, either relative to {@link ResourceHelper}, or an absolute resource name
     * @return input stream
     * @throws NullPointerException
     *         if the resource was not found
     */
    static InputStream requireResourceStream(String resourceName) {
        return requireNonNull(
                ResourceHelper.class.getResourceAsStream(resourceName),
                () -> "resource '%s' not found".formatted(resourceName));
    }

    /**
     * Copies the content of {@code resourceName} to the file {@code destinationPath}.
     * <p>
     * The directory containing {@code destinationPath} is expected to already exist. If the file already exists, it
     * is overwritten
     * </p>
     *
     * @param resourceName
     *         resource name, either relative to {@link ResourceHelper}, or an absolute resource name
     * @param destinationPath
     *         path of the file to write
     * @throws IOException for errors reading from the resource, or creating or writing to {@code destinationPath}
     */
    static void copyResource(String resourceName, Path destinationPath) throws IOException {
        try (InputStream in = requireResourceStream(resourceName);
             OutputStream out = Files.newOutputStream(destinationPath)) {
            in.transferTo(out);
        }
    }
}
