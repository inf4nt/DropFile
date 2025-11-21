package com.evolution.dropfiledaemon.node;

import com.evolution.dropfile.common.dto.NodeConnectionsConnectionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class NodeClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public NodeClient(HttpClient httpClient,
                      ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public HttpResponse<String> ping(URI nodeURI) throws IOException, InterruptedException {
        URI nodeConnectionURI = nodeURI.resolve("/node/ping");
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(nodeConnectionURI)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<Void> connect(Integer port, URI nodeURI) throws IOException, InterruptedException {
        URI nodeConnectionURI = nodeURI.resolve("/node/connections/connect");

        NodeConnectionsConnectionDTO nodeConnectionsConnectionDTO = new NodeConnectionsConnectionDTO(port);
        String jsonBody = objectMapper.writeValueAsString(nodeConnectionsConnectionDTO);

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(nodeConnectionURI)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
