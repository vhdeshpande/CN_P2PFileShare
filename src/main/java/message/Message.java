package main.java.message;

import main.java.peer.PeerConnection;
import main.java.peer.PeerConfiguration;
import main.java.peer.PeerData;
import main.java.constants.AppConstants;
import main.java.peer.PeerHandler;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;

/**
 * Class: Message
 * Description: Represents a threaded message handler responsible for managing communication with peers.
 * Extends: Thread
 */
public class Message extends Thread {

    // Instance variables for handling messages, peers, and configurations
    private final MessageHandler messageHandler;
    private final PeerHandler peerHandler;
    private final PeerConnection peerConnection;
    private final PeerConfiguration peerConfiguration;

    /**
     * Constructor: Message
     * Parameters:
     *   - peerConnection: Connection to a peer
     *   - peerConfiguration: Configuration details for the peer
     * Description: Initializes Message with the provided PeerConnection and PeerConfiguration.
     */
    public Message(PeerConnection peerConnection, PeerConfiguration peerConfiguration) {
        this.peerConnection = peerConnection;
        this.peerHandler = new PeerHandler(peerConnection.getPeerProcessId());
        this.messageHandler = new MessageHandler();
        this.peerConfiguration = peerConfiguration;
    }

    /**
     * Method: run
     * Description: Overrides the run method of Thread. Manages communication with peers.
     */
    @Override
    public void run() {
        synchronized (this) {
            try {
                // Retrieve peer data map and current peer's data
                Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
                PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());
                int peerProcessId = peerData.getPeerProcessId();

                // Set up input stream for peer connection
                InputStream peerConnectionStream = peerConnection.getConnection().getInputStream();
                DataInputStream dataInputStream = new DataInputStream(peerConnectionStream);

                // Construct and send bitfield message to the peer
                byte[] messageToSend = messageHandler.constructBitfieldMessage(peerData.getBitfield());
                Socket connection = peerConnection.getConnection();
                messageHandler.sendMessage(connection, messageToSend);

                // Receive and handle messages until all peers have the file
                while (peerConfiguration.getNumberOfPeerHavingFile() < peerIdToDataMap.size()) {
                    int receivedMessageLength = dataInputStream.readInt();
                    byte[] inBuff = new byte[receivedMessageLength];

                    double init = System.nanoTime();
                    dataInputStream.readFully(inBuff);
                    double end = System.nanoTime();

                    // Extract message type and content
                    MessageType messageType = MessageType.getMessageTypeFromByte((char) inBuff[0]);
                    byte[] receivedMessage = Arrays.copyOfRange(inBuff, 1, receivedMessageLength);

                    // Handle received message
                    messageHandler.handleReceivedMessage(messageType, receivedMessage, peerConfiguration, peerConnection,
                            end - init);
                }

                // Exit connection for all connected peers if the peer has downloaded the file
                int peerHasFile = peerIdToDataMap.get(peerProcessId).getHasFile();
                if (peerHasFile == AppConstants.PEER_HAS_FILE) {
                    messageHandler.notifyAllConnectionsOfExit(peerConfiguration);
                }

                // Hold peer for a message hold period and handle exit message
                peerHandler.peerHold(AppConstants.MESSAGE_HOLD_PERIOD);
                messageHandler.handleExitMessage();
            } catch (Exception e) {
                // Ignore the exception without taking any action
            }
        }
    }
}