package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfiledaemon.file.FileDownloadOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileDownloadStatusRestController {

    private final FileDownloadOrchestrator fileDownloadOrchestrator;

    @Autowired
    public FileDownloadStatusRestController(FileDownloadOrchestrator fileDownloadOrchestrator) {
        this.fileDownloadOrchestrator = fileDownloadOrchestrator;
    }

    @GetMapping("/status")
    public Object getStatus() {
        List<FileDownloadOrchestrator.DownloadProgress> downloadProgressList = new ArrayList<>(fileDownloadOrchestrator.getDownloadProgressList());
        Collections.reverse(downloadProgressList);
        return downloadProgressList;
    }
}
