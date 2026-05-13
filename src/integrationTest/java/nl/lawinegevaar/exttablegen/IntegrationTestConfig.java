// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.firebirdsql.management.FBManager;
import org.firebirdsql.testcontainers.FirebirdContainer;
import org.jspecify.annotations.NullMarked;
import org.testcontainers.containers.BindMode;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Integration test configuration (including container).
 *
 * @since 3
 */
@NullMarked
class IntegrationTestConfig implements AutoCloseable {

    private static final String DB_USER = "SYSDBA";
    private static final String DB_PASSWORD = "masterkey";
    private static final String FIREBIRD_DATA_DIR = "/var/lib/firebird/data";
    private final FirebirdContainer container;
    private final Path localDataDir;

    /**
     * Creates the integration test configuration and starts its container.
     *
     * @param localDataDir
     *         local data directory to be mapped to {@code /var/lib/firebird/data} of the container
     * @throws IllegalArgumentException
     *         if {@code localDataDir} does not exist or is not a directory
     */
    IntegrationTestConfig(Path localDataDir) {
        this.localDataDir = localDataDir.toAbsolutePath();
        if (!Files.isDirectory(localDataDir)) {
            throw new IllegalArgumentException(
                    "Path %s does not exist or is not a directory".formatted(this.localDataDir));
        }
        this.container = new FirebirdContainer(DockerImageName.parse(FirebirdContainer.PROJECT_IMAGE).withTag("5.0.4"))
                .withSysdbaPassword(DB_PASSWORD)
                .withEnv("FIREBIRD_CONF_DatabaseAccess", "Restrict " + FIREBIRD_DATA_DIR)
                .withEnv("FIREBIRD_CONF_ExternalFileAccess", "Restrict " + FIREBIRD_DATA_DIR)
                .withEnv("FIREBIRD_CONF_TcpRemoteBufferSize", "32767")
                .withFileSystemBind(this.localDataDir.toString(), FIREBIRD_DATA_DIR, BindMode.READ_WRITE);
        container.start();
    }

    FBManager createFBManager() {
        checkContainerRunning();
        var fbManager = new FBManager();
        fbManager.setServer(container.getHost());
        fbManager.setPort(mappedFirebirdPort());
        fbManager.setUserName(DB_USER);
        fbManager.setPassword(DB_PASSWORD);
        return fbManager;
    }

    private void checkContainerRunning() {
        if (!container.isRunning()) {
            throw new IllegalStateException("The container is not running. Probably it was already stopped.");
        }
    }

    /**
     * Creates a connection to the database {@code database} in the container
     *
     * @param database
     *         path of the Firebird database
     * @return connection to the database
     * @throws SQLException
     *         for errors connecting
     * @throws IllegalStateException
     *         if the container is not running
     * @see #createConnection(FirebirdTestPath, Properties)
     */
    Connection createConnection(FirebirdTestPath database) throws SQLException {
        return createConnection(database, new Properties());
    }

    /**
     * Creates a connection to the database {@code firebirdFile} in the container
     *
     * @param firebirdFile
     *         container filename or path of the Firebird database
     * @param props
     *         additional JDBC properties; the properties {@code "user"} and {@code "password"} are ignored and replaced
     *         in a copy ({@code props} is not modified)
     * @return connection to the database
     * @throws SQLException
     *         for errors connecting
     * @throws IllegalStateException
     *         if the container is not running
     * @see #createConnection(FirebirdTestPath)
     */
    Connection createConnection(FirebirdTestPath firebirdFile, Properties props) throws SQLException {
        var actualProperties = new Properties();
        actualProperties.putAll(props);
        String jdbcUrl = "jdbc:firebird://%s:%d/%s"
                .formatted(host(), mappedFirebirdPort(), firebirdFile.firebirdPath());
        actualProperties.setProperty("user", DB_USER);
        actualProperties.setProperty("password", DB_PASSWORD);
        return DriverManager.getConnection(jdbcUrl, actualProperties);
    }

