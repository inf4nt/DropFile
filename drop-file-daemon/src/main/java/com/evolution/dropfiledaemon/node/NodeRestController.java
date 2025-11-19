package com.evolution.dropfiledaemon.node;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/daemon/node")
public class NodeRestController {

    private final NodeActiveConnections nodeActiveConnections;

    @Autowired
    public NodeRestController(NodeActiveConnections nodeActiveConnections) {
        this.nodeActiveConnections = nodeActiveConnections;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/connect")
    public HttpStatus connect(@RequestBody String port,
                              HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!remoteAddr.startsWith("http://") || !remoteAddr.startsWith("https://")) {
            remoteAddr = "http://" + remoteAddr;
        }
        URI uri = URI.create(remoteAddr + ":" + port);
        nodeActiveConnections.addConnection(uri);
        return HttpStatus.OK;
    }

    @PostMapping("/disconnect")
    public HttpStatus disconnect(@RequestBody String port,
                              HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!remoteAddr.startsWith("http://") || !remoteAddr.startsWith("https://")) {
            remoteAddr = "http://" + remoteAddr;
        }
        URI uri = URI.create(remoteAddr + ":" + port);
        nodeActiveConnections.removeConnection(uri);
        return HttpStatus.OK;
    }

    @GetMapping
    public String getConnections() {
        return nodeActiveConnections
                .getConnections()
                .stream()
                .map(it -> it.toString())
                .collect(Collectors.joining(","));
    }

    @GetMapping("/files")
    public String list(@RequestParam String filePath) {
        File file = new File(filePath);
        return Arrays.stream(file.listFiles()).map(it -> it.getPath()).collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String filePath) {
        File file = new File(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(file.getName()).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new FileInputStream(file)));
    }
}
