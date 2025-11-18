package com.evolution.dropfiledaemon;

import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/daemon/node")
public class NodeRestController {

    // TODO give me your IP
    @PostMapping("/connect")
    public HttpStatus connect() {
        return HttpStatus.OK;
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
