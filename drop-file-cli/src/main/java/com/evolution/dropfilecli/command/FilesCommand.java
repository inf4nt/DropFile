package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "files",
        description = "Get files"
)
public class FilesCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @CommandLine.Parameters(index = "0", description = "File path")
    private String filePath;

    @Autowired
    public FilesCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        String body = daemonHttpClient.getFiles(filePath).body();
        System.out.println(body);
    }
}
