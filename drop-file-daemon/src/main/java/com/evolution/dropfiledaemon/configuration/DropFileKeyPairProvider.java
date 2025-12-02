package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.keys.DropFileKeysConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.security.KeyPair;
import java.util.Objects;

@Configuration
public class DropFileKeyPairProvider implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${config.public.key:#{null}}")
    private String customPublicKey;

    @Value("${config.private.key:#{null}}")
    private String customPrivateKey;

    private KeyPair keyPair;

    private final DropFileKeysConfigManager keysConfigManager;

    @Autowired
    public DropFileKeyPairProvider(DropFileKeysConfigManager keysConfigManager) {
        this.keysConfigManager = keysConfigManager;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (ObjectUtils.isEmpty(customPublicKey) && ObjectUtils.isEmpty(customPrivateKey)) {
            this.keyPair = keysConfigManager.getKeyPair();
        } else {
            System.out.println("CUSTOM PUBLIC AND PRIVATE KEYS DO NOT WORK YET!");
//            throw new UnsupportedOperationException("Implement me!");
        }
    }

    public KeyPair getKeyPair() {
        Objects.requireNonNull(keyPair, "KeyPair has not initialized yet");
        return keyPair;
    }
}
