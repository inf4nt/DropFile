package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfiledaemon.facade.ApiConnectionsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connections")
public class ApiConnectionsRestController {

    private final ApiConnectionsFacade apiFacade;

    @Autowired
    public ApiConnectionsRestController(ApiConnectionsFacade apiFacade) {
        this.apiFacade = apiFacade;
    }

    @PostMapping("/revoke/{fingerprint}")
    public ResponseEntity<Void> revoke(@PathVariable String fingerprint) {
        boolean result = apiFacade.revoke(fingerprint);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
