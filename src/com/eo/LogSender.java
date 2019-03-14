package com.eo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author Created by eodeluga on 25/10/18.
 * @version %I, %G
 */


public class LogSender {

    /**
     * LogSender is used to start / stop the automatic log sending service that emails
     * log entries (created by LogWriter) at a interval rate schedule. All mandatory class
     * variables must be set otherwise a NullPointerException is thrown.
     *
     * <p>
     * <b>Mandatory class variables</b>
     * <ul>
     * <li><b>host</b> - SMTP server host address
     * <li><b>username</b> - Username <b>(only when using TLS)</b>
     * <li><b>password</b> - Password <b>(only when using TLS)</b>
     * <li><b>from</b> - Sender email address
     * <li><b>subject</b> - Subject line of log entry emails
     * <li><b>to</b> - Recipient email address
     * </ul></p>
     *
     * <p>
     * <b>Optional class variables with defaults set</b>
     * <ul>
     * <li><b>severity</b> Minimum severity level of log entries to send - <b>Default:</b> WARNING
     * <li><b>interval</b> Specifies how often new log entries are sent (15 secs minimum) - <b>Default:</b> 60
     * <li><b>port</b> SMTP server port for sending mail - <b>Default:</b> 587 (TLS)
     * <li><b>useTLS</b> Switch to use TLS encrypted mail - <b>Default:</b> True
     * </p></ul>
     */

    // Mandatory parameters
    public static String host;
    public static String username;
    public static String password;
    public static String from;
    public static String subject;
    public static String to;

    // Optional parameters with defaults set
    public static Level severity = Level.WARNING;
    public static int interval = 60;
    public static int port = 587;
    public static boolean useTLS = true;

    private static boolean isRunning = false;
    private static final StringBuilder MSG_BUFFER = new StringBuilder();

    private static Authenticator getAuthenticator() {

        return new Authenticator() {
            // Override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,password);
            }
        };
    }

    private static void sendMail() {

        // Have to synchronise msgBuffer as could be modified in send()
        synchronized (MSG_BUFFER) {

            if (MSG_BUFFER.length() > 0) {

                String message = MSG_BUFFER.toString();

                // Initialise the mail session
                Session session = setupSession();

                try {
                    // Instantiate a message
                    Message msg = new MimeMessage(session);

                    // Set message attributes
                    msg.setFrom(new InternetAddress(from));
                    InternetAddress[] address = InternetAddress.parse(to);
                    msg.setRecipients(Message.RecipientType.TO, address);
                    msg.setSubject(subject);
                    msg.setSentDate(new Date());

                    // Set message content
                    msg.setText(message);

                    //Send the message
                    Transport.send(msg);

                    // Mark selected log entries as sent
                    LogDatabase.markAsSent(severity);

                    // Clear down the message buffer
                    MSG_BUFFER.setLength(0);

                } catch (MessagingException e) {
                    LogWriter.writeLog(LogWriter.SEVERE, e.toString());
                    System.err.println("SEVERE: " + e.toString());

                    // Clear down the message buffer
                    MSG_BUFFER.setLength(0);
                }
            }
        }
    }

    private static Session setupSession() {

        // Set properties and setup mail session
        Session session;
        Properties props = new Properties();

        // If using static Transport.send(),
        // need to specify which host to send it to
        if (useTLS) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.port", port);
        }

        props.put("mail.smtp.host", host);

        // Get an authenticated or plain text mail session
        if (useTLS) {

            // Get an authenticated SMTP session
            session = Session.getDefaultInstance(props, getAuthenticator());

        } else {

            // Plain text SMTP session so check if a port has been specified
            if (port == 587) {
                // Port is still set for TLS so was not specified
                // Setting port to default SMTP
                port = 25;
            }

            props.put("mail.smtp.port", port);
            session = Session.getInstance(props);
        }

        return session;
    }

    /**
     * @throws NullPointerException Thrown if some email fields are not specified
     */
    private static void validateClassFields() {
        // Checks that no public parameter of the class is unspecified  / null

        Field[] fields = LogSender.class.getFields();
        for (Field field : fields) {
            // Check each parameter in turn and throw exception if null
            try {

                if (field.get(field) == null) {

                    // Ignore username and password fields if not using TLS
                    if (!useTLS) {
                        String fName = field.getName();
                        if (fName.equals("username")
                                || fName.equals("password")) {
                            continue;
                        }
                    }

                    LogWriter.writeLog(LogWriter.SEVERE, field.getName() + " is null");
                    System.err.println("SEVERE: " + field.getName() + " is null");
                    throw new NullPointerException();
                }

            } catch (IllegalAccessException e) {

                LogWriter.writeLog(LogWriter.SEVERE, e.toString());
                System.err.println("SEVERE: " + e.toString());
                throw new NullPointerException();
            }
        }
    }


    static void send(String message) {

        // Have to synchronise msgBuffer as could be accessed in sendMail()
        synchronized (MSG_BUFFER) {

            // Add logged event message to buffer
            MSG_BUFFER.append(message);
        }
    }

    /**
     * Starts the service to send email log entries
     */
    public static void startService() {

        if (isRunning) {

            System.err.println("Service already running");
            return;
        }

        try {

            // Check all email variables have been set
            validateClassFields();

        } catch (NullPointerException e) {

            LogWriter.writeLog(LogWriter.WARNING, e.toString());
            System.err.println("WARNING: Cannot start log sender service: " + e.toString());
            return;
        }

        // Start log db file watcher service
        try {
            LogWatcher.registerWatcher(severity);

            // Check watcher is running
            if (LogWatcher.executor != null) {

                // Override email sending interval if set too low
                if (interval < 15) {
                    interval = 15;}

                // Add new thread to run sendMail at class default or class variable specified interval
                // and start execution to begin after initial 30 sec delay
                LogWatcher.executor
                        .scheduleAtFixedRate(
                                LogSender::sendMail, interval, interval, TimeUnit.SECONDS);

                isRunning = true;
            }

        } catch (IOException e) {
            LogWriter.writeLog(Level.WARNING, e.toString());
            System.err.println("WARNING: " + e.toString());

        }
    }

    /**
     * Stops the service to send email log entries and closes all resources
     */
    public static void stopService() {

        // Stop all threads and unregister watcher service
        LogWatcher.unregisterWatcher();

        isRunning = false;
    }

}