package raft.maple.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import raft.maple.Config;
import raft.maple.Util.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.maple.proto.*;
import raft.maple.socket.RPClient;
import raft.maple.storage.LogManager;

import javax.swing.*;

/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.1
 * @since 1.1
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * core.RaftNode is the core class in the raft algorithm, simulating the function of "node" concept in a distributed
 * system. Its main functions include the storage and the manipulation of the node's data, as well as handling the
 * requests/responses to/from other nodes.
 */
public class RaftNode {

    /**
     * In raft algorithm, each node will play as one of the following roles{follower, candidate, leader} at different
     * stages.
     */
    public enum NodeState {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    static final Logger LOGGER = LoggerFactory.getLogger(RaftNode.class);

    /**
     * State attribute indicates the current state of the current server node.
     * At the start, all the nodes should maintain the follower state till whose scheduled election is triggered.
     * Then all the node running an election would be in the candidate state.
     * Once a leader is elected, leader's turns to the leader state, others should become followers.
     *
     * @see NodeState
     * @since 1.0
     */
    private volatile NodeState state = NodeState.FOLLOWER;

    /**
     * CurrentTerm indicates the term the node sitting in.
     * In each term, at most one of the nodes in the cluster may gain majority votes.
     * <p>
     * The term can grow only in three cases: case1 the node himself start a new election; case2 receive a candidate
     * vote request with a higher term, case3 receive leader's appendEntryRequest(heartbeat) with higher term number.
     * The term may decrease in some corner scenario if a leader's heartbeat in the last term was delayed but reached
     * when the node itself just started and increased his new term. Note it's a rare case.
     *
     * @since 1.0
     */
    private volatile long currentTerm;

    /**
     * The last index the node received to commit in the system.
     *
     * @since 1.0
     */
    private volatile long commitIndex;

    /**
     * The last applied index to state machine, should catch up with commit index.
     *
     * @since 1.0
     */
    private volatile long lastAppliedIndex;

    /**
     * LOCAL_ID indicates the unique id for this node in the cluster. In the current version, the cluster will index
     * nodes from 0.
     * And The Port number will be 3030 + node's id. eg: localId is 0,the port used is 3030.
     *
     * @since 1.0
     */
    private final int LOCAL_ID;

    /**
     * leader_id records the current leader in the current term once a leader is elected.
     * <p>
     * In the current design, if a client send a write request and the node is not the leader, he will send the leader id
     * back to the client. And then the client will reconnect to the leader.
     *
     * @since 1.0
     */
    private volatile int leaderId;

    /**
     * voteTo indicates the server id that the node voteTo for the current term. Only vote once for each term.
     *
     * @since 1.0
     */
    private volatile int voteTo = -1;

    /**
     * A voteGrantedNum map is used to record peer's vote result for each election term that the node ran.
     * Once the vote collected is enough, the node should attempt to be a leader.
     *
     * @since 1.0
     */
    private Map<Long, Integer> voteGrantedNum = new ConcurrentHashMap<>();

    /**
     * The peerMap will be available only if the node became the leader.
     * With this map, the leader should record the preIndex and MatchIndex for each peer so that in next
     * appendEntriesRequest, the leader could ask peers to synchronize leader's logList.
     *
     * @since 1.0
     */
    private Map<Integer, Peer> peerMap = new ConcurrentHashMap<>();

    /**
     * logPath is where the persistence form of all committed logs be located.
     *
     * @since 1.0
     */
    private String logPath;

    /**
     * stateMachine in this project is a distributed in-memory key-value database.
     *
     * @see StateMachine
     * @see MapStateMachine
     * @since 1.0
     */
    private StateMachine stateMachine;

    /**
     * myLogManager manages all the cached entries(uncommitted/pre-commit logs) and committed logs.
     * Its function mainly helps raft node to make decision based on raft rules.
     *
     * @see LogManager
     * @since 1.0
     */
    private LogManager myLogManager;

    /**
     * ioManager is used to manage all the network i/o based on socket communication.
     *
     * @see IOManager
     * @since 1.0
     */
    private IOManager ioManager;

