package raft.maple.socket;

import raft.maple.core.RaftNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;


/**
 * @author kim, yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * This class is where creating socket. And the role is a client socket.
 */
public class RPClient{
    private Socket socket = null;
    private int desIndex;
    private String host;
    private int port;
    private static final Logger LOGGER = LoggerFactory.getLogger(RPClient.class);
    private Endpoint endpoint;
    private RaftNode raftNode;

    public RPClient(String host, int port, int index, RaftNode node) {
        this.host = host;
        this.port = port;
        this.desIndex = index;
        this.raftNode = node;
        try {
            LOGGER.info("Try to connect to server {}",desIndex);
            socket = new Socket(host, port);
            this.endpoint=new Endpoint(socket,desIndex,node);
            this.raftNode.getEs().submit(()->{endpoint.listen();});
            LOGGER.info("Connected to "+host+":"+port);
        } catch (IOException e) {
            endpoint = null;
            LOGGER.warn("Cannot connect to server {}",desIndex);
        }
    }

    public Endpoint getEndpoint(){
        return endpoint;
    }

    public void closeConnection(){
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.warn("RPC client to {} cannot close.",desIndex);
        }
    }

    public void sendMessage(String msg){
        if (msg!=null){
            endpoint.send(msg);
        }
    }
}
