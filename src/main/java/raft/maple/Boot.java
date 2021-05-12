package raft.maple;

import GUI.gui;
import org.apache.commons.cli.*;

public class Boot {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("index", true, "node index, an integer");
        options.addOption("c", false, "select client mode");
        options.addOption("s", false, "select server mode");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (cmd.hasOption("c")) {
            gui.callUser();
        } else {
            if (cmd.hasOption("index")) {
                try {
                    int index = Integer.parseInt(cmd.getOptionValue("index"));
                    gui.callApp(index);
                } catch (NumberFormatException e) {
                    System.out.println("-index requires a port number, parsed: " + cmd.getOptionValue("index"));
                }
            }

        }
    }
}