    /**
     * ses is a scheduled thread pool which is used to schedule two types of tasks in raft: starting an election and
     * sending heatbeat namely (appendEntriesRequest)
     *
     * @since 1.0
     */
    private ScheduledExecutorService ses;

    /**
     * es is a thread pool for the raft node to use multi-threads dealing with i/o communication.
     *
     * @since 1.0
     */
    private ExecutorService es;

    /**
     * indicates the current scheduled election in the waiting queue. Can be cancel in several conditions.
     *
     * @since 1.0
     */
    private ScheduledFuture electionScheduled;

    /**
     * indicates the current scheduled heartbeat in the waiting queue. Can be cancel if a stepDown occurs.
     *
     * @since 1.0
     */
    private ScheduledFuture hbScheduled;

    private Lock lock = new ReentrantLock();

    /**
     * For GUI. since 1.0 displays back-end infos.
     */
    private JTextArea logPrintTextArea;

    /**
     * For GUI.since 1.0 displays current term.
     */
    private JTextArea termInfoTextArea;

    /**
     * for GUI. since 1.0 displays leader id.
     */
    private JTextArea leaderInfoTextArea;

    /**
     * for GUI. since 1.0 displays current state.
     */
    private JTextArea stateInfoTextArea;

    /**
     * for GUI. since 1.0 displays the current statemachine.
     */
    private JTextArea stateMachineTextArea;


    /**
     * Constructor with index from user to decide this node's id in the cluster
     *
     * @param index
     * @deprecated
     */
    public RaftNode(int index) {
        this.LOCAL_ID = index;
        logPath = Config.getDataDir() + "log" + LOCAL_ID;
        this.init();
    }

    /**
     * Constructor with index from user to decide this node's id in the cluster and GUI frameworks.
     *
     * @param index
     * @param logPrintTextArea
     * @param nodeInfo
     */
    public RaftNode(int index, JTextArea logPrintTextArea, ArrayList<JTextArea> nodeInfo) {
        this.LOCAL_ID = index;
        logPath = Config.getDataDir() + "log" + LOCAL_ID;
        this.logPrintTextArea = logPrintTextArea;
        this.termInfoTextArea = nodeInfo.get(0);
        this.leaderInfoTextArea = nodeInfo.get(1);
        this.stateInfoTextArea = nodeInfo.get(2);
        this.stateMachineTextArea = nodeInfo.get(3);
        stateInfoTextArea.setText(state.toString());
        this.init();
    }

    /**
     * Initialise the raftNode. If the node have already done some log persistence, it will firstly
     * reload the logs from disk and apply them to statemachine and also get the term, commitIndex,
     * lastAppliedIndex. Besides, in this project we set a periodic snapshot for statemachine in the
     * init stage. An election timer will be triggered in the end.
     *
     * @since 1.0
     */
    public void init() {
        stateMachine = new MapStateMachine();
        myLogManager = new LogManager();
        myLogManager.loadLogHistory(logPath, 1);
        for (Log log : myLogManager.getLogList()) {
            stateMachine.applyLog(log);
            stateMachineTextArea.setText(stateMachine.toString());
        }
        ;
        commitIndex = getLastLogIndex();
        lastAppliedIndex = getLastLogIndex();
        currentTerm = getLastLogTerm();
        termInfoTextArea.setText(String.valueOf(currentTerm));
        this.es = new ThreadPoolExecutor(Config.getCoreThreadNum(),
                Config.getMaxThreadNum(),
                120,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>());
        this.ses = new ScheduledThreadPoolExecutor(3);
        this.ioManager = new IOManager(LOCAL_ID, this);

        ses.scheduleWithFixedDelay(() -> {
            stateMachine.print();
        }, 30, 10, TimeUnit.SECONDS);  //periodic task
        resetElection();
    }

