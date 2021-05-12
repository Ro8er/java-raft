package raft.maple;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.maple.Util.JsonTool;
import raft.maple.proto.Log;
import raft.maple.proto.Message;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author yilin wang <yiliwang@student.unimelb.edu.au>
 * @version 1.0
 * @since 1.0
 * <p>
 * The University of Melbourne COMP900020 Assignment created in 2021/04:
 * This class provide methods for a client to submit their request to the distributed database.
 */
public class Client{

    private String host = "127.0.0.1";
    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);
    private ScheduledExecutorService ses;
    private ExecutorService es;
    private volatile String token;
    private volatile OutputStream output;
    private volatile  InputStream input;
    private volatile  Socket socket;
    private Map<String, Log> cachedLog= new ConcurrentHashMap<>();
    private JTextArea resultTextArea;
    private ScheduledFuture toScheduled;
    private volatile int port;


    public Client (){
        ses = new ScheduledThreadPoolExecutor(3);
        connectTo(getRandomServerPort());
    }

    public Client(JTextArea resultTextArea) {
        this.resultTextArea = resultTextArea;
        ses = new ScheduledThreadPoolExecutor(3);
        connectTo(getRandomServerPort());
    }

    private int getRandomServerPort(){
        return 0 + (int) (Math.random() * (Config.getNodeNum()))+3030;
    }



    public void connectTo(int port) {
        try {

            this.port = port;
            LOGGER.info("Try to connect to server {}",port);
            resultTextArea.append("Try to connect to server " + port + "\n");
            if (socket!=null){
                try{
                    input.close();
                    output.close();
                    socket.close();
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            };
            socket = new Socket(host, port);
            output = socket.getOutputStream();
            input = socket.getInputStream();
            LOGGER.info("Connected to " + host + ":" + port);
            resultTextArea.append("Connected to " + host + ":" + port + "\n");
        } catch (Exception e){
            LOGGER.warn("Cannot connect to server {}", port);
            resultTextArea.append("Cannot connect to server " + port + "\n");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            connectTo(getRandomServerPort());
        }
    }


    public void listen(Log log){

        byte[] buf = new byte[1024 * 100];
        int len = 0;
        boolean retry=false;
        boolean timeout=false;
        long currentTime = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis()-currentTime)%1000==0){
                System.out.println(System.currentTimeMillis()-currentTime);
            }
            if (System.currentTimeMillis()-currentTime>= Config.getClientRequestTimeout()+1){
                timeout=true;
                break;
            }

            try {
                if (!((len = input.read(buf)) != -1)){
                    continue;
                };
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
                resultTextArea.append("inputstream not available \n");
                connectTo(getRandomServerPort());
                write(log);
                listen(log);
                break;
            }
            if (toScheduled != null && !toScheduled.isDone()) {
                toScheduled.cancel(true);
            }
            String text = new String(buf, 0, len);
            Message msg = JsonTool.JsonStringToObject(text, Message.class);
            Message.MessageType type = msg.getType();
            if (type == Message.MessageType.USER_REQUEST_RESPONSE) {
                if (msg.getStatus() == 300) {
                    int port = Integer.parseInt(new String(msg.getData()));
                    resultTextArea.append("Request denied because leader is server " + port + "\n");
                    int finalPort = port;
                    connectTo(finalPort);
                    write(log);
                    listen(log);
                    break;

                }
                if (msg.getStatus() == 200){
                    System.out.println(new String(msg.getData()));
                    if (log.getCommandType() == Log.OperationType.GET){
                        resultTextArea.append("query result for "+new String(log.getObject())+" is " +new String(msg.getData()) + "\n");
                        System.out.println("query result is "+new String(msg.getData()));
                        cachedLog.remove(msg.getToken());
                    }else{
                        resultTextArea.append("Operation with token "+new String(msg.getData()).substring(0,10) + " successfully\n");
                        System.out.println("Operation with token "+ new String(msg.getData()).substring(0,10) +" done.");
                        cachedLog.remove(new String(msg.getData()));
                    }
                    break;
                }
                if (msg.getStatus() == 500){
                    System.out.println(new String(msg.getData()) + " failed.");
                    resultTextArea.append("Operation with token "+new String(msg.getData()) + " failed.\n");
                    write(log); // redo
                    retry=true;
                    break;
                }
            }
        }
        if (timeout){
            resultTextArea.append("request timeout, server may crash.\n");
            System.out.println("request timeout.");
            try {
                Thread.sleep(Config.getElectionTimeout());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectTo((port +1 )%Config.getNodeNum() + 3030 );
            write(log);
            retry=true;

        }
        if (retry){
            listen(log);
        }
    }


    public void create(String key, String value) {

        Log log=new Log();
        log.setCommandType(Log.OperationType.CREATE);
        log.setObject(key.getBytes(StandardCharsets.UTF_8));
        log.setContent(value.getBytes(StandardCharsets.UTF_8));
        log.setToken(createRandomToken(50));
        cachedLog.put(log.getToken(),log);
        write(log);
        listen(log);

    }

    public void update(String key, String value) {
        Log log=new Log();
        log.setCommandType(Log.OperationType.UPDATE);
        log.setObject(key.getBytes(StandardCharsets.UTF_8));
        log.setContent(value.getBytes(StandardCharsets.UTF_8));
        log.setToken(createRandomToken(50));
        cachedLog.put(log.getToken(),log);
        write(log);
        listen(log);

    }

    public void delete(String key) {
        Log log=new Log();
        log.setCommandType(Log.OperationType.DELETE);
        log.setObject(key.getBytes(StandardCharsets.UTF_8));
        log.setToken(createRandomToken(50));
        cachedLog.put(log.getToken(),log);
        write(log);
        listen(log);

    }

    public static String createRandomToken(int length){
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public void get(String key) {

        Log log=new Log();
        log.setCommandType(Log.OperationType.GET);
        log.setObject(key.getBytes(StandardCharsets.UTF_8));
        log.setToken(createRandomToken(50));
        cachedLog.put(log.getToken(),log);
        write(log);
        listen(log);

    }

    public void write(Log log) {
        if (output!=null){
            try {
                Message msg = new Message();
                msg.setType(Message.MessageType.USER_REQUEST);
                List<Log> logs=new ArrayList<>();
                logs.add(log);
                msg.setLogs(logs);
                System.out.println(msg);
                //请求操作为msg
                resultTextArea.append(msg.toString() + "\n");
                output.write(JsonTool.ObjectToJsonString(msg).getBytes());
                output.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}

