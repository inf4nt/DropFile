package com.evolution.dropfiledaemon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class IndexRestController {

    @GetMapping
    public String index() {
        return "index";
    }
}
