package raft.maple;

public class Config {

    /**
     * The basic setting for election timeout in ms;
     * The real election timeout of each node varies randomly when the algorithm is running and sitting in a range of
     * {electionTimeout + [0,electionTimeout)}
     */
    private static final int electionTimeout = 5000; //ms (basic setting for election timeout, it varies)

    private static final int heartbeat = 1000; //ms

    private static final int coreThreadNum = 20;

    private static final int maxThreadNum = 20;

    private static final int nodeNum = 3;

    private static final String host= "127.0.0.1";

    private static final String dataDir = "./src/main/java/raft/maple/storage/entry";

    private static final int clientRequestTimeout = 10000; //ms

    public static int getClientRequestTimeout() {
        return clientRequestTimeout;
    }

    public static String getDataDir() {
        return dataDir;
    }

    public static int getCoreThreadNum() {
        return coreThreadNum;
    }

    public static int getMaxThreadNum() {
        return maxThreadNum;
    }

    public static int getElectionTimeout() {
        return electionTimeout;
    }

    public static int getHeartbeat() {
        return heartbeat;
    }

    public static int getNodeNum() {
        return nodeNum;
    }

    public static String getHost() {
        return host;
    }
}
