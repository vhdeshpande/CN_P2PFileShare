package main.java.peer;

import java.net.Socket;


public class PeerConnection {
    // Fields
    private final Socket connection;          // The socket representing the connection with the peer
    private boolean isInterested;             // Flag indicating if the peer is interested
    private boolean isChoked;                 // Flag indicating if the peer is choked
    private boolean isOptimisticallyUnchoked; // Flag indicating if the peer is optimistically unchoked
    private double rate;                       // Data transfer rate with the peer
    private final int peerProcessId;          // Unique identifier for the peer process

    // Constructor
    public PeerConnection(Socket connection, int peerProcessId) {
        // Initializing default values
        isChoked = true;
        rate = 0;

        // Setting provided values
        this.connection = connection;
        this.peerProcessId = peerProcessId;
    }

    // Getter and Setter methods

    /**
     * Gets the data transfer rate with the peer.
     *
     * @return The data transfer rate.
     */
    public double getRate() {
        return rate;
    }

    /**
     * Sets the data transfer rate with the peer.
     *
     * @param rate The new data transfer rate.
     */
    public void setRate(double rate) {
        this.rate = rate;
    }

    /**
     * Gets the optimistically unchoked status of the peer.
     *
     * @return True if optimistically unchoked, false otherwise.
     */
    public boolean getIsOptimisticallyUnchoked() {
        return isOptimisticallyUnchoked;
    }

    /**
     * Sets the optimistically unchoked status of the peer.
     *
     * @param isOptimisticallyUnchoked The new optimistically unchoked status.
     */
    public void setIsOptimisticallyUnchoked(Boolean isOptimisticallyUnchoked) {
        this.isOptimisticallyUnchoked = isOptimisticallyUnchoked;
    }

    /**
     * Gets the interested status of the peer.
     *
     * @return True if interested, false otherwise.
     */
    public boolean getIsInterested() {
        return isInterested;
    }

    /**
     * Sets the interested status of the peer.
     *
     * @param isInterested The new interested status.
     */
    public void setIsInterested(Boolean isInterested) {
        this.isInterested = isInterested;
    }

    /**
     * Gets the choked status of the peer.
     *
     * @return True if choked, false otherwise.
     */
    public boolean getIsChoked() {
        return isChoked;
    }

    /**
     * Sets the choked status of the peer.
     *
     * @param isChoked The new choked status.
     */
    public void setIsChoked(Boolean isChoked) {
        this.isChoked = isChoked;
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
     * Gets the socket representing the connection with the peer.
     *
     * @return The connection socket.
     */
    public Socket getConnection() {
        return connection;
    }
}