    /**
     * Reset the election timer. Cancel the last-time timer and schedule a new one. The timer's time is between
     * a T to 2T value according to raft. @see Config electionTimeout;
     * It will be called in the initialization stage or when receiving a heartbeat from leader.
     *
     * @since 1.0
     */
    private void resetElection() {
        if (electionScheduled != null && !electionScheduled.isDone()) {
            electionScheduled.cancel(true);
        }
        electionScheduled = ses.schedule(() -> {
            kickOffAnElection();
        }, randomTimeoutMS(), TimeUnit.MILLISECONDS);
    }

    /**
     * Compute a T to 2T time value based on electionTimeout in config.
     *
     * @return random value for the timer to set in milliseconds.
     * @since 1.0
     */
    private int randomTimeoutMS() {
        ThreadLocalRandom generator = ThreadLocalRandom.current();
        int randomTimeoutMS = Config.getElectionTimeout()
                + generator.nextInt(0, Config.getElectionTimeout());
        LOGGER.info("The election timeout for the current term is after {} ms ", randomTimeoutMS);
        return randomTimeoutMS;
    }

    /**
     * Start an election, increase term immediately, vote to node itself, broadcast vote requests to other peer
     * in the cluster, and finally, reset a new election timer.
     *
     * @since 1.0
     */
    private void kickOffAnElection() {
        lock.lock();
        currentTerm++;
        termInfoTextArea.setText(String.valueOf(currentTerm));
        voteGrantedNum.put(currentTerm, 1);
        LOGGER.info("Start a Leader election in term {}", currentTerm);
        logPrintTextArea.append("Start a Leader election in term " + currentTerm + "\n");
        state = NodeState.CANDIDATE;
        stateInfoTextArea.setText(state.toString());
        voteTo = LOCAL_ID;
        lock.unlock();
        for (int id = 0; id < Config.getNodeNum(); id++) {
            if (id == LOCAL_ID) {
                continue;
            }
            int destination = id;
            es.submit(new Runnable() {
                @Override
                public void run() {
                    voteRequest(destination);
                }
            });
        }
        resetElection();
    }

    /**
     * Send the vote request to a peer.
     * The vote request should include local_id, current-term, lastLogTerm and lastLogIndex in the log list,
     *
     * @param destinationId the peerId to send.
     * @since 1.0
     */
    private void voteRequest(int destinationId) {
        RPClient rpClient = ioManager.getRPClientById(destinationId);
        LOGGER.info("vote request to server {}", destinationId);
        logPrintTextArea.append("vote request to server " + destinationId + "\n");
        lock.lock();
        MessageBuilder builder = new VoteRequestBuilder(LOCAL_ID);
        builder.setTerm(currentTerm);
        builder.setLastLogTerm(getLastLogTerm());
        builder.setLastLogIndex(getLastLogIndex());
        lock.unlock();
        Message msg = builder.create();
        String voteRequest = JsonTool.ObjectToJsonString(msg);
        if (rpClient != null) { // in case the server failed and the rpClient became null @see Endpoint exception
            rpClient.sendMessage(voteRequest);
        }
    }

