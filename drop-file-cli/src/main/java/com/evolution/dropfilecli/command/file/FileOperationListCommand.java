package com.evolution.dropfilecli.command.file;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "list", description = "List available files")
public class FileOperationListCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @CommandLine.Parameters(index = "0", description = "File path")
    private String filePath;

    @Autowired
    public FileOperationListCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        String body = daemonHttpClient.getFiles(filePath).body();
        System.out.println(body);
    }
}
