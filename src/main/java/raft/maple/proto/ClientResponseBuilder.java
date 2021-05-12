package raft.maple.proto;

import java.util.List;

public class ClientResponseBuilder implements MessageBuilder{

    private Message msg;

    public ClientResponseBuilder(int id){
        msg = new Message(Message.MessageType.USER_REQUEST_RESPONSE, id);
    }
    @Override
    public void setTerm(long currentTerm) {

    }

    @Override
    public void setLastLogTerm(long lastLogTerm) {

    }

    public  void setToken(String token){
        msg.setToken(token);
    }


    @Override
    public void setLastLogIndex(long lastLogIndex) {

    }

    @Override
    public void setLastAppliedIndex(long lastAppliedIndex) {

    }

    @Override
    public void setData(byte[] data) {
        msg.setData(data);
    }

    @Override
    public void setVoteGrant(Boolean bool) {

    }

    @Override
    public void setLogs(List<Log> logs) {

    }

    @Override
    public void setStatus(int statusCode) {
        msg.setStatus(statusCode);
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