    /**
     * Stops the container inside this configuration.
     */
    @Override
    public void close() {
        container.stop();
    }

    /**
     * @return local data directory that is mapped to {@code /var/lib/firebird/data} in the container.
     */
    Path localDataDir() {
        return localDataDir;
    }

    FirebirdTestPath resolveFile(String filename) {
        return resolveFile(resolveLocalFile(filename));
    }

    FirebirdTestPath resolveFile(Path localFile) {
        String relativeFile = resolveRelativeFirebirdFile(localFile);
        String firebirdFile = FIREBIRD_DATA_DIR + '/' + relativeFile;
        return new FirebirdTestPathImpl(localFile, relativeFile, firebirdFile);
    }

    /**
     * Resolve filename to an absolute path in the local data directory.
     *
     * @param filename
     *         filename (or relative path)
     * @return absolute path for {@code filename} in the local data directory
     * @throws IllegalArgumentException
     *         if {@code filename} resolves to outside the local data directory
     */
    private Path resolveLocalFile(String filename) {
        Path resolvedPath = localDataDir.resolve(filename).toAbsolutePath();
        if (!resolvedPath.startsWith(localDataDir)) {
            throw new IllegalArgumentException(
                    "Filename %s attempts to escape from the local data directory %s".formatted(filename, localDataDir));
        }
        return resolvedPath;
    }

    /**
     * Resolves a file path in the local data directory to the equivalent absolute path in
     * {@code /var/lib/firebird/data} of the container.
     *
     * @param localDataFile
     *         path of a file in the local data directory
     * @return equivalent absolute path in {@code /var/lib/firebird/data} in the container
     * @throws IllegalArgumentException
     *         if {@code localDataFile} is outside the local data directory
     * @see #resolveRelativeFirebirdFile(Path)
     */
    private String resolveFirebirdFile(Path localDataFile) {
        return FIREBIRD_DATA_DIR + '/' + resolveRelativeFirebirdFile(localDataFile);
    }

    /**
     * Resolves a file path in the local data directory to the equivalent relative path.
     * <p>
     * The path is normalized to use {@code /} as the separator, so it can be used within the (Linux-based) container
     * even if the test runs on Windows.
     * </p>
     * <p>
     * Depending on the usage, and whether this is only a filename, or also contains path segments, direct use of this
     * relative path may not always have the desired effect. In general, use of {@link #resolveFirebirdFile(Path)} is
     * recommended instead.
     * </p>
     *
     * @param localDataFile
     *         path of a file in the local data directory
     * @return equivalent relative path
     * @throws IllegalArgumentException
     *         if {@code localDataFile} is outside the local data directory
     * @see #resolveFirebirdFile(Path)
     */
    private String resolveRelativeFirebirdFile(Path localDataFile) {
        localDataFile = localDataFile.toAbsolutePath();
        if (!localDataFile.startsWith(localDataDir)) {
            throw new IllegalArgumentException(
                    "Local data file %s is not in the local data directory %s".formatted(localDataFile, localDataDir));
        }
        return localDataDir.relativize(localDataFile).toString()
                // Normalize to / if we're running on Windows
                .replace('\\', '/');
    }

    private String host() {
        checkContainerRunning();
        return container.getHost();
    }

    /**
     * Obtains the mapped Firebird port.
     *
     * @return mapped port for Firebird port 3050
     * @throws IllegalStateException
     *         if the container is not running, or if doesn't have a mapped port for port 3050
     */
    private int mappedFirebirdPort() {
        checkContainerRunning();
        Integer port = container.getMappedPort(FirebirdContainer.FIREBIRD_PORT);
        if (port == null) {
            throw new IllegalStateException(
                    "Container has no mapped port for %d".formatted(FirebirdContainer.FIREBIRD_PORT));
        }
        return port;
    }

    private record FirebirdTestPathImpl(Path localPath, String firebirdPath, String relativePath)
            implements FirebirdTestPath {
    }
}
