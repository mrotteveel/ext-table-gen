// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

/**
 * Interface to obtain an output stream to write data.
 */
@FunctionalInterface
interface OutputResource {

    /**
     * Creates an output stream for output.
     * <p>
     * Generally, the output stream should be new and start at the beginning, but implementations are allowed to deviate
     * from that (for example for composite response, etc.). However, implementations need to take into account that
     * the caller will close the returned output stream when done (e.g. by using some kind of close-shield if needed).
     * </p>
     * <p>
     * In general, the returned output stream will be unbuffered (this does not disallow using in-memory destinations or
     * buffering, but is intended to set expectations), and callers are responsible to apply buffering, if needed.
     * </p>
     * <p>
     * If the input stream is backed by a stateful target (e.g. a file), then it is implementation-defined whether
     * this method allows creation of a new input stream and resetting the contents of the target (e.g.
     * equivalent of {@link java.nio.file.StandardOpenOption#TRUNCATE_EXISTING}), allows appending, or throws
     * an {@code IOException}.
     * </p>
     *
     * @return new open output stream
     * @throws IOException
     *         if the output stream cannot be created, for example if the target file already exists and overwriting is
     *         not allowed
     */
    OutputStream newOutputStream() throws IOException;

    /**
     * Path of the output resource, if available.
     * <p>
     * NOTE: This path does not need to exist, nor does it need to be valid on the current host computer. Callers
     * should not make any assumptions if the path is usable. It is intended to be informational, e.g. to include
     * a path in the generated {@code CREATE TABLE} statement, if needed.
     * </p>
     *
     * @return path or empty if this output resource has no path information
     */
    default Optional<Path> path() {
        return Optional.empty();
    }

    /**
     * Does this output resource allow overwriting.
     * <p>
     * The default implementation returns {@code false}.
     * </p>
     *
     * @return {@code true} when this output resource allows overwriting existing data
     */
    default boolean allowOverwrite() {
        return false;
    }

    /**
     * Creates an output resource to {@code filePath}, disallowing overwrite.
     * <p>
     * Equivalent to {@code of(filePath, false)}.
     * </p>
     *
     * @param filePath
     *         path of the file
     * @return output resource to {@code filePath}, disallowing overwrite
     */
    static OutputResource of(Path filePath) {
        return of(filePath, false);
    }

    /**
     * Creates an output resource to {@code filePath}.
     * <p>
     * Equivalent to {@code of(filePath, false)}.
     * </p>
     *
     * @param filePath
     *         path of the file
     * @param allowOverwrite
     *         {@code true} allow overwrite, {@code false} disallow overwrite
     * @return output resource to {@code filePath}
     */
    static OutputResource of(Path filePath, boolean allowOverwrite) {
        return new PathOutputResource(filePath, allowOverwrite);
    }

    /**
     * Creates a single-use output resource wrapping {@code out}.
     * <p>
     * The method {@link #newOutputStream()} can be called only once. Subsequent invocations throw
     * a {@code IOException}.
     * </p>
     *
     * @param out
     *         output stream
     * @return single-use output resource wrapping {@code out}
     */
    static OutputResource of(OutputStream out) {
        return new OutputStreamResource(out);
    }

    /**
     * Returns a null output resource, which will always throw an {@code IOException} for {@link #newOutputStream()}.
     * <p>
     * Equivalent to {@code OutputResource.nullOutputResource(null)}
     * </p>
     *
     * @return null output resource without a path (method may return the same instance for repeated invocations)
     */
    static OutputResource nullOutputResource() {
        return nullOutputResource(null);
    }

    /**
     * Returns a null output resource, which will always throw an {@code IOException} for {@link #newOutputStream()}.
     *
     * @param path
     *         path to report for {@link #path()} (can be {@code null} to report empty)
     * @return null output resource with or without a path (method may return the same instance for repeated
     * invocations; in the current implementation, this only applies when {@code path} is {@code null})
     */
    static OutputResource nullOutputResource(@Nullable Path path) {
        final class NullOutputResource implements OutputResource {

            static final NullOutputResource NULL_INSTANCE = new NullOutputResource(null);

            private final @Nullable Path path;

            NullOutputResource(@Nullable Path path) {
                this.path = path;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                throw new IOException("cannot create output stream for NullOutputResource");
            }

            @Override
            public Optional<Path> path() {
                return Optional.ofNullable(path);
            }

        }
        return path != null ? new NullOutputResource(path) : NullOutputResource.NULL_INSTANCE;
    }

    /**
     * Returns an output resource which decorates the output stream returned by {@code original} using
     * {@code decorator}.
     *
     * @param original
     *         original output resource
     * @param decorator
     *         decorator to wrap or otherwise modify the output stream returned from {@code original.newOutputStream()}
     * @return output resource applying {@code decorator}
     */
    static OutputResource decorate(OutputResource original, UnaryOperator<OutputStream> decorator) {
        record DecoratingOutputResource(OutputResource original, UnaryOperator<OutputStream> decorator)
                implements OutputResource {

            DecoratingOutputResource {
                requireNonNull(original, "original");
                requireNonNull(decorator, "decorator");
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                return decorator.apply(original.newOutputStream());
            }

            @Override
            public Optional<Path> path() {
                return original.path();
            }

            @Override
            public boolean allowOverwrite() {
                return original.allowOverwrite();
            }
        }

        return new DecoratingOutputResource(original, decorator);
    }

}

/**
 * Output resource to a file.
 * <p>
 * The (non-)existence of the file and whether it can be created is verified for each {@link #newOutputStream()}
 * invocation.
 * </p>
 *
 * @param filePath
 *         path of the file
 * @param allowOverwrite
 *         {@code true} allow overwrite, {@code false} disallow overwrite
 */
record PathOutputResource(Path filePath, boolean allowOverwrite) implements OutputResource {

    PathOutputResource {
        requireNonNull(filePath, "filePath");
    }

    /**
     * Creates a new output stream to write {@link #filePath()} from the beginning.
     *
     * @return new output stream
     * @throws IOException
     *         if the file cannot be created (e.g. invalid path, insufficient filesystem permissions), or
     *         if {@link #allowOverwrite()} is {@code false} and the file already exists
     */
    @Override
    public OutputStream newOutputStream() throws IOException {
        return Files.newOutputStream(filePath, createOpenOptions());
    }

    @Override
    public Optional<Path> path() {
        return Optional.of(filePath);
    }

    private OpenOption[] createOpenOptions() {
        return allowOverwrite
                ? new OpenOption[] { CREATE, TRUNCATE_EXISTING, WRITE }
                : new OpenOption[] { CREATE_NEW, WRITE };
    }

}

/**
 * A single-use output resource wrapping an output stream.
 */
final class OutputStreamResource implements OutputResource {

    private @Nullable OutputStream out;

    /**
     * Creates an output stream resource.
     *
     * @param out
     *         the stream to wrap
     */
    OutputStreamResource(OutputStream out) {
        this.out = out;
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        OutputStream out = this.out;
        if (out == null) {
            throw new IOException(
                    "OutputStreamOutputResource is not reusable and newOutputStream() has already been called");
        }
        this.out = null;
        return out;
    }

}
