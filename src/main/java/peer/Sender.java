package main.java.peer;

import main.java.message.MessageHandler;

import java.net.Socket;
import java.util.Map;

/*
 * Sender class represents a thread responsible for initiating connections and sending messages to peers.
 */
public class Sender extends Thread {
    private final PeerConfiguration peerConfiguration; // Configuration details for the peer
    private final MessageHandler messageHandler;       // Handler for creating and sending messages

    public Sender(PeerConfiguration peerConfiguration) {
        // Initializing message handler and peer configuration
        this.messageHandler = new MessageHandler();
        this.peerConfiguration = peerConfiguration;
    }

    // Thread run method
    @Override
    public void run() {
        // Retrieving peer data details
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();

        try {
            // Iterating through peer data to establish connections and send handshake messages
            for (int connPeerProcessId : peerIdToDataMap.keySet()) {
                if (connPeerProcessId != peerConfiguration.getPeerProcessId()) {
                    // Retrieving connection peer data
                    PeerData connPeerData = peerIdToDataMap.get(connPeerProcessId);

                    // Creating socket connection to the peer
                    Socket connection = new Socket(connPeerData.getHost(), connPeerData.getPortNumber());

                    // Sending handshake message and reading the response
                    messageHandler.createAndSendHandshake(connection, peerConfiguration);
                    messageHandler.setConnectionAndReadHandshake(connection, peerConfiguration, connPeerProcessId);
                }
            }
        } catch (Exception e) {
            // Ignoring the exception without taking any specific action
        }
    }
}
