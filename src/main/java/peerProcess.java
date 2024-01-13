package main.java;

import main.java.constants.DisplayConstants;
import main.java.log.LogHandler;
import main.java.peer.PeerHandler;

import java.io.IOException;

/*
 * The main class 'peerProcess' initializes and starts the peer process.
 */
public class peerProcess {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Retrieving peer process ID from command line arguments
        int peerProcessId = Integer.parseInt(args[0]);

        // Initializing log handler and peer handler
        LogHandler logHandler = new LogHandler(peerProcessId);
        PeerHandler peerHandler = new PeerHandler(peerProcessId);

        // Displaying initialization message
        logHandler.printMessage(String.format(DisplayConstants.INIT_PEER_MESSAGE, peerProcessId));

        // Setting up and starting the peer process
        peerHandler.setupAndStartPeerProcess(peerProcessId);
    }
}
