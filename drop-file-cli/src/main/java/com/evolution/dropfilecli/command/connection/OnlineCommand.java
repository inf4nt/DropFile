package com.evolution.dropfilecli.command.connection;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "online",
        description = "Get online connections"
)
public class OnlineCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public OnlineCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        System.out.println("Requesting online connections");
        HttpResponse<String> response = daemonHttpClient.online();
        String body = response.body();
        if (!ObjectUtils.isEmpty(body)) {
            System.out.println(body);
        } else {
            System.out.println("No online connection found");
        }
    }
}