    /**
     * Once receive a vote request from others, do a response based on raft rules.
     * rule1: deny if sender's lastLogTerm < receiver's local lastLogTerm
     * rule2: deny if sender's lastLogTerm = receiver's local lastLogTerm and sender's lastLogIndex < local lastLogIndex
     * rule3: deny if sender's term < = receiver's currentTerm because sry bro I've already voted.
     *
     * @param voteRequest
     * @return voteRequestResponse In this implementation, we simply return null if vote denied and that will not send a
     * response actually. Otherwise a response with term and local id infos should be sent back in json string format.
     * @since 1.0
     */
    public String voteRequestResponse(Message voteRequest) {
        String voteRequestResponse = null;
        LOGGER.info("receive voteRequest from server {}", voteRequest.getLocalId());
        logPrintTextArea.append("receive voteRequest from server " + voteRequest.getLocalId() + "\n");
        lock.lock();
        if (getLastLogTerm() > voteRequest.getLastLogTerm()) {
            LOGGER.info("Ignore voteRequest from server {}, his lastlogterm is stale.", voteRequest.getLocalId());
            logPrintTextArea.append("Ignore voteRequest from server " + voteRequest.getLocalId() + ", his lastlogterm is stale." + "\n");
        } else if (getLastLogTerm() == voteRequest.getLastLogTerm() && getLastLogIndex() > voteRequest.getLastLogIndex()) {
            LOGGER.info("Ignore voteRequest from server {}, his lastlogindex is stale.", voteRequest.getLocalId());
            logPrintTextArea.append("Ignore voteRequest from server " + voteRequest.getLocalId() + ", his lastlogindex is stale." + "\n");
        } else if (currentTerm >= voteRequest.getCurrentTerm()) {
            LOGGER.info("Ignore voteRequest from server {}, I voted already in term {}.", voteRequest.getLocalId(), voteRequest.getCurrentTerm());
            logPrintTextArea.append("Ignore voteRequest from server " + voteRequest.getLocalId() + ", I voted already in term " + voteRequest.getCurrentTerm() + "\n");
        } else {
            currentTerm = voteRequest.getCurrentTerm();
            termInfoTextArea.setText(String.valueOf(currentTerm));
            voteTo = voteRequest.getLocalId();
            logPrintTextArea.append("I vote to " + voteTo + " in term " + voteRequest.getCurrentTerm().toString() + "\n");
            MessageBuilder voteResponseBuilder = new VoteResponseBuilder(LOCAL_ID);
            voteResponseBuilder.setVoteGrant(true);
            voteResponseBuilder.setTerm(getCurrentTerm());
            voteRequestResponse = JsonTool.ObjectToJsonString(voteResponseBuilder.create());
        }
        lock.unlock();
        return voteRequestResponse;
    }

    /**
     * Callback for voteRequest. Candidates will compute if he gets enough votes in his current election term.
     * Note deprecated vote will be ignored.
     *
     * @param voteResponse vote request response from peers, actually only those who agree will send this response.
     * @since 1.0
     */
    public synchronized void processVoteResponse(Message voteResponse) {
        if (voteResponse.getCurrentTerm() < currentTerm || state != NodeState.CANDIDATE) {
            LOGGER.info("ignore this vote.");
            logPrintTextArea.append("ignore this vote." + "\n");
            return;
        } else {
            if (voteResponse.getVoteGrant()) {
                LOGGER.info("Vote from server {} for term {}", voteResponse.getLocalId(), voteResponse.getCurrentTerm());
                logPrintTextArea.append("Vote from server " + voteResponse.getLocalId() + " for term " + voteResponse.getCurrentTerm() + "\n");
                int votes = voteGrantedNum.getOrDefault(voteResponse.getCurrentTerm(), 0);
                voteGrantedNum.put(voteResponse.getCurrentTerm(), votes + 1);
                LOGGER.info("{} Votes granted in term {}", voteGrantedNum.get(voteResponse.getCurrentTerm()), voteResponse.getCurrentTerm());
                logPrintTextArea.append(voteGrantedNum.get(voteResponse.getCurrentTerm()) + " Votes granted in term " + voteResponse.getCurrentTerm() + "\n");
                if (votes + 1 > Config.getNodeNum() / 2 && state != NodeState.LEADER) {
                    LOGGER.info("server {} become leader who got majority vote in term {}", LOCAL_ID, currentTerm);
                    logPrintTextArea.append("server " + LOCAL_ID + " become leader who got majority vote in term " + currentTerm + "\n");
                    beAnLeader();
                }
            } else {
                LOGGER.warn("This should not happen, vote denied should not be sent from server.");
            }
        }
    }

