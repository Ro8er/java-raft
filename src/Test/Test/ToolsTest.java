package Test;

import raft.maple.Util.JsonTool;

import org.junit.Test;
import raft.maple.proto.Message;
import raft.maple.proto.MessageBuilder;
import raft.maple.proto.VoteRequestBuilder;

import java.io.File;

public class ToolsTest {


    @Test
    public void testObjToStr(){
        MessageBuilder builder= new VoteRequestBuilder(1);
        builder.setTerm(10);
        builder.setLastLogTerm(1);
        builder.setLastLogIndex(2);
        Message msg=builder.create();
        String str=JsonTool.ObjectToJsonString(msg);
        System.out.println(str);
    }

    @Test
    public void testStrToObj() throws ClassNotFoundException {
        MessageBuilder builder= new VoteRequestBuilder(1);
        builder.setTerm(10);
        builder.setLastLogTerm(1);
        builder.setLastLogIndex(2);
        Message msg=builder.create();
        String str=JsonTool.ObjectToJsonString(msg);

        Message msg1=JsonTool.JsonStringToObject(new String(str.getBytes()),Message.class);
        System.out.println(msg1);
        System.out.println(System.getProperty("user.dir"));

    }







}
