// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Interface to obtain an input stream from a resource in a repeatable manner.
 */
@FunctionalInterface
interface InputResource {

    /**
     * Creates a new input stream.
     * <p>
     * Each invocation must produce a new input stream which produces the exact same input as other invocations on
     * this instance. This rule has one exception: if the data is no longer available, e.g. because the backing file
     * has been deleted. Implementations must support multiple concurrently open input streams from one instance.
     * </p>
     * <p>
     * In general, the returned input stream will be unbuffered (this does not disallow using in-memory sources or
     * buffering, but is intended to set expectations), and callers are responsible to apply buffering, if any.
     * </p>
     *
     * @return input stream with the data of this resource
     * @throws IOException
     *         for errors opening the input stream, including situations where the backing data (e.g. a file) no longer
     *         exists
     */
    InputStream newInputStream() throws IOException;

    /**
     * Creates a new reader from this input source.
     * <p>
     * The same rules as for {@link #newInputStream()} apply. The returned reader should generally be buffered in some
     * form, but implementations are allowed to deviate from this if necessary.
     * </p>
     * <p>
     * The default implementation is {@code new BufferedReader(new InputStreamReader(createInputStream(), charset))}.
     * </p>
     *
     * @param charset
     *         character set to convert to characters
     * @return reader using {@code charset} around {@link #newInputStream()} (or an equivalent), preferably buffered
     * @throws IOException
     *         for errors opening the input stream, including situations where the backing data (e.g. a file) no longer
     *         exists
     */
    default Reader newReader(Charset charset) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(), charset));
    }

    /**
     * Creates an input resource for the specified path.
     *
     * @param path
     *         file path
     * @return input resource for {@code path}
     */
    static InputResource of(Path path) {
        return new PathInputResource(path);
    }

    /**
     * Creates an in-memory input resource backed by {@code bytes}.
     *
     * @param bytes
     *         byte array backing the input resource (not {@code null}), the array is <strong>not</strong> copied
     * @return input resource backed by {@code bytes}
     */
    static InputResource of(byte[] bytes) {
        return new InMemoryInputResource(bytes);
    }

    /**
     * Creates an in-memory resource with {@code value} as encoded by {@code charset}.
     * <p>
     * Using this is equivalent to using {@code of(value.getBytes(charset))}.
     * </p>
     *
     * @param value
     *         string value (not {@code null})
     * @param charset
     *         character set (not {@code null})
     * @return input resource with {@code value} as encoded by {@code charset}
     */
    static InputResource of(String value, Charset charset) {
        return of(value.getBytes(charset));
    }

    /**
     * Creates an input resource from a classpath resource resolved against this class (@code InputResource).
     *
     * @param resource
     *         absolute or relative classpath resource
     * @return input resource
     * @see #fromClasspath(Class, String)
     */
    static InputResource fromClasspath(String resource) {
        return fromClasspath(InputResource.class, resource);
    }

    /**
     * Creates an input resource from a classpath resource resolved against {@code locatorClass}.
     *
     * @param locatorClass
     *         class to resolve the classpath resource against (using {@link Class#getResource(String)})
     * @param resource
     *         absolute or relative classpath resource
     * @return input resource
     * @see #fromClasspath(String)
     */
    static InputResource fromClasspath(Class<?> locatorClass, String resource) {
        return new ClasspathInputResource(locatorClass, resource);
    }

}

/**
 * Input resource from a file.
 * <p>
 * The existence and readability of the file is verified for each {@link #newInputStream()} or
 * {@link #newReader(Charset)} invocation.
 * </p>
 *
 * @param filePath
 *         file path
 */
record PathInputResource(Path filePath) implements InputResource {

    @Override
    public InputStream newInputStream() throws IOException {
        return Files.newInputStream(filePath);
    }

    @Override
    public Reader newReader(Charset charset) throws IOException {
        return Files.newBufferedReader(filePath, charset);
    }

}

/**
 * An in-memory input resource, backed by a byte-array.
 */
final class InMemoryInputResource implements InputResource {

    private final byte[] bytes;

    /**
     * Creates an in-memory input resource backed by {@code bytes}.
     *
     * @param bytes
     *         byte array backing this instance, the array is <strong>not</strong> copied
     */
    InMemoryInputResource(byte[] bytes) {
        this.bytes = requireNonNull(bytes, "bytes");
    }

    @Override
    public InputStream newInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Reader newReader(Charset charset) {
        return new StringReader(new String(bytes, charset));
    }

}

/**
 * Input resource to read from the classpath.
 */
final class ClasspathInputResource implements InputResource {

    private final URL resourceUrl;
    private final String resourcePath;

    /**
     * Creates a classpath input resource.
     *
     * @param locatorClass
     *         class instance to use to resolve {@code resourcePath}
     * @param resourcePath
     *         absolute or relative path of the resource on the classpath
     */
    ClasspathInputResource(Class<?> locatorClass, String resourcePath) {
        this(locatorClass.getResource(resourcePath), resourcePath);
    }

    private ClasspathInputResource(URL resourceUrl, String resourcePath) {
        // NOTE: resourceUrl is null is handled on each invocation of createInputStream
        this.resourceUrl = resourceUrl;
        // Used for error messages only
        this.resourcePath = resourcePath;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        if (resourceUrl == null) {
            throw new FileNotFoundException("Could not resolve resource: " + resourcePath);
        }
        return resourceUrl.openStream();
    }

    @Override
    public String toString() {
        return "ClasspathInputResource{" +
               "resourceUrl=" + resourceUrl +
               '}';
    }

}
