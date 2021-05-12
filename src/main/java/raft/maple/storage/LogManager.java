package raft.maple.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.maple.Util.JsonTool;
import raft.maple.proto.Log;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * This class manage the logList in memory.
 */
public class LogManager {

    /**
     * loglist stores the cached log of the node
     * Note: some logs in this list may not be committed now and may be removed in the future
     *
     * @since 1.0
     */
    private List<Log> logList = Collections.synchronizedList(new ArrayList<>());

    /**
     * the log index started to record in the logList, this attribute will be useful if we split data in several files
     * in the future.(when we have to do segmentation for large scale data)
     * <p>
     * Note in this project, we don't have log0, the first log is 1.
     *
     * @since 1.0
     */
    private long startRecordIndex = 1;

    /**
     * tokenSet helps server to avoid the repeated user requests.
     *
     * @since 1.0
     */
    private Map<String, Log> tokenSet = new ConcurrentHashMap<>();

    private static Logger LOGGER = LoggerFactory.getLogger(LogManager.class);


    /**
     * Sometimes we will load the logs from a file segment with a specific startIndex,
     * the index of logList cannot match the logIndex, so this method will directly
     * return the corresponding log with logIndex.
     *
     * @param logIndex the real index of log in the whole statemachine
     * @return the log of that logIndex, if the logIndex is smaller than the startIndex of this Manager,
     * will return null.
     * @since 1.0
     */
    public Log getLogByIndex(long logIndex) {
        if (logIndex - startRecordIndex < 0) {
            return null;
        }
        return getLogList().get((int) (logIndex - startRecordIndex));
    }

    /**
     * Load logs from disk if we have committed logs.
     *
     * @param filepath   log file path to load
     * @param startIndex where should be read, not useful in current version, in the future we could append this index to the filename
     * @since 1.0
     */
    public synchronized void loadLogHistory(String filepath, int startIndex) {
        InputStream in = null;
        BufferedReader br = null;
        startRecordIndex = startIndex;
        try {
            new File(filepath).createNewFile();
            in = new FileInputStream(filepath);
            br = new BufferedReader(new InputStreamReader(in));
            String str;
            while ((str = br.readLine()) != null) {
                Log log = JsonTool.JsonStringToObject(str, Log.class);
                tokenSet.put(log.getToken(), log);
                getLogList().add(log);
            }
            LOGGER.info("Load logs from disk since logindex {}", startIndex);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * write committed logs to file
     *
     * @param path file path to write
     * @param log  the log to write
     * @since 1.0
     */
    public synchronized void logPersistence(String path, Log log) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(JsonTool.ObjectToJsonString(log));
            log.setCommited(true);
            out.newLine();
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove logs from some logIndex.
     * <p>
     * Note the given index will not be removed
     *
     * @param index
     * @since 1.0
     */
    public synchronized void removeLogsFromIndex(long index) { //exclude index
        index = index + 1 - startRecordIndex;
        long size = getLogList().size() - 1;
        for (long k = size; k >= index; k--) {
            getLogList().remove(k - 1);
        }
    }

    /**
     * copy logs to a list from some logIndex
     * Note this index will note be copied
     *
     * @param index
     * @return logList be copied
     * @since 1.0
     */
    public synchronized List<Log> copyLogsFromIndex(long index) { //exclude index
        List<Log> logs = new ArrayList<>();
        if (index < 0) {
            LOGGER.warn("over the capacity of it");
        }
        for (long k = index; k < getLogList().size(); k++) {
            logs.add(getLogByIndex(k + 1));
        }
        return logs;
    }

    /**
     * @return the last log's term in this log list
     * @since 1.0
     */
    public long getLastLogTerm() {
        if (getLogList().size() == 0) {
            return 0;
        } else {
            return getLogList().get(getLogList().size() - 1).getLogTerm();
        }
    }

    /**
     * @return the last log's index in this log list
     * @since 1.0
     */
    public long getLastLogIndex() {
        if (getLogList().size() == 0) {
            return 0;
        } else {
            return getLogList().get(getLogList().size() - 1).getLogIndex();
        }
    }

    /**
     * @param token
     * @return true if token is in the logList, false if token is not in the logList
     * @since 1.0
     */
    public boolean containsToken(String token) {
        return tokenSet.keySet().contains(token);
    }

    /**
     * For leader: Add logs after the last log once reacive a non-repeated writing request from client
     *
     * @param logs       logs to append
     * @param sinceIndex the logIndex that already matched with leader's
     * @since 1.0
     */
    public synchronized void appendNewEntries(List<Log> logs, long sinceIndex) {
        removeLogsFromIndex(sinceIndex);
        for (Log log : logs) {
            getLogList().add(log);
            tokenSet.put(log.getToken(), log);
            System.out.println(log);
        }
    }

    /**
     * For client: Add logs after the last log where the node can match with leader's
     *
     * @param logs       logs to append
     * @param sinceIndex the logIndex that already matched with leader's
     * @param leaderId   leader's id
     * @since 1.0
     */
    public synchronized void appendNewEntries(List<Log> logs, long sinceIndex, int leaderId) {
        removeLogsFromIndex(sinceIndex);
        for (Log log : logs) {
            getLogList().add(log);
            tokenSet.put(log.getToken(), log);
            log.increaseVoteGranted(leaderId); // follower should record this log has been voted by leader already
            System.out.println(log);
        }
    }

    public List<Log> getLogList() {
        return logList;
    }

    public void setLogList(List<Log> logList) {
        this.logList = logList;
    }

    public long getStartRecordIndex() {
        return startRecordIndex;
    }

    public void setStartRecordIndex(int startRecordIndex) {
        this.startRecordIndex = startRecordIndex;
    }

    public Map<String, Log> getTokenSet() {
        return tokenSet;
    }

    public void setTokenSet(Map<String, Log> tokenSet) {
        this.tokenSet = tokenSet;
    }

}
