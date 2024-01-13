package main.java.constants;

public class DisplayConstants {
    public static final String INIT_PEER_MESSAGE = "Initiating Peer Process %s";
    public static final String MAKE_CONNECTION_MESSAGE = "Peer %s makes a connection to Peer %s.";
    public static final String CONNECTED_MESSAGE = "Peer %s is connected from Peer %s.";
    public static final String PREFERRED_NEIGHBORS_MESSAGE = "Peer %s has the preferred neighbors%s";
    public static final String OPTIMISTIC_UNCHOKE_NEIGHBOR_MESSAGE = "Peer %s has the optimistically unchoked neighbor %s.";
    public static final String UNCHOKED_MESSAGE = "Peer %s is unchoked by %s.";
    public static final String CHOKED_MESSAGE = "Peer %s is choked by %s.";
    public static final String HAVE_MESSAGE = "Peer %s received the 'have' message from %s for the piece %s.";
    public static final String INTERESTED_MESSAGE = "Peer %s received the 'interested' message to %s.";
    public static final String NOT_INTERESTED_MESSAGE = "Peer %s received the 'not interested' message from %s.";
    public static final String DOWNLOAD_CHUNK_MESSAGE = "Peer %s has downloaded the piece %s from %s. Now the number of pieces it has is %s.";
    public static final String DOWNLOAD_COMPLETE_MESSAGE = "Peer %s has downloaded the complete file.";
}
