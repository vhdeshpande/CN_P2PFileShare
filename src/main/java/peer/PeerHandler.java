package main.java.peer;

import main.java.constants.DisplayConstants;
import main.java.log.LogHandler;
import main.java.message.MessageHandler;
import main.java.constants.AppConstants;
import main.java.message.MessageType;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
/**
 * Class: PeerHandler
 * Description: Manages the setup and execution of peer processes in a peer-to-peer network.
 */
public class PeerHandler {
    // Static variable to hold the peer process ID
    private static int peerProcessId;
    /**
     * Constructor: PeerHandler
     * Description: Initializes the PeerHandler with the given peer process ID.
     *
     * @param peerProcessId The ID of the peer process.
     */
    public PeerHandler(int peerProcessId) {
        PeerHandler.peerProcessId = peerProcessId;
    }
    /**
     * Method: setupAndStartPeerProcess
     * Description: Sets up the peer configuration, initializes necessary components, and starts the peer process.
     *
     * @param peerProcessId The ID of the peer process.
     * @throws IOException If an I/O error occurs.
     */
    public static void setupAndStartPeerProcess(int peerProcessId) throws IOException {
        PeerConfiguration peerConfiguration = new PeerConfiguration();
        peerConfiguration.setPeerProcessId(peerProcessId);
        peerConfiguration.setConnections(new ConcurrentHashMap<>());

        LogHandler logHandler = new LogHandler(peerProcessId);
        File peerDirectory = setupPeerDirectory();
        peerConfiguration.setDirectory(peerDirectory);

        Properties commonConfiguration = parseCommonConfiguration();
        int numberOfPreferredNeighbors = Integer.parseInt(commonConfiguration.getProperty(AppConstants.NUMBER_OF_PREFERRED_NEIGHBORS));
        int unchokingInterval = Integer.parseInt(commonConfiguration.getProperty(AppConstants.UNCHOKING_INTERVAL));
        int optimisticUnchokingInterval = Integer.parseInt(commonConfiguration.getProperty(AppConstants.OPTIMISTIC_UNCHOKING));
        String filename = commonConfiguration.getProperty(AppConstants.FILE_NAME);
        int fileSize = Integer.parseInt(commonConfiguration.getProperty(AppConstants.FILE_SIZE));
        int pieceSize = Integer.parseInt(commonConfiguration.getProperty(AppConstants.PIECE_SIZE));

        Map<Integer, PeerData> peerIdToDataMap = parsePeerConfiguration();
        peerConfiguration.setPeerIdToDataMap(peerIdToDataMap);
        peerConfiguration.setLogHandler(logHandler);

        peerConfiguration.setNumberOfPreferredNeighbors(numberOfPreferredNeighbors);
        peerConfiguration.setUnchokingInterval(unchokingInterval);
        peerConfiguration.setOptimisticUnchokingInterval(optimisticUnchokingInterval);
        peerConfiguration.setFileName(filename);
        peerConfiguration.setFileSize(fileSize);
        peerConfiguration.setChunkSize(pieceSize);

        calculateAndAssignBitfield(peerConfiguration);
        chopFileIntoPieces(peerConfiguration);

        peerHold(AppConstants.DELAY*2);
        new Sender(peerConfiguration).start();
        new Receiver(peerConfiguration).start();
        new ChokeUnchokeHandler(peerConfiguration).start();
        new OptimisticUnchokeHandler(peerConfiguration).start();
    }

