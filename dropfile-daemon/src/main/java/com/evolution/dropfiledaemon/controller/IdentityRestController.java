package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity")
public class IdentityRestController {

    private final KeysConfigStore keysConfigStore;

    @Autowired
    public IdentityRestController(KeysConfigStore keysConfigStore) {
        this.keysConfigStore = keysConfigStore;
    }

    @GetMapping
    public String identity() {
        byte[] publicKey = keysConfigStore.getRequired().publicKey();
        return CryptoUtils.encodeBase64(publicKey);
    }
}
