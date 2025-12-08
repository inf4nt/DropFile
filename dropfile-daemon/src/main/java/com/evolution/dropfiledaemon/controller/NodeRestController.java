package com.evolution.dropfiledaemon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/node")
public class NodeRestController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