    /**
     * Method: setupAndStartPeerProcess
     * Description: Sets up the peer configuration, initializes necessary components, and starts the peer process.
     *
     * @param peerProcessId The ID of the peer process.
     * @throws IOException If an I/O error occurs.
     */
    public static File setupPeerDirectory() {
        String peerDirFileName = String.format(AppConstants.PEER_DIRECTORY_NAME, peerProcessId);
        File peerDirectory = new File(peerDirFileName);
        if (!peerDirectory.exists()) {
            peerDirectory.mkdir();
        }
        return peerDirectory;
    }
    /**
     * Method: parsePeerConfiguration
     * Description: Parses the peer configuration file and returns a map of peer data.
     *
     * @return A map containing peer data with peer process ID as the key.
     */
    public static Map<Integer, PeerData> parsePeerConfiguration() {
        String peerConfigFileName = AppConstants.PEER_CONFIGURATION_FILE_NAME;
        Map<Integer, PeerData> peerIdToDataMap = new LinkedHashMap<>();
        try {
            FileReader fileReader = new FileReader(peerConfigFileName);
            BufferedReader buffReader = new BufferedReader(fileReader);
            String peerInfoEntry;
            while ((peerInfoEntry = buffReader.readLine()) != null) {
                String[] entry = peerInfoEntry.split(AppConstants.CONFIG_FILE_REGEX);
                if (entry.length == 4) {
                    int peerProcessId = Integer.parseInt(entry[0]);
                    String host = entry[1];
                    int portNumber = Integer.parseInt(entry[2]);
                    int hasFile = Integer.parseInt(entry[3]);

                    peerIdToDataMap.put(peerProcessId,new PeerData(peerProcessId, host, portNumber, hasFile));
                }
            }
            buffReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return peerIdToDataMap;
    }
    /**
     * Method: parseCommonConfiguration
     * Description: Parses the common configuration file and returns a Properties object.
     *
     * @return A Properties object containing common configuration properties.
     */
    public static Properties parseCommonConfiguration() {
        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader(AppConstants.COMMON_CONFIGURATION_FILE_NAME);
             BufferedReader buffReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = buffReader.readLine()) != null) {
                String[] entry = line.split(AppConstants.CONFIG_FILE_REGEX);
                if (entry.length == 2) {
                    properties.setProperty(entry[0], entry[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * Method: calculateAndAssignBitfield
     * Description: Calculates and assigns the bitfield for the peer based on file information.
     *
     * @param peerConfiguration The PeerConfiguration object containing peer-specific configuration.
     */
    private static void calculateAndAssignBitfield(PeerConfiguration peerConfiguration) {
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerProcessId);

        // Calculate the number of file chunks based on file size and piece size
        int numberOfFileChunks = getNumberOfFileChunks(peerConfiguration.getFileSize(), peerConfiguration.getChunkSize());
        int peerHasFile = peerData.getHasFile();
        int[] peerBitfield = new int[numberOfFileChunks];

        // Initialize the bitfield with the peer's file status
        Arrays.fill(peerBitfield, peerHasFile);
        peerData.setBitfield(peerBitfield);

        // If the peer has the complete file, update the file status for the peer
        if (peerData.getHasFile() == AppConstants.PEER_HAS_FILE) {
            peerConfiguration.updateFileStatusForPeer();
        }
    }

    /**
     * Method: getNumberOfFileChunks
     * Description: Calculates the number of file chunks based on file size and piece size.
     *
     * @param fileSize  The total size of the file.
     * @param pieceSize The size of each file piece (chunk).
     * @return The calculated number of file chunks.
     */
    private static int getNumberOfFileChunks(int fileSize, int pieceSize){
        return (fileSize + pieceSize - 1) / pieceSize;
    }
    /**
     * Method: readFileBytes
     * Description: Reads the bytes of a file given its directory, filename, and size.
     *
     * @param dir      The directory containing the file.
     * @param filename The name of the file.
     * @param fileSize The size of the file.
     * @return An array of bytes representing the contents of the file.
     * @throws IOException If an I/O error occurs.
     */
    private static byte[] readFileBytes(File dir, String filename, int fileSize) throws IOException {
        // Create input stream for the file
        FileInputStream inStream = new FileInputStream(dir.getAbsolutePath() + File.separator + filename);
        BufferedInputStream file = new BufferedInputStream(inStream);

        // Read the bytes of the file into a byte array
        byte[] fileBytes = new byte[fileSize];
        int read = file.read(fileBytes);

        // Close the file stream
        file.close();

        return fileBytes;
    }


    /**
     * Method: chopFileIntoPieces
     * Description: Reads the file, divides it into chunks, and updates peer configuration accordingly.
     *
     * @param peerConfiguration The PeerConfiguration object containing peer-specific configuration.
     * @throws IOException If an I/O error occurs.
     */
    private static void chopFileIntoPieces(PeerConfiguration peerConfiguration) throws IOException {
        String filename = peerConfiguration.getFileName();
        int fileSize = peerConfiguration.getFileSize();
        int chunkSize = peerConfiguration.getChunkSize();

        // Calculate the number of file chunks based on file size and chunk size
        int numberOfFileChunks = getNumberOfFileChunks(fileSize, chunkSize);
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerProcessId);

        // If the peer does not have the file, set chunks to an empty array and return
        if (peerData.getHasFile() == AppConstants.PEER_HAS_NO_FILE) {
            peerConfiguration.setChunks(new byte[numberOfFileChunks][]);
            return;
        }

        // Initialize the array to store file chunks
        byte[][] fileChunkIdToContentMap = new byte[numberOfFileChunks][];
        byte[] fileBytes = readFileBytes(peerConfiguration.getDirectory(), filename, fileSize);
        int index = 0;
        int peerChunkCount = peerData.getChunkCount();
        int bitfieldLength = peerData.getBitfield().length;

        // Loop through the file and create chunks
        for (int i = 0; i < fileSize; i += chunkSize) {
            int chunkEnd = Math.min(i + chunkSize, fileSize);
            byte[] chunk = Arrays.copyOfRange(fileBytes, i, chunkEnd);
            fileChunkIdToContentMap[index++] = chunk;

            // Update peer information if all chunks are received
            if (++peerChunkCount == bitfieldLength) {
                peerData.setHasFile(AppConstants.PEER_HAS_FILE);
            }
        }

        // Update peer chunk count and set the chunks in the configuration
        peerData.setChunkCount(peerChunkCount);
        peerConfiguration.setChunks(fileChunkIdToContentMap);
    }


    /**
     * Method: updatePeerFileStatus
     * Description: Updates the file status for the peer, logs completion message, and writes the file if all chunks are received.
     *
     * @param peerConfiguration The PeerConfiguration object containing peer-specific configuration.
     */
    public void updatePeerFileStatus(PeerConfiguration peerConfiguration) {
        String filePath = peerConfiguration.getDirectory().getAbsolutePath() + File.separator + peerConfiguration.getFileName();
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());
        LogHandler logHandler = peerConfiguration.getLogHandler();
        byte[][] fileChunks = peerConfiguration.getChunks();

        // Count the number of '1' bits in the peer's bitfield
        int bitfieldIndex = (int) Arrays.stream(peerData.getBitfield())
                .filter(bit -> bit == AppConstants.PEER_HAS_FILE)
                .count();

        // Check if all chunks are received
        if (bitfieldIndex == peerData.getBitfield().length) {
            // Log download completion message
            String message = String.format(DisplayConstants.DOWNLOAD_COMPLETE_MESSAGE, peerData.getPeerProcessId());
            logHandler.printMessageAndLogMessageToFile(message);

            // Reset bitfield index and create a byte array to store the complete file
            bitfieldIndex = 0;
            byte[] peerBitfield = new byte[peerConfiguration.getFileSize()];

            // Iterate through file chunks and copy them to the complete file array
            for (int i = 0; i < fileChunks.length; i++) {
                byte[] chunk = fileChunks[i];

                for (int j = 0; j < chunk.length; j++) {
                    peerBitfield[bitfieldIndex++] = chunk[j];
                }
            }

            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
                // Write the complete file to disk
                bufferedOutputStream.write(peerBitfield);
            } catch (IOException e) {
                // Ignoring IOException as it is expected and does not require any action
            }

            // Update peer file status and configuration
            peerData.setHasFile(AppConstants.PEER_HAS_FILE);
            peerConfiguration.updateFileStatusForPeer();
        }
    }


