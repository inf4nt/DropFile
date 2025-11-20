package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ConnectionsOnline;
import com.evolution.dropfiledaemon.ConnectionStore;
import com.evolution.dropfiledaemon.exception.ConnectionFacadeException;
import com.evolution.dropfiledaemon.node.NodeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class ConnectionsFacade {

    @Value("${server.port:8080}")
    private Integer serverPort;

    private final NodeClient nodeClient;

    private final ConnectionStore connectionStore;

    public ConnectionsFacade(NodeClient nodeClient, ConnectionStore connectionStore) {
        this.nodeClient = nodeClient;
        this.connectionStore = connectionStore;
    }

    public ConnectionsOnline getOnlineConnections() {
        List<ConnectionsOnline.Online> onlineList = connectionStore.getConnections().entrySet().stream()
                .map(it -> {
                    String connectionId = it.getKey();
                    String connectionAddress = it.getValue();
                    URI addressNodeURI = CommonUtils.toURI(connectionAddress);
                    try {
                        HttpResponse<String> httpResponse = nodeClient.ping(addressNodeURI);
                        if (httpResponse.statusCode() == 200) {
                            return new ConnectionsOnline.Online(ConnectionsOnline.OnlineStatus.ONLINE, connectionId, connectionAddress);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new ConnectionsOnline.Online(ConnectionsOnline.OnlineStatus.OFFLINE, connectionId, connectionAddress);
                })
                .toList();
        return new ConnectionsOnline(onlineList);
    }

    public String connect(String address) {
        URI nodeURI = CommonUtils.toURI(address);
        try {
            HttpResponse<Void> httpResponse = nodeClient.connect(serverPort, nodeURI);
            if (httpResponse.statusCode() == 200) {
                return connectionStore.addConnection(address);
            } else {
                throw new ConnectionFacadeException(String.format("Failed to connect to node. Http status %s", httpResponse));
            }
        } catch (Exception e) {
            throw new ConnectionFacadeException(e.getMessage(), e);
        }
    }
}
