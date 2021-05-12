package GUI;

import raft.maple.Config;
import raft.maple.core.RaftNode;

import javax.swing.*;
import java.util.ArrayList;

public class application extends JFrame{
    private JPanel rootPanel;
    private JLabel nodeindex;
    private JTextArea logPrintTextArea;
    private JTextArea termInfoTextArea;
    private JTextArea leaderInfoTextArea;
    private JTextArea stateInfoTextArea;
    private JTextArea stateMachineTextArea;
    private JLabel number;
    private int index = 0;
    private ArrayList<JTextArea> nodeInfo = new ArrayList<>();


    public application(int index) {
        this.index = index;
        initialization();
        add(rootPanel);
        setSize(700,500);
        new RaftNode(index, logPrintTextArea, nodeInfo);
    }

    public void initialization(){
        nodeindex.setText("this is node " + index);
        number.setText(String.valueOf(Config.getNodeNum()));
        nodeInfo.add(termInfoTextArea);
        nodeInfo.add(leaderInfoTextArea);
        nodeInfo.add(stateInfoTextArea);
        nodeInfo.add(stateMachineTextArea);
    }
}