    /**
     * Method: handleSendChokeUnchokeMessage
     * Description: Handles sending choke or unchoke messages to a peer connection.
     *
     * @param peerConnection The PeerConnection object representing the connection to a peer.
     * @param messageType    The type of message (choke or unchoke).
     * @param setChokeValue  The value indicating whether to set the choke state (true for choke, false for unchoke).
     */
    public void handleSendChokeUnchokeMessage(PeerConnection peerConnection, MessageType messageType, Boolean setChokeValue) {
        // Set the choke state for the peer connection
        peerConnection.setIsChoked(setChokeValue);

        try {
            // Send the choke or unchoke message to the peer connection
            MessageHandler.sendMessage(peerConnection.getConnection(), MessageHandler.constructMessage(1, messageType, null));
        } catch (IOException e) {
            // Ignoring IOException as it is expected and does not require any action
        }
    }


    /**
     * Method: sendUnchokeMessage
     * Description: Sends an unchoke message to a specific peer if currently choked.
     *
     * @param peerConfiguration    The PeerConfiguration object containing peer-specific configuration.
     * @param peerIdForNeighbor    The peer ID for the neighbor to send the unchoke message.
     */
    public void sendUnchokeMessage(PeerConfiguration peerConfiguration, int peerIdForNeighbor) {
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // Check if the neighbor is currently choked
        if (peerIdToConnectionsMap.get(peerIdForNeighbor).getIsChoked()) {
            // Retrieve the connection for the neighbor and send an unchoke message
            PeerConnection currConn = peerIdToConnectionsMap.get(peerIdForNeighbor);
            handleSendChokeUnchokeMessage(currConn, MessageType.UNCHOKE, false);
        }
    }


