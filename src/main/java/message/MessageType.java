package main.java.message;

/**
 * Enum representing different types of messages exchanged between peers.
 */
public enum MessageType {
    EXIT,
    CHOKE,
    UNCHOKE,
    INTERESTED,
    NOT_INTERESTED,
    HAVE,
    BITFIELD,
    REQUEST,
    PIECE,
    DONE,
    UNKNOWN;

    /**
     * Converts a byte to a MessageType.
     *
     * @param msg The byte representation of the message type.
     * @return The corresponding MessageType.
     */
    public static MessageType getMessageTypeFromByte(char msg) {
        switch (msg) {
            case '0':
                return EXIT;
            case '1':
                return CHOKE;
            case '2':
                return UNCHOKE;
            case '3':
                return INTERESTED;
            case '4':
                return NOT_INTERESTED;
            case '5':
                return HAVE;
            case '6':
                return BITFIELD;
            case '7':
                return REQUEST;
            case '8':
                return PIECE;
            case '9':
                return DONE;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Converts a MessageType to a byte.
     *
     * @param messageType The MessageType to be converted.
     * @return The byte representation of the MessageType.
     */
    public static byte getByteFromMessageType(MessageType messageType) {
        switch (messageType) {
            case EXIT:
                return (byte) '0';
            case CHOKE:
                return (byte) '1';
            case UNCHOKE:
                return (byte) '2';
            case INTERESTED:
                return (byte) '3';
            case NOT_INTERESTED:
                return (byte) '4';
            case HAVE:
                return (byte) '5';
            case BITFIELD:
                return (byte) '6';
            case REQUEST:
                return (byte) '7';
            case PIECE:
                return (byte) '8';
            case DONE:
                return (byte) '9';
        }
        return 0;
    }
}
