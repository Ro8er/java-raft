package raft.maple.proto;

import java.util.Arrays;
import java.util.HashSet;

public class Log {
    private Long logIndex;
    private Long logTerm;
    private Boolean isCommited;
    private OperationType commandType;
    private byte[] object;
    private byte[] content;
    private HashSet<Integer> set= new HashSet<>();
    private int voteGranted=1;
    private String token;


    public enum OperationType{
        UPDATE,
        CREATE,
        DELETE,
        GET,
    }

    public int getVoteGranted() {
        return voteGranted;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int increaseVoteGranted(int voteId) {
        set.add(voteId);
        this.voteGranted = 1+set.size();
        return voteGranted;
    }

    public void setVoteGranted(int voteGranted) {
        this.voteGranted = voteGranted;
    }

    public Long getLogIndex() {
        return logIndex;
    }

    public void setLogIndex(Long logIndex) {
        this.logIndex = logIndex;
    }

    public Long getLogTerm() {
        return logTerm;
    }

    public void setLogTerm(Long logTerm) {
        this.logTerm = logTerm;
    }

    public Boolean getCommited() {
        return isCommited;
    }

    public void setCommited(Boolean commited) {
        isCommited = commited;
    }

    public OperationType getCommandType() {
        return commandType;
    }

    public void setCommandType(OperationType commandType) {
        this.commandType = commandType;
    }

    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }



    @Override
    public String toString() {
        return "Log{" +
                "logIndex=" + logIndex +
                ", logTerm=" + logTerm +
                ", isCommited=" + isCommited +
                ", commandType=" + commandType +
                ", object=" + Arrays.toString(object) +
                ", content=" + Arrays.toString(content) +
                ", set=" + set +
                ", voteGranted=" + voteGranted +
                ", token='" + token + '\'' +
                '}';
    }
}
