package main.java.message;

import main.java.constants.AppConstants;
import main.java.constants.DisplayConstants;
import main.java.peer.PeerConfiguration;
import main.java.peer.PeerConnection;
import main.java.peer.PeerData;
import main.java.peer.PeerHandler;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.System.*;
import static java.lang.System.arraycopy;

/**
 * Class: MessageHandler
 * Description: Handles message communication and processing for peers.
 */
public class MessageHandler {
    public static final int MESSAGE_SIZE = 32;
    public static final int PEER_PROCESS_ID_SIZE = 4;

    /**
     * Converts an integer message into a byte array of specified size.
     *
     * @param size    The size of the byte array.
     * @param message The integer message to be converted.
     * @return The byte array representation of the integer message.
     */
    private static byte[] getByteArrayMessageInt(int size, int message) {
        byte[] messageByteArray = ByteBuffer.allocate(size).putInt(message).array();
        return messageByteArray;
    }

    /**
     * Sends a byte array message over the provided socket connection.
     *
     * @param connection The socket connection to send the message through.
     * @param message    The byte array message to be sent.
     * @throws IOException If an I/O error occurs during the message sending process.
     */
    public static void sendMessage(Socket connection, byte[] message) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.write(message);
        dataOutputStream.flush();
    }

    /**
     * Reads and accepts a byte array message from the specified socket connection.
     *
     * @param connection The socket connection from which to accept the message.
     * @return The byte array message received from the socket connection.
     * @throws IOException If an I/O error occurs during the message acceptance process.
     */
    public static byte[] acceptMessage(Socket connection) throws IOException {
        DataInputStream dataInputStream = null;
        byte[] byteMessage = new byte[AppConstants.BYTE_SIZE];
        dataInputStream = new DataInputStream(connection.getInputStream());
        dataInputStream.readFully(byteMessage);
        return byteMessage;
    }

    /**
     * Handles the received message based on its type and performs appropriate actions.
     *
     * @param messageType       The type of the received message.
     * @param receivedMessage   The byte array containing the received message.
     * @param peerConfiguration The configuration of the peer.
     * @param peerConnection    The connection information for the peer.
     * @param totalTime         The total time elapsed for the operation.
     * @throws IOException If an I/O error occurs during message handling.
     */
    public static void handleReceivedMessage(MessageType messageType, byte[] receivedMessage, PeerConfiguration peerConfiguration, PeerConnection peerConnection, double totalTime) throws IOException {
        switch (messageType){
            case REQUEST:
                sendMessage(peerConnection.getConnection(),
                        createPieceMessage(ByteBuffer.wrap(receivedMessage).getInt(),
                                peerConfiguration.getChunks()[ByteBuffer.wrap(receivedMessage).getInt()]));
                break;

            case HAVE:
                handleHaveMessage(peerConfiguration, peerConnection, receivedMessage);
                break;

            case PIECE:
                handlePieceMessage(peerConfiguration, peerConnection, receivedMessage, totalTime/AppConstants.TIME_CONSTANT);
                break;

            case INTERESTED:
                handleInterestedMessage(peerConfiguration, peerConnection);
                break;

            case NOT_INTERESTED:
                handleNotInterestedMessage(peerConfiguration, peerConnection);
                break;

            case CHOKE:
                handleChokeMessage(peerConfiguration, peerConnection);
                break;

            case UNCHOKE:
                handleUnChokeMessage(peerConfiguration, peerConnection);
                break;

            case BITFIELD:
                handleBitfieldMessage(peerConfiguration, peerConnection, receivedMessage);
                break;

            case EXIT:
                handleExitMessage();
                break;

            default:
                break;
        }
    }

    /**
     * Creates a handshake message with the given peer process ID and sends it over the specified connection.
     *
     * @param connection        The socket connection to send the handshake message through.
     * @param peerConfiguration The configuration of the peer.
     * @throws IOException If an I/O error occurs during the handshake message creation or sending.
     */
    public static void createAndSendHandshake(Socket connection, PeerConfiguration peerConfiguration ) throws IOException {
        int peerProcessId = peerConfiguration.getPeerProcessId();
        byte[] handshakeMessage = createHandshakeMessage(peerProcessId);
        sendMessage(connection, handshakeMessage );
    }

    /**
     * Accepts an incoming connection, reads the handshake message, and establishes the connection details.
     *
     * @param connection        The socket connection to accept and read the handshake message from.
     * @param peerConfiguration The configuration of the peer.
     */
    public static void acceptConnectionAndReadHandshake(Socket connection, PeerConfiguration peerConfiguration) {
        int peerProcessId = peerConfiguration.getPeerProcessId();

        try {
            byte[] byteMessage = acceptMessage(connection);
            int connPeerProcessId = ByteBuffer.wrap(byteMessage, 32-4, AppConstants.INT_SIZE_BYTES).getInt();
            String message = String.format(DisplayConstants.CONNECTED_MESSAGE, peerProcessId, connPeerProcessId );
            setHandshakeAndConnection(connection, peerConfiguration, message, byteMessage, connPeerProcessId, connPeerProcessId );

            createAndSendHandshake(connection, peerConfiguration);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets up the connection details and reads the handshake message from the specified socket connection.
     * Closes the connection if the received peer ID does not match the expected peer process ID.
     *
     * @param connection        The socket connection to set up and read the handshake message from.
     * @param peerConfiguration The configuration of the peer.
     * @param connPeerProcessId The expected peer process ID for the connection.
     */
    public static void setConnectionAndReadHandshake(Socket connection, PeerConfiguration peerConfiguration, int connPeerProcessId) {
        int peerProcessId = peerConfiguration.getPeerProcessId();
        try {
            byte[] byteMessage = acceptMessage(connection);
            int peerID = ByteBuffer.wrap(byteMessage, 32-4, AppConstants.INT_SIZE_BYTES).getInt();
            if(peerID != connPeerProcessId){
                connection.close();
            }else{
                String message = String.format(DisplayConstants.MAKE_CONNECTION_MESSAGE, peerProcessId, connPeerProcessId);
                setHandshakeAndConnection(connection, peerConfiguration, message, byteMessage, connPeerProcessId, peerID );

            }
        } catch (IOException e) {
//            throw new RuntimeException(e);
        }

    }

    /**
     * Combines multiple byte arrays into a single byte array message.
     *
     * @param messageContents Variable number of byte arrays to be combined.
     * @return The combined byte array message.
     */
    private static byte[] getByteArrayMessage(byte[]... messageContents) {
        byte[] message = new byte[MessageHandler.MESSAGE_SIZE];
        int index = 0;
        for (byte[] byteArray : messageContents) {
            arraycopy(byteArray, 0, message, index, byteArray.length);
            index += byteArray.length;
        }
        return message;
    }

    /**
     * Creates a handshake message byte array for the specified peer process ID.
     *
     * @param peerProcessId The peer process ID to be included in the handshake message.
     * @return The byte array representing the handshake message.
     */
    public static byte[] createHandshakeMessage(int peerProcessId){
        byte[] handshakeMessageHeader = AppConstants.HANDSHAKE_MESSAGE_HEADER.getBytes();
        byte[] handshakeMessageZeroBits = AppConstants.HANDSHAKE_MESSAGE_ZERO_BITS.getBytes();
        byte[] peerIdByteArray = getByteArrayMessageInt(PEER_PROCESS_ID_SIZE, peerProcessId);
        return getByteArrayMessage(handshakeMessageHeader, handshakeMessageZeroBits, peerIdByteArray);
    }

    /**
     * Constructs a message byte array with specified size, type, and content.
     *
     * @param messageSize    The size of the message content.
     * @param messageType    The type of the message.
     * @param messageContent The content of the message.
     * @return The byte array representing the constructed message.
     */
    public static byte[] constructMessage(int messageSize, MessageType messageType, byte[] messageContent){
        byte[] message = new byte[messageSize + AppConstants.INT_SIZE_BYTES];
        byte[] byteMessage = getByteArrayMessageInt(AppConstants.INT_SIZE_BYTES, messageSize);

        arraycopy(byteMessage, 0, message, 0, byteMessage.length);
        int byteMessageLength = byteMessage.length;

        message[byteMessageLength++] = MessageType.getByteFromMessageType(messageType);

        if (messageContent != null) {
            arraycopy(messageContent, 0, message, byteMessageLength, messageContent.length);
        }

        return message;
    }

    /**
     * Constructs a bitfield message byte array with the specified bitfield values.
     *
     * @param bitfield The array of bitfield values to be included in the message.
     * @return The byte array representing the constructed bitfield message.
     */
    public byte[] constructBitfieldMessage(int[] bitfield){
        int msgLen = 1 + (AppConstants.INT_SIZE_BYTES * bitfield.length);
        byte[] message = new byte[msgLen - 1];
        int index = 0;
        for (int i = 0; i < bitfield.length; i++) {
            int bit = bitfield[i];
            byte[] byteMessage = getByteArrayMessageInt(AppConstants.INT_SIZE_BYTES, bit);
            arraycopy(byteMessage, 0, message, index, byteMessage.length);
            index += byteMessage.length;
        }
        return constructMessage(msgLen, MessageType.BITFIELD, message);
    }

    /**
     * Creates a piece message byte array with the specified piece index and content.
     *
     * @param pieceIndex The index of the piece in the file.
     * @param piece      The content of the piece.
     * @return The byte array representing the constructed piece message.
     */
    public static byte[] createPieceMessage(int pieceIndex, byte[] piece){
        byte[] indexBytes = getByteArrayMessageInt(AppConstants.INT_SIZE_BYTES, pieceIndex);
        byte[] messageContent = new byte[indexBytes.length + piece.length];

        System.arraycopy(indexBytes, 0, messageContent, 0, indexBytes.length);
        System.arraycopy(piece, 0, messageContent, indexBytes.length, piece.length);

        return constructMessage(AppConstants.INT_SIZE_BYTES + AppConstants.MESSAGE_TYPE_SIZE + piece.length, MessageType.PIECE, messageContent);
    }

    /**
     * Updates the local bitfield status, identifies missing chunks in the connection's bitfield,
     * and sends a request.
     *
     * @param connection        The socket connection to send the request through.
     * @param bitfield          The local bitfield status.
     * @param connectionBitfield The connection's bitfield status.
     * @throws IOException If an I/O error occurs during the message sending process.
     */
    public static void updateBitfieldStatusAndSendRequest(Socket connection, int[] bitfield, int[] connectionBitfield) throws IOException {
        ArrayList<Integer> bitfieldIndices = new ArrayList<>();
        for (int bitfieldIndex = 0; bitfieldIndex < bitfield.length; bitfieldIndex++) {
            if (bitfield[bitfieldIndex] == 0 && connectionBitfield[bitfieldIndex] == AppConstants.HAS_BITFIELD) {
                bitfieldIndices.add(bitfieldIndex);
            }
        }
        if (bitfieldIndices.size() > 0) {
            int requestChunkIndex = bitfieldIndices.get(Math.abs(new Random().nextInt() % bitfieldIndices.size()));
            sendMessage(connection, constructMessage(AppConstants.INT_SIZE_BYTES + AppConstants.MESSAGE_TYPE_SIZE, MessageType.REQUEST, getByteArrayMessageInt(AppConstants.INT_SIZE_BYTES, requestChunkIndex)));
        }
    }

    /**
     * Compares the local bitfield with the connection's bitfield, determines interest status,
     * and sends an interest or not interested message accordingly.
     *
     * @param connection        The socket connection to send the interest message through.
     * @param bitfield          The local bitfield status.
     * @param connectionBitfield The connection's bitfield status.
     * @throws IOException If an I/O error occurs during the message sending process.
     */
    public static void compareBitfieldAndSendInterestMessage(Socket connection, int[] bitfield, int[] connectionBitfield) throws IOException {
        boolean isInterested = false;
        for (int bit = 0; bit < bitfield.length; bit++) {
            if (bitfield[bit] == AppConstants.PEER_HAS_NO_FILE && connectionBitfield[bit] == AppConstants.PEER_HAS_FILE) {
                isInterested = true;
                break;
            }
        }
        MessageType messageType = isInterested ? MessageType.INTERESTED : MessageType.NOT_INTERESTED;
        sendMessage(connection,constructMessage(AppConstants.MESSAGE_TYPE_SIZE, messageType, null));

    }

    /**
     * Handles the HAVE message, updates the bitfield, checks for completion, and sends interest message.
     *
     * @param peerConfiguration The configuration of the peer.
     * @param peerConnection    The connection information for the peer.
     * @param receivedMessage   The byte array containing the received HAVE message.
     * @throws IOException If an I/O error occurs during the message handling process.
     */
    public static void handleHaveMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection, byte[] receivedMessage) throws IOException {
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());
        int index = ByteBuffer.wrap(receivedMessage).getInt();

        int [] bitfield = peerIdToDataMap.get(peerConnection.getPeerProcessId()).getBitfield();
        bitfield[index] = AppConstants.HAS_BITFIELD;
        peerIdToDataMap.get(peerConnection.getPeerProcessId()).setBitfield(bitfield);

        int bits = (int) Arrays.stream(peerIdToDataMap.get(peerConnection.getPeerProcessId()).getBitfield())
                .filter(b -> b == AppConstants.HAS_BITFIELD)
                .count();

        int [] peerDataBitfield = peerData.getBitfield();
        if(bits == peerDataBitfield.length){
            peerIdToDataMap.get(peerConnection.getPeerProcessId()).setHasFile(AppConstants.PEER_HAS_FILE);
            peerConfiguration.updateFileStatusForPeer();
        }
        compareBitfieldAndSendInterestMessage(peerConnection.getConnection(), peerDataBitfield, peerIdToDataMap.get(peerConnection.getPeerProcessId()).getBitfield());

        String message = String.format(DisplayConstants.HAVE_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId(), index );
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);
    }

    /**
     * Handles the PIECE message, updates peer data, file chunks, bitfield, and sends requests if unchoked.
     *
     * @param peerConfiguration The configuration of the peer.
     * @param peerConnection    The connection information for the peer.
     * @param receivedMessage   The byte array containing the received PIECE message.
     * @param totalTime         The total time elapsed for the operation.
     * @throws IOException If an I/O error occurs during the message handling process.
     */
    public static void handlePieceMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection, byte[] receivedMessage, double totalTime) throws IOException {
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        PeerHandler peerHandler = new PeerHandler(peerConfiguration.getPeerProcessId());
        byte[][] fileChunks = peerConfiguration.getChunks();

        Map<Integer, PeerConnection> connectedPeers = peerConfiguration.getPeerProcessIdToConnectionsMap();
        int index = 0;
        for (int i = 0; i < AppConstants.INT_SIZE_BYTES; i++) {
            index = (index << 8) | (receivedMessage[i] & 0xFF);
        }
        fileChunks[index] = Arrays.copyOfRange(receivedMessage, AppConstants.INT_SIZE_BYTES, receivedMessage.length);
        int [] bitfield = peerData.getBitfield();
        bitfield[index] = AppConstants.HAS_BITFIELD;
        peerData.setBitfield(bitfield);

        int peerChunkCount = peerData.getChunkCount() + 1;
        peerData.setChunkCount(peerChunkCount);
        if(peerChunkCount == peerData.getBitfield().length){
            peerData.setHasFile(AppConstants.PEER_HAS_FILE);
        }

        if(!peerConnection.getIsChoked()){
            updateBitfieldStatusAndSendRequest(peerConnection.getConnection(), peerData.getBitfield(), peerIdToDataMap.get(peerConnection.getPeerProcessId()).getBitfield());
        }

        calculateDownloadRate(receivedMessage, peerConnection, peerConfiguration, totalTime);

        String message = String.format(DisplayConstants.DOWNLOAD_CHUNK_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId(), index, peerData.getChunkCount());
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);

        peerHandler.updatePeerFileStatus(peerConfiguration);
        for(int connection : connectedPeers.keySet()){
            PeerConnection currConnData = connectedPeers.get(connection);
            sendMessage(currConnData.getConnection(), constructMessage(AppConstants.INT_SIZE_BYTES + AppConstants.MESSAGE_TYPE_SIZE, MessageType.HAVE, getByteArrayMessageInt(4, index)));
        }
    }

    /**
     * Calculates the download rate based on the received message size and total time elapsed.
     *
     * @param receivedMessage   The byte array containing the received message.
     * @param peerConnection    The connection information for the peer.
     * @param peerConfiguration The configuration of the peer.
     * @param totalTime         The total time elapsed for the operation.
     */
    private static void calculateDownloadRate(byte[] receivedMessage, PeerConnection peerConnection,
                                              PeerConfiguration peerConfiguration, double totalTime) {

        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();
        double rate = ((double)(receivedMessage.length + AppConstants.INT_SIZE_BYTES + AppConstants.MESSAGE_TYPE_SIZE) / totalTime);
        boolean peerHasFile = (peerIdToDataMap.get(peerConnection.getPeerProcessId()).getHasFile() == AppConstants.PEER_HAS_FILE);
        if(peerHasFile){
            rate = -1;
        }
        peerConnection.setRate(rate);
    }

    /**
     * Retrieves a sorted list of files from the specified directory path.
     *
     * @param directoryPath The path of the directory containing the files.
     * @return A sorted list of files or null if the directory is empty or does not exist.
     */
    private static List<File> getSortedFileList(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            List<File> fileList = new ArrayList<>(Arrays.asList(files));
            Collections.sort(fileList);
            return fileList;
        } else {
            return null;
        }
    }

    /**
     * Reconstructs a file from a directory containing its pieces.
     *
     * @param reconstructedFilePath The path where the reconstructed file will be saved.
     * @param piecesDirectory       The directory containing the pieces of the file.
     */
    public static void fileReconstructor(String reconstructedFilePath, File piecesDirectory) {
        String inputDirectory = piecesDirectory.getAbsolutePath();

        List<File> fileList = getSortedFileList(inputDirectory);
        if (fileList != null && !fileList.isEmpty()) {
            try (FileOutputStream fos = new FileOutputStream(reconstructedFilePath)) {
                for (File file : fileList) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[(int) file.length()];
                        fis.read(buffer);
                        fos.write(buffer);
                    }
                    catch (Exception e){
//                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    /**
     * Handles an interested message received from a peer.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @param peerConnection    The connection to the peer sending the interested message.
     */
    public static void handleInterestedMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection) {
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());

        String message = String.format(DisplayConstants.INTERESTED_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId());
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);

        peerConnection.setIsInterested(true);
    }

    /**
     * Handles a not interested message received from a peer.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @param peerConnection    The connection to the peer sending the not interested message.
     * @throws IOException If an I/O error occurs during message handling.
     */
    public static void handleNotInterestedMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection) throws IOException {
        // Get the PeerData associated with the current peer's process ID
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());

        // Log the interested message to the file
        String interestedMessage = String.format(DisplayConstants.INTERESTED_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId());
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(interestedMessage);

        // Format and log the not interested message to the file
        String notInterestedMessage = String.format(DisplayConstants.NOT_INTERESTED_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId());
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(notInterestedMessage);

        // Set the "isInterested" flag to false in the peer connection
        peerConnection.setIsInterested(false);

        // Check if the peer is not choked and perform necessary actions
        if (!peerConnection.getIsChoked()) {
            // Set the "isChoked" flag to true and send a choke message
            peerConnection.setIsChoked(true);
            sendMessage(peerConnection.getConnection(), constructMessage(1, MessageType.CHOKE, null));
        }
    }

    /**
     * Handles a choke message received from a peer.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @param peerConnection    The connection to the peer sending the choke message.
     */
    public static void handleChokeMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection) {
        // Get the PeerData associated with the current peer's process ID
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerConfiguration.getPeerProcessId());

        // Format and log the choked message to the file
        String message = String.format(DisplayConstants.CHOKED_MESSAGE, peerData.getPeerProcessId(), peerConnection.getPeerProcessId());
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);

        // Set the "isChoked" flag to true in the peer connection
        peerConnection.setIsChoked(true);
    }

    /**
     * Handles an unchoke message received from a peer.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @param peerConnection    The connection to the peer sending the unchoke message.
     * @throws IOException If an I/O error occurs during message handling.
     */
    public static void handleUnChokeMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection) throws IOException {
        // Get the process ID of the current peer
        int peerProcessId = peerConfiguration.getPeerProcessId();

        // Get the PeerData associated with the current peer's process ID
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerProcessId);

        // Get the map of peer IDs to PeerData objects
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();

        // Get the process ID of the peer sending the unchoke message
        int peerConnectionProcessId = peerConnection.getPeerProcessId();

        // Set the "isChoked" flag to false in the peer connection
        peerConnection.setIsChoked(false);

        // Format and log the unchoked message to the file
        String message = String.format(DisplayConstants.UNCHOKED_MESSAGE, peerData.getPeerProcessId(), peerConnectionProcessId);
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);

        // Get the bitfield of the peer sending the unchoke message
        int[] bitfield = peerIdToDataMap.get(peerConnectionProcessId).getBitfield();

        // Update the bitfield status and send request messages if needed
        updateBitfieldStatusAndSendRequest(peerConnection.getConnection(), peerData.getBitfield(), bitfield);
    }

    /**
     * Handles a bitfield message received from a peer.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @param peerConnection    The connection to the peer sending the bitfield message.
     * @param receivedMessage   The received bitfield message as a byte array.
     * @throws IOException If an I/O error occurs during message handling.
     */
    public static void handleBitfieldMessage(PeerConfiguration peerConfiguration, PeerConnection peerConnection, byte[] receivedMessage) throws IOException {
        // Get the map of peer IDs to PeerData objects
        Map<Integer, PeerData> peerIdToDataMap = peerConfiguration.getPeerIdToDataMap();

        // Get the process ID of the current peer
        int peerProcessId = peerConfiguration.getPeerProcessId();

        // Get the process ID of the peer sending the bitfield message
        int peerConnectionProcessId = peerConnection.getPeerProcessId();

        // Get the PeerData associated with the current peer's process ID
        PeerData peerData = peerConfiguration.getPeerIdToDataMap().get(peerProcessId);

        // Calculate the size of the bitfield based on the received message
        int bitfieldSize = receivedMessage.length / AppConstants.INT_SIZE_BYTES;

        // Initialize an array to store the bitfield
        int[] bitfield = new int[bitfieldSize];

        // Wrap the received message in a ByteBuffer and extract the bitfield values
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedMessage);
        for (int i = 0; i < bitfieldSize; i++) {
            bitfield[i] = byteBuffer.getInt();
        }

        // Set the bitfield in the PeerData associated with the peer sending the message
        peerIdToDataMap.get(peerConnectionProcessId).setBitfield(bitfield);

        // Count the number of '1' bits in the bitfield to determine if the peer has the complete file
        int bits = (int) Arrays.stream(peerIdToDataMap.get(peerConnectionProcessId).getBitfield())
                .filter(bit -> bit == AppConstants.HAS_BITFIELD)
                .count();

        // Get the bitfield of the current peer
        int[] peerDataBitfield = peerData.getBitfield();

        // Set the "hasFile" status for the peer based on the comparison of bitfields
        int hasFile = bits == peerDataBitfield.length ? AppConstants.PEER_HAS_FILE : AppConstants.PEER_HAS_NO_FILE;
        peerIdToDataMap.get(peerConnectionProcessId).setHasFile(hasFile);

        // Update the file status for the peer configuration if the peer has the complete file
        if (bits == peerDataBitfield.length) {
            peerConfiguration.updateFileStatusForPeer();
        }

        // Compare bitfields and send an interest message if needed
        compareBitfieldAndSendInterestMessage(peerConnection.getConnection(), peerDataBitfield, bitfield);
    }

    /**
     * Handles an exit message, terminating the program with a status code of 0.
     */
    public static void handleExitMessage() {
        exit(0);
    }

    /**
     * Sets up the handshake and establishes a connection with a peer.
     *
     * @param connection         The Socket connection to the peer.
     * @param peerConfiguration  The configuration of the current peer.
     * @param message            The handshake message to be logged.
     * @param byteMessage        The handshake message as a byte array.
     * @param connPeerProcessId  The process ID of the connected peer.
     * @param peerId             The process ID of the current peer.
     */
    public static void setHandshakeAndConnection(Socket connection, PeerConfiguration peerConfiguration, String message,
                                                 byte[] byteMessage, int connPeerProcessId, int peerId) {
        // Get the map of peer process IDs to PeerConnection objects
        Map<Integer, PeerConnection> peerIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // Log the handshake message to the file
        peerConfiguration.getLogHandler().printMessageAndLogMessageToFile(message);

        // Create a new PeerConnection with the established Socket connection and peer ID
        PeerConnection peerConnection = new PeerConnection(connection, peerId);

        // Add the PeerConnection to the map of peer process IDs to connections
        peerIdToConnectionsMap.put(connPeerProcessId, peerConnection);

        // Start a new Message thread to handle communication with the connected peer
        new Message(peerConnection, peerConfiguration).start();
    }

    /**
     * Notifies all active connections of the program exit.
     *
     * @param peerConfiguration The configuration of the current peer.
     * @throws IOException If an I/O error occurs while sending exit messages.
     */
    public static void notifyAllConnectionsOfExit(PeerConfiguration peerConfiguration) throws IOException {
        // Get the map of peer process IDs to PeerConnection objects
        Map<Integer, PeerConnection> peerProcessIdToConnectionsMap = peerConfiguration.getPeerProcessIdToConnectionsMap();

        // Iterate through all active connections and send exit messages
        for (PeerConnection currConnection : peerProcessIdToConnectionsMap.values()) {
            sendMessage(currConnection.getConnection(), constructMessage(1, MessageType.EXIT, null));
        }
    }
}
