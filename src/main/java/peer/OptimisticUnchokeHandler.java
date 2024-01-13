package main.java.peer;

import main.java.constants.AppConstants;
import main.java.constants.DisplayConstants;
import main.java.log.LogHandler;
import main.java.message.MessageHandler;
import main.java.message.MessageType;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class OptimisticUnchokeHandler extends Thread {
    private final PeerHandler peerHandler;
    private final MessageHandler messageHandler;
    private final PeerConfiguration peerConfiguration;

    /*
     * Constructor: OptimisticUnchokeHandler
     * Initializes OptimisticUnchokeHandler with the provided PeerConfiguration.
     */
    public OptimisticUnchokeHandler(PeerConfiguration peerConfiguration) {
        this.peerHandler = new PeerHandler(peerConfiguration.getPeerProcessId());
        this.peerConfiguration = peerConfiguration;
        this.messageHandler = new MessageHandler();
    }

    /*
     * Overridden run() method from Thread class.
     * Implements the logic for handling optimistic unchoking of peers.
     */
    @Override
    public void run() {
        // Initialization
        int optimisticUnChokingInterval = peerConfiguration.getOptimisticUnchokingInterval();
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        PeerData peerData = peerIdToDataMap.get(peerConfiguration.getPeerProcessId());
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();
        LogHandler logHandler = peerConfiguration.getLogHandler();

        // Main Processing Loop
        while (peerConfiguration.getNumberOfPeerHavingFile() < peerIdToDataMap.size()) {
            List<Integer> interestedPeers = peerIdToConnectionsMap.keySet().stream()
                    .filter(connection -> peerIdToConnectionsMap.get(connection).getIsInterested())
                    .collect(Collectors.toList());

            // Optimistically unchoke a randomly selected interested peer
            if (interestedPeers.size() > 0) {
                int connection = interestedPeers.get(new Random().nextInt(interestedPeers.size()));
                PeerConnection currConn =  peerIdToConnectionsMap.get(connection);
                peerHandler.handleSendChokeUnchokeMessage(currConn, MessageType.UNCHOKE, false);

                // Set the peer as optimistically unchoked
                peerIdToConnectionsMap.get(connection).setIsOptimisticallyUnchoked(true);

                // Logging optimistic unchoke message
                String message = String.format(DisplayConstants.OPTIMISTIC_UNCHOKE_NEIGHBOR_MESSAGE, peerData.getPeerProcessId(), peerIdToConnectionsMap.get(connection).getPeerProcessId());
                logHandler.printMessageAndLogMessageToFile(message);

                // Delay for the optimistic unchoking interval
                peerHandler.peerHold(optimisticUnChokingInterval * AppConstants.DELAY);

                // Resetting the optimistic unchoke status
                peerIdToConnectionsMap.get(connection).setIsOptimisticallyUnchoked(false);
            }
        }

        // Final delay and handling exit message
        peerHandler.peerHold(5 * AppConstants.DELAY);
        messageHandler.handleExitMessage();
    }
}