package com.eo;

import java.util.logging.Level;

/**
 * @author Created by eodeluga on 07/02/18.
 */


public class LogWriter {

    /**
     * Used to write messages to the log database
     *
     * <p>
     * <b>Class variables</b>
     * <ul>
     * <li><b>maxLogSizeKb</b> - Specify the maximum size the log database can grow before it is deleted
     * </p></ul>
     */

    public static int maxLogSizeKb = 1024;

    // Used to get respective element of dateTime array
    private static final int DATE = 0;
    private static final int TIME = 1;


    // Logging severity levels
    public static Level INFO = Level.INFO;
    public static Level SEVERE = Level.SEVERE;
    public static Level WARNING = Level.WARNING;


    /**
     * @param level The severity level of the logged message
     * @param msg The message text to log
     */
    public static void writeLog (Level level, String msg) {

        // Gets the calling class name of the method that calls this method
        String cClassName = new Exception().getStackTrace()[1].getClassName();

        // Get the date and time in a string array
        String[] dateTime = LogUtility.getDateTime();

        // Write log entry to database
        LogDatabase.insertLog(
                dateTime[TIME], dateTime[DATE], level,
                cClassName, msg);
    }
}