    /**
     * A candidate get enough votes then he will turn his state to leader, cancel scheduled election, and
     * set up the peer map for peers. At start. each peer's next index is leader's lastLogIndex + 1.
     * Also, leader should schedule next time heartbeat.
     *
     * @since 1.0
     */
    private synchronized void beAnLeader() {
        if (state == NodeState.LEADER) {
            LOGGER.info("ur already a leader bro");
            return;
        }
        if (electionScheduled != null && !electionScheduled.isDone()) {
            electionScheduled.cancel(true);
            System.out.println("cancel next election");
        }
        state = NodeState.LEADER;
        stateInfoTextArea.setText(state.toString());
        leaderId = LOCAL_ID;
        leaderInfoTextArea.setText(String.valueOf(leaderId));
        for (int id = 0; id < Config.getNodeNum(); id++) {
            if (id == LOCAL_ID) {
                continue;
            }
            if (!peerMap.containsKey(id)) {
                Peer peer = new Peer();
                peer.setId(id);
                peer.setNextIndex(getLastLogIndex() + 1);
                peerMap.put(id, peer);
            }
        }
        resetHeartbeat();
    }

    /**
     * For leader node resets the scheduled heatBeat.
     *
     * @since 1.0
     */
    private void resetHeartbeat() {
        if (hbScheduled != null && !hbScheduled.isDone()) {
            hbScheduled.cancel(true);
        }
        hbScheduled = ses.schedule(() -> {
            startHeartbeat();
        }, Config.getHeartbeat(), TimeUnit.MILLISECONDS);
    }

    /**
     * A heartBeat is actually a appendEntryRequest. They can be equivalent in raft algorithm.
     * Just broadcast appendEntryRequest to other peers and reset next time heartbeat.
     *
     * @since 1.0
     */
    private void startHeartbeat() {
        LOGGER.info("start sending heartbeat");
        logPrintTextArea.append("start sending heartbeat" + "\n");
        for (int id = 0; id < Config.getNodeNum(); id++) {
            if (id == LOCAL_ID) {
                continue;
            }
            int destination = id;
            es.submit(new Runnable() {
                @Override
                public void run() {
                    appendEntryRequest(destination);
                }
            });
        }
        resetHeartbeat();
    }

    /**
     * For nodes processing the heartBeat information. This is a sub-method called by appendEntryResponse.
     *
     * @param msg it's actually the appendEntryRequest message from leader.
     *            Becasue only leader can send a heartbeat, when a candidate or a leader receive a heartbeat then he should
     *            consider if his leader state went stale.
     *            rule1: whether i am a leader or a candidate or a follower, some guy send a hb with a higher term, I will follow him.
     *            rule2: i am a candidate (current Term >= sender's term), i will step down and follow the sender.
     *            rule3; my term> sender's term, ignore it.
     *            rule4: I am a leader, myterm = sender's term, but i do not vote to myself, i should step down.
     *            <p>
     *            note#1: I am a leader and i receive a hb with a term = mine will not happen in raft.
     *            note#2: note#1 is wrong, an update of rule4 since 1.1 work. I am a leader and I vote to a new candidate,
     *                    because my hb somehow delays in the network, then my term = sender's term, but i vote to the sender.
     *                    I should step down.         
     * @since 1.1
     */
    private void processHb(Message msg) {
        LOGGER.info("receive hb from server {}, myterm {}, his {}", msg.getLocalId(), currentTerm, msg.getCurrentTerm());
        logPrintTextArea.append("receive hb from server " + msg.getLocalId() + ", myterm " + currentTerm + ", his " + msg.getCurrentTerm() + "\n");
        if (msg.getCurrentTerm() == currentTerm && state == NodeState.FOLLOWER) {
            leaderId = msg.getLocalId();
            leaderInfoTextArea.setText(String.valueOf(leaderId));
            resetElection();
        }
        if (msg.getCurrentTerm() > currentTerm || state == NodeState.CANDIDATE || (state == NodeState.LEADER && voteTo != LOCAL_ID)) { //case: i am leader in old term , but a new leader elected
            LOGGER.info("There is a leader now so I should follow him ,and even step down.");
            logPrintTextArea.append("There is a leader now so I should follow him,and even step down.." + "\n");
            stepDown(msg.getCurrentTerm(), msg.getLocalId());
            resetElection();
        }
    }

