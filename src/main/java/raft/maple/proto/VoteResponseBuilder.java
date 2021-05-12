package raft.maple.proto;

import java.util.List;

public class VoteResponseBuilder implements MessageBuilder {
    private Message msg;

    public VoteResponseBuilder(int localId) {
        msg = new Message(Message.MessageType.VOTE_RESPONSE, localId);
    }

    @Override
    public void setTerm(long currentTerm) {
        msg.setCurrentTerm(currentTerm);
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