    /**
     * Method: peerHold
     * Description: Pauses the current thread for the specified duration in milliseconds.
     *
     * @param milliseconds The duration to pause the thread.
     */
    public static void peerHold(long milliseconds) {
        try {
            // Pause the thread for the specified duration
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
            // Ignoring InterruptedException as it is expected and does not require any action
        }
    }


    /**
     * Method: checkInterestedAndSendData
     * Description: Determines which peers to unchoke based on interest and sends appropriate messages.
     *
     * @param peerConfiguration      The PeerConfiguration object containing peer-specific configuration.
     * @param interestedPeers        The list of peers interested in the current peer.
     * @param numberOfPreferredNeighbors The maximum number of preferred neighbors to unchoke.
     * @param preferredNeighbors     An array to store the peer IDs of preferred neighbors.
     */
    public void checkInterestedAndSendData(PeerConfiguration peerConfiguration, List<Integer> interestedPeers,
                                           int numberOfPreferredNeighbors, int[] preferredNeighbors) {
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // If the number of interested peers is less than or equal to the maximum preferred neighbors,
        // unchoke all interested peers
        if (interestedPeers.size() <= numberOfPreferredNeighbors) {
            for (Integer currPeerId : interestedPeers) {
                sendUnchokeMessage(peerConfiguration, currPeerId);
            }
        } else {
            // If there are more interested peers than the maximum preferred neighbors,
            // randomly select and unchoke the maximum preferred neighbors
            Random random = new Random();
            IntStream.range(0, numberOfPreferredNeighbors)
                    .forEach(nei -> {
                        int randomPeer = Math.abs(random.nextInt(interestedPeers.size()));
                        preferredNeighbors[nei] = interestedPeers.remove(randomPeer);
                    });

            // Unchoke the selected preferred neighbors
            unchokeAllNeighbors(peerConfiguration, preferredNeighbors);

            // Choke remaining interested peers
            for (Integer interestedPeer : interestedPeers) {
                if (!peerIdToConnectionsMap.get(interestedPeer).getIsChoked() &&
                        !peerIdToConnectionsMap.get(interestedPeer).getIsOptimisticallyUnchoked()) {
                    PeerConnection currConn =  peerIdToConnectionsMap.get(interestedPeer);
                    handleSendChokeUnchokeMessage(currConn, MessageType.CHOKE, true);
                }
            }
        }
    }


