package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "node",
        description = "List of nodes"
)
public class NodesCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public NodesCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        HttpResponse<String> httpResponse = daemonHttpClient.getNodes();
        String body = httpResponse.body();
        System.out.println(body);
    }
}
