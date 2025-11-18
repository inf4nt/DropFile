package com.evolution.dropfilecli.command.connect;

import picocli.CommandLine;

@CommandLine.Command(
        name = "connect",
        description = "Connect to remote peer"
)
public class ConnectCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "IP address")
    private String ip;

    @Override
    public void run() {
        System.out.println("connected " + ip);
    }
}