package com.eo;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.logging.Level;

/**
 * Created by eodeluga on 23/12/18.
 */
class LogDatabase {

    private static Connection conn = null;
    private static PreparedStatement statement;
    // Change size from Kb to bytes
    private static final int MAX_SIZE = LogWriter.maxLogSizeKb * 1024;

    private static void checkDatabaseSize() {

        try {
            // Check for existing log db file
            if (LogUtility.getLogFileExists()) {

                // Check existing log db size against max size
                long fSize;
                fSize = LogUtility.getLogFile().length();

                // Check if size exceeds max
                if (fSize >= MAX_SIZE) {

                    closeDatabase();

                    // Delete log as it exceeds max size
                    LogUtility.deleteLog();
                }
            }
        } catch (IOException e) {

            System.err.println("SEVERE: Cannot check database size "
                    .concat(e.toString()));
        }
    }

    private static boolean createDatabase() throws SQLException {

        // Connection strings
        StringBuilder url;

        // Need to remove extension from file name
        String logFileName =
                LogUtility.logPath.toString().replace(".mv.db", "");

        url = new StringBuilder()
                .append(LogDBStrings.OPEN)
                .append(logFileName)
                .append(LogDBStrings.NO_AUTO_CLOSE)
                .append(LogDBStrings.USE_ENCRYPTION);

        // Execute connection URL to open existing database
        Connection connection = executeSql(url);

        // Return status of connection validity
        return (connection != null && connection.isValid(1000));
    }

    private static void createSchema() throws SQLException {

        // Construct and execute SQL statements
        Statement statement = conn.createStatement();
        statement.execute(LogDBStrings.CREATE_TABLE);
        statement.execute(LogDBStrings.CREATE_INDEX);
    }

    private static Connection executeSql(StringBuilder connectionUrl)
            throws SQLException {
        // Executes the operation to open existing or create new log database

        String user = "logalicious";
        String lpath = LogUtility.logPath.toString();
        char[] seed = lpath.toCharArray();
        String dbConnUrl;
        String password;
        char[] lpass;

        dbConnUrl = connectionUrl.toString();

        dbConnUrl = dbConnUrl.replace(".mv.db","");

        try {
            // Generate password for log Db
            lpass = LogUtility.generatePassword(seed);
        } catch (NoSuchAlgorithmException e) { return null;}


        // Create password statement from generated char array password
        password = LogDBStrings.PWD.concat(String.valueOf(lpass));

        // Execute the url to open existing database
        conn = DriverManager.getConnection(dbConnUrl, user, password);
        return conn;
    }

    private static void initialise() throws IOException {

        // Install JVM shutdown hook to cleanup on unexpected shutdown
        LogUtility.hookJvmShutdown();

        // Check for existing log db file at path
        if (!LogUtility.getLogFileExists()) {

            try {
                // No existing db so create one
                if (createDatabase()) {
                    // If successful create schema
                    createSchema();

                } else throw new IOException("SEVERE: Cannot create new database");

            } catch (SQLException e) {
                throw new IOException(e.toString());
            }

        } else {
            // There is an existing db so try opening it
            try {
                if (!openDatabase()) {
                    // Database could not be opened due to connection issue not file
                    throw new IOException("SEVERE: Cannot open connection to existing database");
                }
            } catch (SQLException e) {
                // There is a problem with the database so delete and create new one
                if (LogUtility.deleteLog()) {
                    // Deletion successful so try creating new Db
                    try {
                        // Check creation was successful
                        if (createDatabase()) {
                            createSchema();

                        } else throw new IOException("SEVERE: Cannot create new database");

                    } catch (SQLException err) {
                        throw new IOException(err.toString());
                    }
                } else throw new IOException("SEVERE: Cannot delete existing database");
            }
        }
    }

    private static boolean openDatabase() throws SQLException {

        // Connection strings
        StringBuilder url;

        // Need to remove extension from file name
        String logFileName = LogUtility.logPath.toString();

        url = new StringBuilder()
                .append(LogDBStrings.OPEN)
                .append(logFileName)
                .append(LogDBStrings.EXISTING)
                .append(LogDBStrings.NO_AUTO_CLOSE)
                .append(LogDBStrings.USE_ENCRYPTION);

        // Execute connection URL to open existing database
        Connection connection = executeSql(url);

        // Return status of connection validity
        return (connection != null && connection.isValid(1000));
    }

    private static void openDatabaseConnection() {

        if (conn == null) {
            try {

                LogDatabase.initialise();

            } catch (IOException e) {

                System.err.println("SEVERE: Cannot write to database "
                        .concat(e.toString()));
            }
        }
    }

    private static ResultSet queryLog(int severity, boolean isSent) throws SQLException {

        ResultSet result;

        // Initialise SQL query to retrieve log entries
        statement = conn.prepareStatement(LogDBStrings.QUERY);

        // Insert query parameters into statement
        statement.setInt(1, severity);
        statement.setBoolean(2, isSent);

        // Execute prepared SQL statement
        result = statement.executeQuery();
        return result;
    }


    static void closeDatabase() {

        // Close any existing connection
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {

                System.err.println("SEVERE: Cannot close existing database connection "
                        .concat(e.toString()));
            }
            // Reset connection
            conn = null;
        }
    }

    static void insertLog(
            String time, String date, Level severity,
            String callingClass, String message) {

        // Check if an existing log db exceeds max size and delete if so
        checkDatabaseSize();
        openDatabaseConnection();

        try {
            // Initialise SQL statement to insert log records
            statement = conn.prepareStatement(LogDBStrings.INSERT);

            // Insert parameters into statement
            statement.setString(LogDBStrings.TIME, time);
            statement.setString(LogDBStrings.DATE, date);
            statement.setInt(LogDBStrings.SEVERITY, severity.intValue());
            statement.setString(LogDBStrings.SEVERITY_NAME, severity.toString());
            statement.setString(LogDBStrings.CLASS, callingClass);
            statement.setString(LogDBStrings.MESSAGE, message);
            // Set whether the log has been sent
            statement.setBoolean(LogDBStrings.SENT, false);

            // Execute the statement
            int numInsertedRecords = statement.executeUpdate();

            if (numInsertedRecords < 1) {
                // The statement was not executed
                // as inserted records are less than one
                throw new SQLException();
            }

        } catch (SQLException e) {
            // The statement wasn't loaded
            System.err.println("SEVERE: Cannot execute SQL statement "
                    .concat(e.toString()));
        }
    }

    static void markAsSent(Level severity) {

        openDatabaseConnection();

        try {
            // Initialise SQL query to retrieve and mark log records as read
            statement = conn.prepareStatement(LogDBStrings.UPDATE_AS_READ);
            statement.setInt(1,severity.intValue());
            statement.executeUpdate();

        } catch (SQLException e) {
            // The statement wasn't loaded
            System.err.println("SEVERE: Cannot update database "
                    .concat(e.toString()));
        }
    }

    static ResultSet getLog(Level severity, boolean isSent) {

        // Run query to retrieve log entries
        openDatabaseConnection();

        try {
            return queryLog(severity.intValue(), isSent);

        } catch (SQLException e) {

            // The statement wasn't loaded
            System.err.println("SEVERE: Cannot prepare SQL statement "
                    .concat(e.toString()));
            return null;
        }
    }
}
