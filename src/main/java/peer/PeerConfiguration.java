package main.java.peer;

import main.java.log.LogHandler;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Peer Process Configuration Setup
 */
public class PeerConfiguration {
    private int peerProcessId;
    private Map<Integer, PeerData> peerIdToDataMap;
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int chunkSize;
    private final AtomicInteger numberOfPeerHavingFile;
    private Map<Integer, PeerConnection> peerIdToConnectionsMap;
    private LogHandler logHandler;
    private File directory;
    private byte[][] chunks;

    public int getPeerProcessId() {
        return peerProcessId;
    }

    public void setPeerProcessId(int peerProcessId) {
        this.peerProcessId = peerProcessId;
    }

    public LogHandler getLogHandler() {
        return logHandler;
    }

    public void setLogHandler(LogHandler logHandler) {
        this.logHandler = logHandler;
    }

    public Map<Integer, PeerData> getPeerIdToDataMap() {
        return peerIdToDataMap;
    }

    public void setPeerIdToDataMap(Map<Integer, PeerData> peerIdToDataMap) {
        this.peerIdToDataMap = peerIdToDataMap;
    }

    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public void setNumberOfPreferredNeighbors(int numberOfPreferredNeighbors) {
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public void setUnchokingInterval(int unchokingInterval) {
        this.unchokingInterval = unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public PeerConfiguration(){
        numberOfPeerHavingFile = new AtomicInteger(0);
    }

    public int getNumberOfPeerHavingFile(){
        return numberOfPeerHavingFile.get();
    }

    public void updateFileStatusForPeer(){
        int numberOfPeersHavingFile;
        do {
            numberOfPeersHavingFile = getNumberOfPeerHavingFile();
        } while (!numberOfPeerHavingFile.compareAndSet(numberOfPeersHavingFile, numberOfPeersHavingFile + 1));
    }

    public Map<Integer, PeerConnection> getPeerProcessIdToConnectionsMap() {
        return peerIdToConnectionsMap;
    }

    public void setConnections(Map<Integer, PeerConnection> connections) {
        this.peerIdToConnectionsMap = connections;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public byte[][] getChunks() {
        return chunks;
    }

    public void setChunks(byte[][] chunks) {
        this.chunks = chunks;
    }

}
