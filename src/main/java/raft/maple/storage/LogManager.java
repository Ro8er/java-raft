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

    private List<Log> logList = Collections.synchronizedList(new ArrayList<>());
    private long startRecordIndex = 1;
    private static Logger LOGGER = LoggerFactory.getLogger(LogManager.class);
    private Map<String, Log> tokenSet = new ConcurrentHashMap<>();

    public long getStartRecordIndex() {
        return startRecordIndex;
    }

    public void setStartRecordIndex(int startRecordIndex) {
        this.startRecordIndex = startRecordIndex;
    }

    public Log getLogByIndex(long logIndex) {
        if (logIndex - startRecordIndex < 0) {
            return null;
        }
        return getLogList().get((int) (logIndex - startRecordIndex));
    }

    public Map<String, Log> getTokenSet() {
        return tokenSet;
    }

    public void setTokenSet(Map<String, Log> tokenSet) {
        this.tokenSet = tokenSet;
    }

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
                tokenSet.put(log.getToken(),log);
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

    public synchronized void removeLogsFromIndex(long index) { //exclude index
        index = index + 1 - startRecordIndex;
        long size = getLogList().size() - 1;
        for (long k = size; k >= index; k--) {
            getLogList().remove(k - 1);
        }
    }

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

    public long getLastLogTerm() {
        if (getLogList().size() == 0) {
            return 0;
        } else {
            return getLogList().get(getLogList().size() - 1).getLogTerm();
        }
    }

    public long getLastLogIndex() {
        if (getLogList().size() == 0) {
            return 0;
        } else {
            return getLogList().get(getLogList().size() - 1).getLogIndex();
        }
    }

    public boolean containsToken(String token){
        return tokenSet.keySet().contains(token);
    }

    public synchronized void appendNewEntries(List<Log> logs, long sinceIndex) {
        removeLogsFromIndex(sinceIndex);
        for (Log log : logs) {
            getLogList().add(log);
            tokenSet.put(log.getToken(), log);
            System.out.println(log);
        }
    }

    public synchronized void appendNewEntries(List<Log> logs, long sinceIndex, int leaderId) {
        removeLogsFromIndex(sinceIndex);
        for (Log log : logs) {
            getLogList().add(log);
            tokenSet.put(log.getToken(),log);
            log.increaseVoteGranted(leaderId);
            System.out.println(log);
        }
    }

    public List<Log> getLogList() {
        return logList;
    }

    public void setLogList(List<Log> logList) {
        this.logList = logList;
    }
}
