// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.firebirdsql.management.FBManager;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@NullUnmarked
final class IntegrationTestProperties {

    static final String DB_HOST = System.getProperty("test.db.host");
    static final int DB_PORT = Integer.getInteger("test.db.port", 3050);
    static final String DB_USER = System.getProperty("test.db.user");
    static final String DB_PASSWORD = System.getProperty("test.db.password");
    static final boolean DB_ASSUME_LINUX;
    /**
     * The directory where the database exists on the server.
     * <p>
     * Configured with {@code test.db.path.prefix}; values not ending in a separator will receive a separator.
     * </p>
     */
    static final String DB_PATH_PREFIX;
    /**
     * Local path matching the same location as {@link #DB_PATH_PREFIX} (either directly, or mapped).
     * <p>
     * Configured with {@code test.db.path.local}, falling back to {@code test.db.path.prefix} (where empty is taken as
     * "current directory").
     * </p>
     */
    static final Path DB_PATH_LOCAL;
    /**
     * The directory where the external table files exist on the server.
     * <p>
     * Configured with {@code test.db.ext-table-dir.prefix}; values not ending in a separator will receive a separator.
     * </p>
     */
    static final String DB_EXT_TABLE_DIR_PREFIX;
    /**
     * Local path matching the same location as {@link #DB_EXT_TABLE_DIR_PREFIX} (either directly, or mapped).
     * <p>
     * Configured with {@code test.db.ext-table-dir.local}, falling back to {@code test.db.ext-table-dir.prefix} (where
     * empty is taken as "current directory").
     * </p>
     */
    static final Path DB_EXT_TABLE_DIR_LOCAL;
    static {
        DB_ASSUME_LINUX = !"false".equalsIgnoreCase(System.getProperty("test.db.linux"));
        String dbPathPrefix = System.getProperty("test.db.path.prefix");
        DB_PATH_PREFIX = normalizePrefix(dbPathPrefix);
        String dbPathLocal = System.getProperty("test.db.path.local");
        DB_PATH_LOCAL = dbPathLocal != null ? Path.of(dbPathLocal).toAbsolutePath() : prefixAsLocalPath(dbPathPrefix);
        String dbExtTableDirPrefix = System.getProperty("test.db.ext-table-dir.prefix");
        DB_EXT_TABLE_DIR_PREFIX = normalizePrefix(dbExtTableDirPrefix);
        String dbExtTableDirLocal = System.getProperty("test.db.ext-table-dir.local");
        DB_EXT_TABLE_DIR_LOCAL = dbExtTableDirLocal != null ? Path.of(dbExtTableDirLocal).toAbsolutePath()
                : prefixAsLocalPath(dbExtTableDirPrefix);
    }

    private IntegrationTestProperties() {
        // no instances
    }

    /**
     * @return {@code true} if the configuration is complete (i.e. provides all necessary information for the test to
     * run).
     */
    static boolean isCompleteConfiguration() {
        return DB_HOST != null && DB_USER != null && DB_PASSWORD != null;
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
     *         local database path
     * @return connection to the database
     * @throws SQLException
     *         if the connection can not be made
     * @throws IllegalArgumentException
     *         if {@code databasePath} is not within {@link #DB_PATH_LOCAL}
     */
    static Connection createConnection(Path databasePath) throws SQLException {
        String databaseFirebirdFile = databaseFirebirdFile(databasePath);
        String jdbcUrl = "jdbc:firebird://%s:%d/%s".formatted(DB_HOST, DB_PORT, databaseFirebirdFile);
        return DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD);
    }

    /**
     * Resolves {@code databaseName} against {@link #DB_PATH_LOCAL}.
     *
     * @param databaseName
     *         database name
     * @return resolved path
     * @throws IllegalArgumentException
     *         if {@code databaseName} tries to escape {@link #DB_PATH_LOCAL}
     */
    static Path databasePath(String databaseName) {
        return createPath(DB_PATH_LOCAL, databaseName);
    }

    /**
     * Resolves {@code externalTableFilename} against {@link #DB_EXT_TABLE_DIR_LOCAL}.
     *
     * @param externalTableFilename
     *         external table filename
     * @return resolved path
     * @throws IllegalArgumentException
     *         if {@code externalTableFilename} tries to escape {@link #DB_EXT_TABLE_DIR_LOCAL}
     */
    static Path externalTableFile(String externalTableFilename) {
        return createPath(DB_EXT_TABLE_DIR_LOCAL, externalTableFilename);
    }

    /**
     * Transforms a local external table file path in {@link #DB_EXT_TABLE_DIR_LOCAL} to the Firebird-side equivalent
     * path rooted in {@link #DB_EXT_TABLE_DIR_PREFIX}.
     *
     * @param externalTablePath
     *         local path of the external table file
     * @return String form of the equivalent path in {@link #DB_EXT_TABLE_DIR_PREFIX}
     * @throws IllegalArgumentException
     *         if {@code externalTablePath} is not rooted in {@link #DB_EXT_TABLE_DIR_LOCAL}
     */
    static String externalTableFirebirdFile(Path externalTablePath) {
        return toFirebirdPath(DB_EXT_TABLE_DIR_PREFIX, DB_EXT_TABLE_DIR_LOCAL, externalTablePath);
    }

    /**
     * Transforms a local database file path in {@link #DB_PATH_LOCAL} to the Firebird-side equivalent
     * path rooted in {@link #DB_PATH_PREFIX}.
     *
     * @param databasePath
     *         local path of the external table file
     * @return String form of the equivalent path in {@link #DB_PATH_PREFIX}
     * @throws IllegalArgumentException
     *         if {@code databasePath} is not rooted in {@link #DB_PATH_LOCAL}
     */
    static String databaseFirebirdFile(Path databasePath) {
        return toFirebirdPath(DB_PATH_PREFIX, DB_PATH_LOCAL, databasePath);
    }

    private static Path createPath(Path root, String fileName) {
        Path path = root.resolve(fileName).toAbsolutePath();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(
                    "%s tries to escape from configured directory %s".formatted(fileName, root));
        }
        return path;
    }

    private static String normalizePrefix(@Nullable String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        } else if (isSeparator(prefix.charAt(prefix.length() - 1))) {
            return prefix;
        } else {
            return prefix + "/";
        }
    }

    private static Path prefixAsLocalPath(@Nullable String prefix) {
        Path localPath;
        if (prefix == null || prefix.isBlank()) {
            localPath = Path.of(".");
        } else {
            localPath = Path.of(prefix);
        }
        return localPath.toAbsolutePath();
    }

    private static boolean isSeparator(char ch) {
        return ch == '\\' || ch == '/';
    }

    private static String toFirebirdPath(String prefix, Path root, Path path) {
        path = path.toAbsolutePath();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("path %s must be rooted in %s"
                    .formatted(path, root));
        }
        return toFirebirdPath(prefix, root.relativize(path));
    }

    /**
     * Creates a Firebird-side path rooted in {@code prefix}.
     *
     * @param prefix
     *         prefix of the Firebird-side path (can be blank)
     * @param path
     *         relative path of the file
     * @return String form of {@code path} rooted in {@code prefix}
     * @throws IllegalArgumentException
     *         when {@code path} is absolute
     */
    private static String toFirebirdPath(String prefix, Path path) {
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Path %s should be a relative path".formatted(path));
        }
        String pathString = prefix + path;
        if (DB_ASSUME_LINUX) {
            return pathString.replace('\\', '/');
        }
        return pathString;
    }

}
