# CNT5106C Compter Networks: Peer to Peer File Sharing Application

## Implementation

Main peer instance:
- `peerProcess`:
  - Initiates the peer process setup, serving as the entry point for execution. Every peer runs this process.
  
Models:
- `peerData`:
    - Holds information about the in accordance to PeerInfo configuration - process ID, host, port number and file status.
- `peerConnection`:
    - Holds information about the connection setup for the peer - socket connection, choke-unchoke status and download information.
- `peerConfiguration`:
   - Consists of peer configuration, file configuration, connection status information about connected peers and neighbor configuration.
   - It also consists instances in reference to peer, message and logging handlers for the peer instance.

Handlers
- `PeerHandler`:
    - Responsible for managing all utility methods for the peer like parsing configurations (Common.cfg and PeerInfo.cfg), setting up peer configuration, and handling peer file operations.
- `MessageHandler`:
    - Responsible for managing all utility methods for handling messages - message setup, construction and processing received messages.
- `LogHandler`:
    - Responsible for managing all utility methods for creating logs for the peer instance.
- `ChokeUnchokeHandler`:
    - Responsible for managing the scheduler for the choke and unchoke mechanisms.
- `OptimisticUnchokeHandler`:
  - Responsible for managing the scheduler for optimistic unchoke in accordance with the configuration.

## Steps to run

- Copy the project code to the CISE machine.
- Ensure the configuration files are added to the same directory - PeerInfo.cfg and Common.cfg.
- According to the configuration, the directory of the node having the file should be present in the same directory in the format `peer_<peerId>` and store the file in this folder.
- For all the other nodes which don't have a file, no need to create a directory.
- Commands to build the jar:
``` 
make
```
- Start each peer process.
- Command to run each peer process:
``` 
java -jar peerProcess.jar <peer_id>
```

## Working


- Our program begins from the peerProcess class's `main()` method. The `peerId` is passed to this main() method as a command line parameter.
- A PeerHandler object is created and its `setupAndStartPeerProcess()` is called. In this method, the `Common.cfg` and `PeerInfo.cfg` are read and PeerConfiguration object is created and set according to these values. `PeerConfiguration` object represents all the parameters needed by the current peer process like its pID, information about all of its peers, a map containing connections to all the connected peers and its current file chunks.
- The peer's bitfield is calculated and set and if the peer has the file, it is chopped into pieces.
- A `Sender` thread is started which sends a connection request to connect to all the peers that started before the current peer. It exchanges handshake and bitfield messages with the peer and adds the other peer's information to its map in `setHandshakeAndConnection()`. It then starts a Message thread and listens to incoming messages from the other peer. Depending on the type of the message, the message is directed to the correct method and processed accordingly.
- A `Receiver` thread is started which keeps a ServerSocket alive in the background to listen for connection requests sent by peers started after the current peer. If the peer receives a connection request, it exchanges handshake and bitfield messages and adds the other peer's information to its peerIdToConnectionsMap. It then goes into the same infinite Message thread to listen to incoming messages.
- `ChokeUnchokeHandler` is a background thread which functions as the peer unchoke scheduler that runs periodically in the background after every 'UnchokingInterval' seconds. The top 'NumberOfPreferredNeighbors(N)' interested peers are calculated based on the download rate and they are unchoked. The other peers are choked. In case the current peer has the file, N neighbors are randomly unchoked.
- `OptimisticUnchokeHandler` is a background thread which functions as the optimistic peer unchoke scheduler that runs periodically in the background after every 'OptimisticUnchokingInterval' seconds and unchokes a random interested neighbor.
- Once a peer has the complete file, it combines the pieces and writes out the file to disk in `PeerHandler.updatePeerFileStatus()`.
- Once the number of completed peers becomes equal to the peers in PeerInfo.cfg, the peer sends an exit message to all the other peers and terminates.
