package raft.maple.proto;

import java.util.List;

public class AppendEntryResponseBuilder implements  MessageBuilder{
    private Message msg;

    public AppendEntryResponseBuilder(Boolean voteGranted, int myId, long appendSize){
        msg=new Message(Message.MessageType.APPEND_ENTRY_RESPONSE, myId);
        msg.setVoteGrant(voteGranted);
        msg.setLogCopiedSize(appendSize);
    }

    @Override
    public void setTerm(long currentTerm) {

    }

    @Override
    public void setLastLogTerm(long lastLogTerm) {

    }

    @Override
    public void setLastLogIndex(long lastLogIndex) {

    }

    @Override
    public void setLastAppliedIndex(long lastAppliedIndex) {

    }

    @Override
    public void setData(byte[] data) {

    }

    @Override
    public void setVoteGrant(Boolean bool) {
        msg.setVoteGrant(bool);

    }

    @Override
    public void setLogs(List<Log> logs) {

    }

    @Override
    public void setStatus(int statusCode) {

    }

    @Override
    public void setLocalId(int messagefrom) {

    }

    @Override
    public void setLeaderCommitIndex(long leaderCommitId) {

    }

    @Override
    public Message create() {
        return msg;
    }
}
