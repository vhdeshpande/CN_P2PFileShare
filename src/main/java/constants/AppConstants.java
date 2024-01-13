package main.java.constants;

public class AppConstants {
    public static final String PEER_DIRECTORY_NAME = "peer_%s";
    public static final String CONFIG_FILE_REGEX = "\\s+";
    public static final String PEER_CONFIGURATION_FILE_NAME = "PeerInfo.cfg";
    public static final String COMMON_CONFIGURATION_FILE_NAME = "Common.cfg";
    public static final String NUMBER_OF_PREFERRED_NEIGHBORS = "NumberOfPreferredNeighbors";
    public static final String UNCHOKING_INTERVAL = "UnchokingInterval";
    public static final String OPTIMISTIC_UNCHOKING = "OptimisticUnchokingInterval";
    public static final String FILE_NAME = "FileName";
    public static final String FILE_SIZE = "FileSize";
    public static final String PIECE_SIZE = "PieceSize";
    public static final int PEER_HAS_FILE = 1;
    public static final int PEER_HAS_NO_FILE = 0;
    public static final String HANDSHAKE_MESSAGE_HEADER = "P2PFILESHARINGPROJ";
    public static final String HANDSHAKE_MESSAGE_ZERO_BITS = "0000000000";
    public static final String LOG_FILE_PATH = "%s%slog_peer_%s.log";
    public static final String LOG_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final int BYTE_SIZE = 32;
    public static final String USER_DIR = "user.dir";
    public static final  int INT_SIZE_BYTES = 4;
    public static final int MESSAGE_TYPE_SIZE = 1;
    public static final int HAS_BITFIELD = 1;
    public static final int THREAD_POOL_SIZE = 10;
    public static final int MESSAGE_HOLD_PERIOD = 1000;

    public static final int TIME_CONSTANT = 100_000_000;
    public static final long DELAY = 1000L;
}
