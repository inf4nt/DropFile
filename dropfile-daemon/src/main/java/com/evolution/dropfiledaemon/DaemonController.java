package com.evolution.dropfiledaemon;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/daemon")
@CrossOrigin(origins = "*")
public class DaemonController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "READY"));
    }

    @PostMapping("/share")
    public ResponseEntity<Map<String, String>> shareFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("secure") boolean secure,
            @RequestParam("secret") String secret,
            @RequestParam("singleUse") boolean singleUse) {

        System.out.println("Received " + files.size() + " files from Electron.");

        String url = "http://localhost:18181/s/qs/" + UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/exit")
    public void shutdownApp() {
        System.out.println("shutdownApp");
        DropFileDaemonApplication.exit();
    }
}