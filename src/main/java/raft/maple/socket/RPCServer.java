package raft.maple.socket;

import raft.maple.core.RaftNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author kim, yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * This class is where creating socket. And the role is a server socket.
 */
public class RPCServer {
    private ServerSocket serverSocket = null;
    private ArrayList<ServerSocket> serverSockets = new ArrayList<>();
    private List<Endpoint> acceptedEndpoints = new ArrayList<>();
    private Socket clientSocket = null;
    private int port;
    private OutputStream output = null;
    private int localIndex;
    private RaftNode raftNode;
    private static final Logger LOGGER = LoggerFactory.getLogger(RPCServer.class);

    public RPCServer(int port, int index, RaftNode raftNode) {
        try {
            this.port = port;
            this.localIndex = index;
            this.raftNode = raftNode;
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.warn("Server cannot use port {} to set up a socket", port);
        }
    }

    public void startRPCServer() {
        try {
            LOGGER.info("RaftNode-" + localIndex + " started a server at port {}.", port);
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                clientSocket = serverSocket.accept();
                Endpoint endpoint = new Endpoint(clientSocket, localIndex, raftNode);
                acceptedEndpoints.add(endpoint);
                ExecutorService es = raftNode.getEs();
                es.submit(() -> endpoint.listen());
                System.out.println(clientSocket.getInetAddress().getHostAddress() + clientSocket.getPort() + " SUCCESS TO CONNECT...");
            }
        } catch (IOException e) {
            LOGGER.warn("Server {} crashed", localIndex);
        }
    }
}