    /**
     * Method: unchokeAllNeighbors
     * Description: Unchokes all neighbors specified in the array.
     *
     * @param peerConfiguration The PeerConfiguration object containing peer-specific configuration.
     * @param neighbors         An array of peer IDs to be unchoked.
     */
    private void unchokeAllNeighbors(PeerConfiguration peerConfiguration, int[] neighbors) {
        for (int peerId : neighbors) {
            // Send unchoke message to each neighbor
            sendUnchokeMessage(peerConfiguration, peerId);
        }
    }

    /**
     * Method: findHighestRatePeer
     * Description: Finds the peer with the highest upload rate among a list of peers.
     *
     * @param peerConfiguration      The PeerConfiguration object containing peer-specific configuration.
     * @param peers                  The list of peer IDs to compare based on upload rate.
     * @return The peer ID with the highest upload rate.
     */
    private int findHighestRatePeer(PeerConfiguration peerConfiguration, List<Integer> peers) {
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // Initialize with the first peer in the list
        int highestRatePeer = peers.get(0);

        // Iterate through the list of peers and find the one with the highest upload rate
        for (int peer = 1; peer < peers.size(); peer++) {
            int currentPeer = peers.get(peer);
            if (peerIdToConnectionsMap.get(highestRatePeer).getRate() <=
                    peerIdToConnectionsMap.get(currentPeer).getRate()) {
                highestRatePeer = currentPeer;
            }
        }

        return highestRatePeer;
    }


    /**
     * Method: checkPreferenceAndSendChokeUnchoke
     * Description: Determines which peers to unchoke based on preference and sends appropriate messages.
     *
     * @param peerConfiguration      The PeerConfiguration object containing peer-specific configuration.
     * @param interestedPeers        The list of peers interested in the current peer.
     * @param numberOfPreferredNeighbors The maximum number of preferred neighbors to unchoke.
     * @param preferredNeighbors     An array to store the peer IDs of preferred neighbors.
     */
    public void checkPreferenceAndSendChokeUnchoke(PeerConfiguration peerConfiguration, List<Integer> interestedPeers,
                                                   int numberOfPreferredNeighbors, int[] preferredNeighbors) {
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // If the number of interested peers is less than or equal to the maximum preferred neighbors,
        // unchoke all interested peers and set preferred neighbors
        if (interestedPeers.size() <= numberOfPreferredNeighbors) {
            Iterator<Integer> iterator = interestedPeers.iterator();
            int index = 0;
            while (iterator.hasNext() && index < numberOfPreferredNeighbors) {
                int peerId = iterator.next();
                preferredNeighbors[index++] = peerId;
                sendUnchokeMessage(peerConfiguration, peerId);
            }
        } else {
            // If there are more interested peers than the maximum preferred neighbors,
            // select and unchoke the peers with the highest upload rates up to the maximum preferred neighbors
            for (int peer = 0; peer < numberOfPreferredNeighbors && !interestedPeers.isEmpty(); peer++) {
                int peerId = findHighestRatePeer(peerConfiguration, interestedPeers);
                sendUnchokeMessage(peerConfiguration, peerId);
                preferredNeighbors[peer] = peerId;

                // Remove the selected peer from the list
                if (!interestedPeers.isEmpty()) {
                    interestedPeers.remove(peerId);
                }
            }

            // Choke remaining interested peers
            interestedPeers.stream()
                    .filter(peer -> !peerIdToConnectionsMap.get(peer).getIsChoked() &&
                            !peerIdToConnectionsMap.get(peer).getIsOptimisticallyUnchoked())
                    .forEach(peer -> {
                        PeerConnection currConn = peerIdToConnectionsMap.get(peer);
                        handleSendChokeUnchokeMessage(currConn, MessageType.CHOKE, true);
                    });
        }
    }


}
