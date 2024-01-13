package main.java.log;

import main.java.constants.AppConstants;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class: LogHandler
 * Description: Handles logging functionality for the application.
 */
public class LogHandler {

    // Instance variable for date-time formatting
    private final SimpleDateFormat dateTimeStamp;

    // PrintWriter instance to write logs to a file
    private PrintWriter printWriter;

    /**
     * Constructor: LogHandler
     * Parameters:
     *   - peerProcessId: ID of the peer for which logging is performed
     * Description: Initializes the LogHandler with the specified peerProcessId.
     *              Creates a log file and prepares PrintWriter for writing logs.
     * Throws: IOException if there is an issue with file operations.
     */
    public LogHandler(int peerProcessId) throws IOException {
        dateTimeStamp = new SimpleDateFormat(AppConstants.LOG_TIME_FORMAT);
        String logFilePath = String.format(AppConstants.LOG_FILE_PATH, System.getProperty(AppConstants.USER_DIR), File.separator, peerProcessId);
        printWriter = new PrintWriter(logFilePath);
        printWriter.flush();
    }

    /**
     * Method: printMessageAndLogMessageToFile
     * Parameters:
     *   - message: Message to be printed and logged
     * Description: Prints the formatted message to the console and logs it to the file.
     */
    public void printMessageAndLogMessageToFile(String message){
        Date time = new Date();
        String messageToLog = String.format("%s : %s\n", dateTimeStamp.format(time), message);
        printMessage(messageToLog);
        logMessageToFile(messageToLog);
    }

    /**
     * Method: printMessage
     * Parameters:
     *   - message: Message to be printed
     * Description: Prints the message to the console.
     */
    public void printMessage(String message){
        System.out.println(message);
    }

    /**
     * Method: logMessageToFile
     * Parameters:
     *   - message: Message to be logged to the file
     * Description: Writes the formatted message to the log file and flushes the PrintWriter.
     */
    public void logMessageToFile(String message){
        printWriter.printf(message);
        printWriter.flush();
    }

}