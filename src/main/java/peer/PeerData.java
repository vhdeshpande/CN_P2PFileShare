package main.java.peer;

/*
 * PeerData class represents data associated with a peer in a peer-to-peer network.
 */
public class PeerData {
    private final int peerProcessId; // Unique identifier for the peer process
    private final String host;       // Hostname or IP address of the peer
    private final int portNumber;    // Port number for communication with the peer
    private int hasFile;              // Flag indicating if the peer has the complete file
    private int chunkCount = 0;       // Count of chunks held by the peer
    private int[] bitfield;           // Bitfield representing the availability of chunks

    // Constructor
    public PeerData(int peerId, String host, int portNumber, int hasFile) {
        this.peerProcessId = peerId;
        this.portNumber = portNumber;
        this.host = host;
        this.hasFile = hasFile;
    }

    // Getter and Setter methods

    /**
     * Gets the count of chunks held by the peer.
     *
     * @return The count of chunks.
     */
    public int getChunkCount() {
        return chunkCount;
    }

    /**
     * Sets the count of chunks held by the peer.
     *
     * @param chunkCount The new count of chunks.
     */
    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    /**
     * Gets the unique identifier of the peer process.
     *
     * @return The peer process identifier.
     */
    public int getPeerProcessId() {
        return peerProcessId;
    }

    /**
     * Gets the hostname or IP address of the peer.
     *
     * @return The host information.
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port number for communication with the peer.
     *
     * @return The port number.
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Gets the flag indicating if the peer has the complete file.
     *
     * @return 1 if the peer has the file, 0 otherwise.
     */
    public int getHasFile() {
        return hasFile;
    }

    /**
     * Sets the flag indicating if the peer has the complete file.
     *
     * @param hasFile The new value of the hasFile flag (1 if the peer has the file, 0 otherwise).
     */
    public void setHasFile(int hasFile) {
        this.hasFile = hasFile;
    }

    /**
     * Gets the bitfield representing the availability of chunks.
     *
     * @return The bitfield array.
     */
    public int[] getBitfield() {
        return bitfield;
    }

    /**
     * Sets the bitfield representing the availability of chunks.
     *
     * @param bitfield The new bitfield array.
     */
    public void setBitfield(int[] bitfield) {
        this.bitfield = bitfield;
    }
}
