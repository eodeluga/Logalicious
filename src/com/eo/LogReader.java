package com.eo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * @author Created by eodeluga on 21/11/18.
 */


public class LogReader {

    /**
     * Read log entries from the the log database
     *
     * @param  severity The minimum severity level of entries to read
     * @param sent Toggle for entries that have been emailed
     */
    public static String readLog(Level severity, Boolean sent) {

        ResultSet result = LogDatabase.getLog(severity, sent);
        StringBuilder logEntry = new StringBuilder();

        try {

            while (result != null && result.next()){

                logEntry.append(result.getString(LogDBStrings.SEVERITY_NAME));
                logEntry.append(":");
                logEntry.append(System.lineSeparator());

                // Date
                logEntry.append(result.getString(LogDBStrings.DATE));
                logEntry.append(" ");

                // Time
                logEntry.append(result.getString(LogDBStrings.TIME));
                logEntry.append(" ");

                // Exception throwing class
                logEntry.append(result.getString(LogDBStrings.CLASS));
                logEntry.append(System.lineSeparator());

                // Message
                logEntry.append(result.getString(LogDBStrings.MESSAGE));
                logEntry.append(System.lineSeparator());

                logEntry.append(System.lineSeparator());
            }

        } catch (SQLException e) {
            System.err.println("SEVERE: Cannot read log database "
                    .concat(e.toString()));
        }

        return logEntry.toString();
    }
}