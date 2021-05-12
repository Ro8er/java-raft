package raft.maple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.maple.Util.JsonTool;
import raft.maple.proto.Log;
import raft.maple.proto.Message;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * @author yilin wang
 * @deprecated
 * @since 1.0
 * <p>
 * This class was used for client test and deprecated since 2021/05/07.
 * @see Client
 */
public class User {
    private volatile int commandIndex = 0;
    private volatile OutputStream output = null;
    private volatile InputStream input = null;
    private String host = "127.0.0.1";
    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);
    private ScheduledExecutorService ses;
    private ExecutorService es;
    private List<Log> entryLogs;
    private JTextField resultTextField;

    public User(JTextField resultTextField) {
        this.resultTextField = resultTextField;
        entryLogs = new ArrayList<>();
        loadfile();
        int port = 0 + (int) (Math.random() * (Config.getNodeNum()));
        this.es = new ThreadPoolExecutor(10,
                10,
                120,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>());
        es.submit(new Runnable() {
            @Override
            public void run() {
                connectTo(port+3030);
            }
        });
        ses = new ScheduledThreadPoolExecutor(3);
    }


    public void connectTo(int port) {
        try {
            LOGGER.info("Try to connect to server {}",port);
            Socket socket = new Socket(host, port);
            output = socket.getOutputStream();
            input = socket.getInputStream();
            LOGGER.info("Connected to " + host + ":" + port);
            write();
            byte[] buf = new byte[1024 * 100];
            int len = 0;
            while ((len = input.read(buf)) != -1) {
                String text = new String(buf, 0, len);
                Message msg = JsonTool.JsonStringToObject(text, Message.class);
                Message.MessageType type = msg.getType();
                if (type == Message.MessageType.USER_REQUEST_RESPONSE) {
                    if (msg.getStatus() == 300) {
                        port = Integer.parseInt(new String(msg.getData()));
                        int finalPort = port;
                        commandIndex--;
                        es.submit(new Runnable() {
                            @Override
                            public void run() {
                                connectTo(finalPort);
                            }
                        });
                        socket.close();
                    }
                    if (msg.getStatus() == 200){
                        System.out.println(new String(msg.getData()));
                        write();

                    }
                    if (msg.getStatus() == 500){
                        System.out.println(new String(msg.getData()) + " failed.");
                        commandIndex--;
                        write();
                    }
                }
            }
        } catch (Exception e){
            LOGGER.warn("Cannot connect to server {}", port);
            int p = 0 + (int) (Math.random() * (Config.getNodeNum()));
            connectTo(p+3030);
        }
    }

    public void write() {
        if (output!=null){
            try {
                Message msg = new Message();
                msg.setType(Message.MessageType.USER_REQUEST);
                List<Log> logs=new ArrayList<>();
                if (commandIndex< entryLogs.size()){
                    logs.add(entryLogs.get(commandIndex));
                }else{
                    Scanner sc=new Scanner(System.in);
                    String str=sc.nextLine();
                    logs.add(parseCommandline(str));
                }
                msg.setLogs(logs);
                System.out.println(msg);
                output.write(JsonTool.ObjectToJsonString(msg).getBytes());
                output.flush();
                commandIndex++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public Log parseCommandline(String str){
        Log log=new Log();
        String[] commandline = str.split(":");
        log.setToken(commandline[0]);
        if (commandline[1].equals("u")) {
            log.setCommandType(Log.OperationType.UPDATE);
            log.setContent(commandline[3].getBytes(StandardCharsets.UTF_8));
        }
        else if (commandline[1].equals("c")) {
            log.setCommandType(Log.OperationType.CREATE);
            log.setContent(commandline[3].getBytes(StandardCharsets.UTF_8));
        }
        else if (commandline[1].equals("d")) {
            log.setCommandType(Log.OperationType.DELETE);
        }
        else if (commandline[1].equals("g")){
            log.setCommandType(Log.OperationType.GET);
        }else{
            System.out.println("invalid command");
            return null;
        }
        log.setObject(commandline[2].getBytes(StandardCharsets.UTF_8));
        return log;
    }

    public void loadfile() {
        InputStream in = null;
        BufferedReader br = null;
        try {
            String path = "./src/main/java/raft/maple/userConmandForTest";
            new File(path).createNewFile();
            in = new FileInputStream(path);
            br = new BufferedReader(new InputStreamReader(in));
            String str = null;
            while ((str = br.readLine()) != null) {
                Log log=parseCommandline(str);
                if (log!=null){
                    entryLogs.add(log);
                }
            }
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

}
