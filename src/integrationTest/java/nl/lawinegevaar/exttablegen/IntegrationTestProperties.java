// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.firebirdsql.management.FBManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class IntegrationTestProperties {

    static final String DB_HOST = System.getProperty("test.db.host");
    static final int DB_PORT = Integer.getInteger("test.db.port", 3050);
    static final String DB_USER = System.getProperty("test.db.user");
    static final String DB_PASSWORD = System.getProperty("test.db.password");
    static final Path DB_ROOT_PATH;
    static final Path DB_EXT_TABLE_DIR;
    static {
        String dbRootPath = System.getProperty("test.db.root-path");
        DB_ROOT_PATH = dbRootPath != null ? Path.of(dbRootPath).toAbsolutePath() : null;
        String extTableDir = System.getProperty("test.db.ext-table-dir");
        DB_EXT_TABLE_DIR = extTableDir != null ? Path.of(extTableDir).toAbsolutePath() : null;
    }

    private IntegrationTestProperties() {
        // no instances
    }

    /**
     * @return {@code true} if the configuration is complete (i.e. provides all necessary information for the test to
     * run).
     */
    static boolean isCompleteConfiguration() {
        return DB_HOST != null && DB_USER != null && DB_PASSWORD != null && DB_ROOT_PATH != null
               && DB_EXT_TABLE_DIR != null;
    }

    static FBManager createFBManager() {
        if (!isCompleteConfiguration()) {
            throw new IllegalStateException("Incomplete integration test configuration");
        }
        var fbManager = new FBManager();
        fbManager.setServer(DB_HOST);
        fbManager.setPort(DB_PORT);
        fbManager.setUserName(DB_USER);
        fbManager.setPassword(DB_PASSWORD);
        return fbManager;
    }

    /**
     * Creates a connection to the specified database name.
     *
     * @param databasePath
     *         database path
     * @return connection to the database
     * @throws SQLException
     *         if the connection can not be made
     * @throws IllegalStateException
     *         if system property {@code test.db.root-path} is not set
     * @throws IllegalArgumentException
     *         if {@code databasePath} is not within {@link #DB_ROOT_PATH}
     */

    static Connection createConnection(Path databasePath) throws SQLException {
        if (!databasePath.toAbsolutePath().startsWith(requireRootPath())) {
            throw new IllegalArgumentException("databasePath %s must be rooted in %s"
                    .formatted(databasePath, DB_ROOT_PATH));
        }
        String jdbcUrl = "jdbc:firebird://%s:%d/%s".formatted(DB_HOST, DB_PORT, databasePath);
        return DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD);
    }

    /**
     * Resolves {@code databaseName} against {@link #DB_ROOT_PATH}.
     *
     * @param databaseName
     *         database name
     * @return resolved path
     * @throws IllegalStateException
     *         if system property {@code test.db.root-path} is not set
     * @throws IllegalArgumentException
     *         if {@code databaseName} tries to escape {@link #DB_ROOT_PATH}
     */
    static Path databasePath(String databaseName) {
        return createPath(requireRootPath(), databaseName);
    }

    /**
     * Resolves {@code externalTableFilename} against {@link #DB_EXT_TABLE_DIR}.
     *
     * @param externalTableFilename
     *         external table filename
     * @return resolved path
     * @throws IllegalStateException
     *         if system property {@code test.db.ext-table-dir} is not set
     * @throws IllegalArgumentException
     *         if {@code externalTableFilename} tries to escape {@link #DB_EXT_TABLE_DIR}
     */
    static Path externalTableFile(String externalTableFilename) {
        return createPath(requireExternalTableDir(), externalTableFilename);
    }

    private static Path createPath(Path root, String fileName) {
        Path path = root.resolve(fileName).toAbsolutePath();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(
                    "%s tries to escape from configured directory %s".formatted(fileName, root));
        }
        return path;
    }

    private static Path requireRootPath() {
        return requirePath(DB_ROOT_PATH, "test.db.root-path");
    }

    private static Path requireExternalTableDir() {
        return requirePath(DB_EXT_TABLE_DIR, "test.db.ext-table-dir");
    }

    private static Path requirePath(Path path, String systemProperty) {
        if (path == null) {
            throw new IllegalStateException("System property %s is not set".formatted(systemProperty));
        }
        return path;
    }

}
