package com.evolution.dropfilecli.old.command;

import com.evolution.dropfilecli.old.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
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
        if  (ObjectUtils.isEmpty(body)) {
            System.out.println("No connected nodes found");
        } else {
            System.out.println("Connected nodes");
            System.out.println(body);
        }
    }
}
