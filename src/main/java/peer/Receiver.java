package main.java.peer;

import main.java.message.MessageHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/*
 * Receiver class represents a thread responsible for handling incoming connections from peers.
 */
public class Receiver extends Thread {
    private final PeerConfiguration peerConfiguration; // Configuration details for the peer
    private final MessageHandler messageHandler;       // Handler for processing incoming messages

    public Receiver(PeerConfiguration peerConfiguration) {
        // Initializing message handler and peer configuration
        messageHandler = new MessageHandler();
        this.peerConfiguration = peerConfiguration;
    }

    // Thread run method
    @Override
    public void run() {
        // Retrieving peer data and connection details
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        PeerData peerData = peerIdToDataMap.get(peerConfiguration.getPeerProcessId());
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        try {
            // Setting up server socket for incoming connections
            int portNumber = peerData.getPortNumber();
            int numberOfExpectedConnections = peerIdToDataMap.size() - 1;
            int numberOfCurrentConnections = peerIdToConnectionsMap.size();
            ServerSocket serverSocket = new ServerSocket(portNumber);

            // Accepting incoming connections until the expected number is reached
            while (numberOfCurrentConnections < numberOfExpectedConnections) {
                Socket connection = serverSocket.accept();
                messageHandler.acceptConnectionAndReadHandshake(connection, peerConfiguration);
            }
        } catch (Exception e) {
            // Ignoring the exception without taking any specific action
        }
    }
}
