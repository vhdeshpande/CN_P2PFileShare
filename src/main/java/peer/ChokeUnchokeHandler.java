package main.java.peer;

import main.java.constants.AppConstants;
import main.java.constants.DisplayConstants;
import main.java.log.LogHandler;
import main.java.message.MessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChokeUnchokeHandler extends Thread{
    private final PeerHandler peerHandler;
    private final MessageHandler messageHandler;
    private final PeerConfiguration peerConfiguration;
    /*
     * Constructor: ChokeUnchokeHandler
     * Initializes ChokeUnchokeHandler with the provided PeerConfiguration.
     */
    public ChokeUnchokeHandler(PeerConfiguration peerConfiguration){
        this.peerHandler = new PeerHandler(peerConfiguration.getPeerProcessId());
        this.peerConfiguration = peerConfiguration;
        this.messageHandler = new MessageHandler();
    }

    /*
     * Overridden run() method from Thread class.
     * Implements the logic for handling choking and unchoking of peers.
     */
    @Override
    public void run() {
        // Initialization
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        PeerData peerData = peerIdToDataMap.get(peerConfiguration.getPeerProcessId());
        int numberOfPreferredNeighbors = peerConfiguration.getNumberOfPreferredNeighbors();

        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();
        LogHandler logHandler = peerConfiguration.getLogHandler();

        // Main Processing Loop
        while (peerConfiguration.getNumberOfPeerHavingFile() < peerIdToDataMap.size()) {
            int[] preferredNeighbors = new int[numberOfPreferredNeighbors];

            // Handling when the current peer has the file
            if (peerData.getHasFile() == AppConstants.PEER_HAS_FILE) {
                List<Integer> interestedPeers = peerIdToConnectionsMap.entrySet().stream()
                        .filter(entry -> entry.getValue().getIsInterested())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                if (interestedPeers.size() == 0) {
                    continue;
                }

                peerHandler.checkInterestedAndSendData(peerConfiguration, interestedPeers, numberOfPreferredNeighbors, preferredNeighbors);
            }
            // Handling when the current peer does not have the file
            else {
                ArrayList<Integer> interestedPeers = new ArrayList<>();
                peerIdToConnectionsMap.entrySet().stream()
                        .filter(entry -> entry.getValue().getIsInterested() && entry.getValue().getRate() >= 0)
                        .forEach(entry -> interestedPeers.add(entry.getKey()));

                peerHandler.checkPreferenceAndSendChokeUnchoke(peerConfiguration, interestedPeers, numberOfPreferredNeighbors, preferredNeighbors);
            }

            // Logging Preferred Neighbors
            boolean isPref = true;
            StringBuilder stringBuilder = new StringBuilder();
            for (int prefNeighbor : preferredNeighbors) {
                if (prefNeighbor != 0) {
                    isPref = false;
                    stringBuilder.append(" ").append(prefNeighbor).append(",");
                }
            }
            if (!isPref) {
                String message = String.format(DisplayConstants.PREFERRED_NEIGHBORS_MESSAGE, peerData.getPeerProcessId(), stringBuilder.substring(0, stringBuilder.length() - 1));
                logHandler.printMessageAndLogMessageToFile(message);
            }

            // Introducing delay for unchoking interval
            peerHandler.peerHold(peerConfiguration.getUnchokingInterval() * AppConstants.DELAY);
        }

        // Final delay and handling exit message
        peerHandler.peerHold(5 * AppConstants.DELAY);
        messageHandler.handleExitMessage();
    }
}