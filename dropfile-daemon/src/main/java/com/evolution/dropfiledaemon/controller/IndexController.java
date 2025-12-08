package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class IndexController {

    @GetMapping
    public String index() {
        return DropFileDaemonApplication.class.getSimpleName();
    }
}
