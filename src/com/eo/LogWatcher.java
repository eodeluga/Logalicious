package com.eo;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created by eodeluga on 21/11/18.
 */
class LogWatcher implements Runnable {

    private static boolean alreadyTriggered = false;
    private static File logFile;
    private static Level severity;
    private static WatchKey watchKey;
    private static WatchService watchService;


    static ScheduledExecutorService executor;


    private static void checkWatchEvent() {

        for (WatchEvent<?> event : watchKey.pollEvents()) {

            // Ensure a successful file watch trigger only occurs once
            if (!alreadyTriggered) {

                // Get the type of event that was triggered
                WatchEvent.Kind<?> eventKind = event.kind();

                // Skip overflow events
                if (eventKind == OVERFLOW) {
                    continue;
                }

                // Run through event types to see if watched event has been signalled
                if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY) {

                    // Found a file change event so get context to check which file it was
                    Path whichFileChanged = (Path) event.context();

                    // If it was the log file then trigger actions
                    if (whichFileChanged.toString().contains(logFile.toString())) {

                        // Get unsent log entries of selected severity
                        String logToString = LogReader.readLog(severity, false);

                        if (!logToString.isEmpty()) {
                            // Email the entries
                            LogSender.send(logToString);
                        }

                        // Set this to avoid double trigger of event
                        alreadyTriggered = true;
                    }
                }
            }
        }

        // Clean up if there is no more file to watch
        if (!watchKeyValid()) {
            unregisterWatcher();
            destroyWatchThread();
            return;
        }

        alreadyTriggered = false;
    }

    private static void createWatchThread() {

        // Create a 5 thread capable pool
        executor = Executors.newScheduledThreadPool(5);

        // Add new thread and execute every 3 secs
        executor.scheduleAtFixedRate(LogWatcher::checkWatchEvent,
                1, 3, TimeUnit.SECONDS);
    }

    private static void destroyWatchThread() {
        executor.shutdownNow();
    }

    private static boolean watchKeyValid() {
        return watchKey.reset();
    }


    static void registerWatcher(Level severity) throws IOException {

        LogWatcher.severity = severity;
        Path path;

        try {
            // Specify the watch path as the log folder
            path = LogUtility.getLogPath();
            logFile = path.getFileName().toFile();

            // Get the path without the filename
            path = LogUtility.getLogPath().getParent();

            // Start new watch service and register watch path for file modify events
            watchService = FileSystems.getDefault().newWatchService();
            watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        } catch (IOException e) {

            LogWriter.writeLog(LogWriter.SEVERE, e.toString());
            System.err.println("SEVERE: " + e.toString());
            throw new IOException(e.toString());
        }

        // Launch independent thread that watches for file changes
        createWatchThread();
    }

    static void unregisterWatcher() {

        if (executor != null) {

            try {
                destroyWatchThread();
                watchService.close();
            } catch(IOException e){

                LogWriter.writeLog(LogWriter.SEVERE, e.toString());
                System.err.println("SEVERE: " + e.toString());
            }
        }
    }


    public void run() {
    }
}