    /**
     * Just synchronize with leader's term. And be a follower state. Cancel my scheduled hb if there is one.
     *
     * @param leaderTerm leader term
     * @param leader     leader id
     * @since 1.0
     */
    private void stepDown(long leaderTerm, int leader) {
        if (currentTerm > leaderTerm && state == NodeState.LEADER) {
            LOGGER.warn("it is not possible.");
            logPrintTextArea.append("it is not possible." + "\n");
            return;
        }
        ioManager.clearRPCients();
        currentTerm = leaderTerm;
        termInfoTextArea.setText(String.valueOf(currentTerm));
        voteTo = leaderId;
        leaderId = leader;
        leaderInfoTextArea.setText(String.valueOf(leaderId));
        state = NodeState.FOLLOWER;
        stateInfoTextArea.setText(state.toString());
        LOGGER.info("server {} become leader in term {}.", leaderId, currentTerm);
        logPrintTextArea.append("server " + leaderId + " become leader in term " + currentTerm + "\n");
        if (hbScheduled != null && !hbScheduled.isDone()) {
            hbScheduled.cancel(true);
        }
    }

    /**
     * Give a proper response to user's request following majority vote idea.
     * Apart from Raft, I added the idea of token to avoid duplicated requests.
     * <p>
     * rule1: only leader can do writing-type tasks. reply 300 if i am not leader and it is not a GET request.
     * rule2: if token is already contained and the corresponding entry has been committed, directly reply to client
     * rule2 will be used if a leader crashed and the client re-request the same operation to new leader.
     * rule3: Ask other peer to append the new entry, if get majority agreements, commit it and reply to client with 200
     * else reply 500 indicating failed.
     *
     * @param msg client's request
     * @return message to clients with different info in different cases.
     * 300 and leader's port if i cannot do this task.
     * 200 and query result for GET request if complete the task successfully
     * 200 and operation's token for other request if do the task successfully
     * 500 and operation's token if client request timeout and leader does not receive enough agreements
     * @since 1.0
     */
    public String userRequestResponse(Message msg) {
        LOGGER.info("receive a user request.");
        logPrintTextArea.append("receive a user request." + "\n");
        List<Log> logs = msg.getLogs();
        Log log = logs.get(0);//only contain 1 log for user
        ClientResponseBuilder replyToClient = new ClientResponseBuilder(LOCAL_ID);
        if (state != NodeState.LEADER && log.getCommandType() != Log.OperationType.GET) {
            LOGGER.info("I am not leader, I cannot do this task.");
            logPrintTextArea.append("I am not leader, I cannot do this task." + "\n");
            replyToClient.setStatus(300);
            int port = leaderId + 3030;
            replyToClient.setData(String.valueOf(port).getBytes(StandardCharsets.UTF_8));
            return JsonTool.ObjectToJsonString(replyToClient.create());
        }
        if (logs.get(0).getCommandType() == Log.OperationType.GET) {
            lock.lock();
            replyToClient.setStatus(200);
            if (stateMachine.get(log.getObject()) == null) {
                replyToClient.setData("not found".getBytes(StandardCharsets.UTF_8));
                LOGGER.info("query result is: not found.");
                logPrintTextArea.append("query result is:  not found. \n");
            } else {
                replyToClient.setData(stateMachine.get(log.getObject()));
                LOGGER.info("query result is: " + new String(stateMachine.get(log.getObject())));
                logPrintTextArea.append("query result is: " + new String(stateMachine.get(log.getObject())) + "\n");
            }
            replyToClient.setToken(log.getToken());
            lock.unlock();
            return JsonTool.ObjectToJsonString(replyToClient.create());
        } else {
            lock.lock();
            if (myLogManager.containsToken(log.getToken())) { //duplicate requests cuz sometimes leader crashed client repost the request to new leader
                log = myLogManager.getTokenSet().get(log.getToken());
                if (log.getLogIndex() <= commitIndex) {
                    LOGGER.info("Duplicate request, and  the request already committed.");
                    logPrintTextArea.append("Duplicate request, and  the request already committed." + "\n");
                    replyToClient.setStatus(200);
                    replyToClient.setData(log.getToken().getBytes(StandardCharsets.UTF_8));
                    replyToClient.setToken(log.getToken());
                    return JsonTool.ObjectToJsonString(replyToClient.create());
                }
                LOGGER.info("Duplicate request, let me ask others");
                logPrintTextArea.append("Duplicate request,, let me ask others." + "\n");
            } else {
                long lastLogIndex = getLastLogIndex();
                log.setLogIndex(lastLogIndex + 1);
                log.setLogTerm(currentTerm);
                myLogManager.appendNewEntries(logs, lastLogIndex);
            }
            LOGGER.info("I will wait for the preCommit result");
            logPrintTextArea.append("I will wait for the preCommit result" + "\n");
            try {
                long currentTime = System.currentTimeMillis();
                while (log.getVoteGranted() <= Config.getNodeNum() / 2 && System.currentTimeMillis() - currentTime < Config.getClientRequestTimeout()) {
                    long wait = System.currentTimeMillis() - currentTime;
                    if (wait % 1000 == 0) {
                        LOGGER.info("time passed by {} ms", wait);
                        logPrintTextArea.append("waiting for " + wait + " ms.\n");
                    }
                }
                logPrintTextArea.append("waiting ends." + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (log.getVoteGranted() > Config.getNodeNum() / 2) {
                replyToClient.setStatus(200);
                replyToClient.setData(log.getToken().getBytes(StandardCharsets.UTF_8));
            } else {
                replyToClient.setStatus(500);
                replyToClient.setData(log.getToken().getBytes(StandardCharsets.UTF_8));
            }
            lock.unlock();
            replyToClient.setToken(log.getToken());
            return JsonTool.ObjectToJsonString(replyToClient.create());
        }
    }

    /**
     * In leader's heartbeat, sending appendEntryRequest to peers.
     * The message include leader's current term, leader's commit index, lastLogIndex(peermap's nextIndex-1),
     * lastLogTerm, logs after peer's nextIndex.
     *
     * @param destination peer's id
     * @since 1.0
     */
    private void appendEntryRequest(int destination) {
        RPClient rpClient = ioManager.getRPClientById(destination);
        long preLogIndex = peerMap.get(destination).getNextIndex() - 1;
        long preLogTerm = preLogIndex == 0 ? 0 : myLogManager.getLogByIndex(preLogIndex).getLogTerm();
        MessageBuilder appendEntryBuilder = new AppendEntryRequestBuilder(LOCAL_ID);
        appendEntryBuilder.setLastLogIndex(preLogIndex);
        appendEntryBuilder.setLastLogTerm(preLogTerm);
        appendEntryBuilder.setLeaderCommitIndex(commitIndex);
        appendEntryBuilder.setTerm(currentTerm);
        appendEntryBuilder.setLogs(myLogManager.copyLogsFromIndex(preLogIndex));
        String msg = JsonTool.ObjectToJsonString(appendEntryBuilder.create());
        if (rpClient != null) { // in case the server failed and the rpClient became null @see Endpoint exception
            rpClient.sendMessage(msg);
        }
    }

    /**
     * Process leader's appendEntryRequest. We will call processHb @see processHb.
     * <p>
     * So if my lastLogTerm and LastLogIndex cannot match leader's then I deny this;
     * otherwise, it matched and i synchronized logs from the matched index.
     *
     * @param msg leader's appendEntryRequest
     * @return my response agree or not
     * @since 1.0
     */
    public String appendEntryResponse(Message msg) {
        processHb(msg);
        Log log = msg.getLogs().size() == 0 ? null : msg.getLogs().get(0); //hb
        System.out.println(log);
        if (msg.getLeaderCommitIndex() > commitIndex) {
            this.commitIndex = Math.min(msg.getLeaderCommitIndex(), myLogManager.getLogList().size());
            es.submit(() -> {
                forwardCommit();
            });
        }
        if (msg.getCurrentTerm() < this.currentTerm || myLogManager.getLogList().size() + myLogManager.getStartRecordIndex() - 1 < msg.getLastLogIndex()
                || (myLogManager.getLogByIndex(msg.getLastLogIndex()) != null && myLogManager.getLogByIndex(msg.getLastLogIndex()).getLogTerm() != msg.getLastLogTerm())) { //<= then my last log index<leader's
            if (log != null) {
                LOGGER.info("Deny Append Log Request for log term{} index {}", log.getLogTerm(), log.getLogIndex());
                logPrintTextArea.append("Deny Append Log Request for log term" + log.getLogTerm() + " index " + log.getLogIndex() + "\n");
            } else {
                LOGGER.info("cannot match my preLogIndex and preLogTerm with leader's peermap");
                logPrintTextArea.append("cannot match my preLogIndex and preLogTerm with leader's peermap" + "\n");
            }
            Message reply = new AppendEntryResponseBuilder(false, LOCAL_ID, 0).create();
            return JsonTool.ObjectToJsonString(reply);
        } else {
            if (log == null) {
                LOGGER.info("Already matched my preLogIndex and preLogTerm with leader's");
                logPrintTextArea.append("Already matched my preLogIndex and preLogTerm with leader's" + "\n");
                Message reply = new AppendEntryResponseBuilder(true, LOCAL_ID, msg.getLogs().size()).create();
                return JsonTool.ObjectToJsonString(reply);
            }
            es.submit(() -> {
                myLogManager.appendNewEntries(msg.getLogs(), msg.getLastLogIndex(), leaderId);
            });
            LOGGER.info("Agree Append Log Request for log term{} index {}", log.getLogTerm(), log.getLogIndex());
            logPrintTextArea.append("Agree Append Log Request for log term" + log.getLogTerm() + " index " + log.getLogIndex() + "\n");
            Message reply = new AppendEntryResponseBuilder(true, LOCAL_ID, msg.getLogs().size()).create();
            return JsonTool.ObjectToJsonString(reply);
        }
    }

    /**
     * updates peerMap according to peer's response, if he denied we will decrease his nextIndex.
     * if he grant it, then leader will count the agreements to decide if he should commit the log.
     *
     * @param msg peer's response to appendEntryReqeust
     * @since 1.0
     */
    public synchronized void processAppendEntryResponse(Message msg) {
        Peer peer = peerMap.get(msg.getLocalId());
        if (msg.getVoteGrant()) {
            long oldNextIndex = peer.getNextIndex();
            peer.setMatchIndex(peer.getNextIndex() - 1 + msg.getLogCopiedSize());
            peer.setNextIndex(peer.getMatchIndex() + 1);
            for (long i = oldNextIndex - 1; i < oldNextIndex + msg.getLogCopiedSize(); i++) {
                int votes = myLogManager.getLogByIndex(i).increaseVoteGranted(msg.getLocalId());
                if (votes > Config.getNodeNum() / 2) {
                    commitIndex = Math.max(i, commitIndex);
                }
            }
            es.submit(() -> {
                forwardCommit();
            });
        } else {
            peer.setNextIndex(peer.getNextIndex() - 1);
        }
    }

    /**
     * Commit all logs before commitIndex
     *
     * @since 1.0
     */
    public synchronized void forwardCommit() {
        long commit = commitIndex;
        for (long i = lastAppliedIndex + 1; i <= commit; i++) {
            stateMachine.applyLog(myLogManager.getLogByIndex(i));
            stateMachineTextArea.setText(stateMachine.toString());
            lastAppliedIndex++;
            myLogManager.logPersistence(logPath, myLogManager.getLogByIndex(i));
        }
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public int getLocalId() {
        return LOCAL_ID;
    }

    public ExecutorService getEs() {
        return es;
    }


    public IOManager getIoManager() {
        return ioManager;
    }

    public long getLastLogTerm() {
        return myLogManager.getLastLogTerm();
    }

    public long getLastLogIndex() {
        return myLogManager.getLastLogIndex();
    }
}

