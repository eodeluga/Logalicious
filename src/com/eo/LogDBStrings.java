package com.eo;

/**
 * Created by eodeluga on 16/01/19.
 */
final class LogDBStrings {

    static final String EXISTING = ";IFEXISTS=TRUE";
    static final String NO_AUTO_CLOSE = ";DB_CLOSE_ON_EXIT=FALSE";
    static final String OPEN = "jdbc:h2:";
    static final String PWD = "filepwd ";
    static final String USE_ENCRYPTION = ";CIPHER=AES";

    static final int TIME = 1;
    static final int DATE = 2;
    static final int SEVERITY = 3;
    static final int SEVERITY_NAME = 4;
    static final int CLASS = 5;
    static final int MESSAGE = 6;
    static final int SENT = 7;


    static final String CREATE_INDEX
            = "CREATE INDEX Logs_index "
            .concat("ON Logs ")
            .concat("(Severity, Sent)");

    static final String CREATE_TABLE
            = "CREATE TABLE Logs ("
                .concat("Time varchar(255), ")
                .concat("Date varchar(255), ")
                .concat("Severity int, ")
                .concat("SevName varchar(255), ")
                .concat("Class varchar(255), ")
                .concat("Message clob, ")
                .concat("Sent bit")
                .concat(")");


    static final String INSERT
            = "INSERT INTO Logs "
                .concat("(Time, Date, Severity, SevName, Class, Message, Sent)")
                .concat("VALUES (?,?,?,?,?,?,?);");

    static final String QUERY
            = "SELECT * FROM Logs "
                .concat("WHERE Severity >= ? ")
                .concat("AND Sent = ?;");

    static final String UPDATE_AS_READ
            = "UPDATE Logs "
                .concat("SET Sent = 'true' ")
                .concat("WHERE Severity >= ? ")
                .concat("AND Sent = 'false';");
}