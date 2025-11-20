package com.evolution.dropfiledaemon.old;

import com.evolution.dropfiledaemon.old.node.NodeHttpClient;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/daemon")
public class FileOperationRestController {

    private final NodeHttpClient nodeHttpClient;

    private final ConnectionSession connectionSession;

    @Autowired
    public FileOperationRestController(NodeHttpClient nodeHttpClient, ConnectionSession connectionSession) {
        this.nodeHttpClient = nodeHttpClient;
        this.connectionSession = connectionSession;
    }

    @GetMapping("/files")
    public String files(@RequestParam String filePath) {
        URI connection = connectionSession.getConnection();

        return nodeHttpClient.getFiles(connection, filePath).body();
    }

    @SneakyThrows
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String filePath) {
        URI connection = connectionSession.getConnection();
        HttpResponse<InputStream> download = nodeHttpClient.download(connection, filePath);

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
        Map<String, List<String>> headers = download.headers().map();
        headers.forEach((key, values) -> values.forEach(value -> bodyBuilder.header(key, value)));

        return bodyBuilder.body(new InputStreamResource(download.body()));
    }
}
