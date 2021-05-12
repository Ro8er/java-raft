package raft.maple.proto;

import java.util.List;

public interface MessageBuilder {
    void setTerm(long currentTerm);

    void setLastLogTerm(long lastLogTerm);

    void setLastLogIndex(long lastLogIndex);

    void setLastAppliedIndex(long lastAppliedIndex);

    void setData(byte[] data);

    void setVoteGrant(Boolean bool);

    void setLogs(List<Log> logs);

    void setStatus(int statusCode);

    void setLocalId(int messagefrom);

    void setLeaderCommitIndex(long leaderCommitIndex);

    Message create();

}
