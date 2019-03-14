package com.eo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by eodeluga on 16/12/18.
 */
class LogUtility {

    private static boolean jvmHookInstalled = false;
    static Path logPath;


    private static char[] byteToCharArray(byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(byte b: bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String s = sb.toString();

        eraseArray(bytes);
        return s.toCharArray();
    }

    private static void eraseArray(byte[] bytes) {

        int len = bytes.length;
        // Fill entire length of byte array with zeros
        for (int i = 0; i < len; i++) {
            bytes[i] = '0';
        }
    }

    private static void eraseArray(char[] chars) {

        int len = chars.length;
        // Fill entire length of char array with zeros
        for (int i = 0; i < len; i++) {
            chars[i] = '0';
        }
    }

    private static void TTFO() {

        // Jvm is quitting so clean up
        LogWatcher.unregisterWatcher();
        LogDatabase.closeDatabase();
    }


    static boolean deleteLog() {
        return new File(logPath.toString()).delete();
    }

    static char[] generatePassword(char[] keySeed) throws NoSuchAlgorithmException {

        MessageDigest md;

        // Set hashing complexity
        md = MessageDigest.getInstance("SHA-512");

        // Calculate key by digesting the key seed converted to string
        byte[] key = md.digest(new String(keySeed).getBytes());
        // Overwrite variable for security
        eraseArray(keySeed);

        // Return the encryption key as CharArray
        return byteToCharArray(key);
    }

    static String[] getDateTime() {

        LocalDateTime now = LocalDateTime.now();

        // Return time and date as a String array
        String[] timeDate;
        timeDate = (now.format(DateTimeFormatter.ofPattern("dd MMM yyyy.HH:mm:ss"))).split("\\.");

        return timeDate;
    }

    static File getLogFile() throws IOException {

        logPath = getLogPath();
        String logFilePath = logPath.toString();
        return new File(logFilePath);
    }

    static boolean getLogFileExists() throws IOException { return getLogFile().exists(); }

    static Path getLogPath() throws IOException {

        Path logPath;

        // Get execution path
        logPath = new File(".").getCanonicalFile().toPath();

        // Append log file location
        logPath = (logPath.resolve("log")).resolve("log.mv.db");

        return logPath;
    }

    static void hookJvmShutdown() {
        // Install a hook in the Jvm shutdown mechanism
        // to clean up resources when shutdown occurs

        if (!jvmHookInstalled) {

            Runtime.getRuntime().addShutdownHook(new Thread(LogUtility::TTFO));

            jvmHookInstalled = true;
        }
    }
}
