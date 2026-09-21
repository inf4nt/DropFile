package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class AccessKeyService implements Purgeable {

    private final AccessKeyStore accessKeyStore;

    public KeyEnvelope generate() {
        String key = CommonUtils.generateFormattedId();
        String id = getId(key);
        return new KeyEnvelope(id, key);
    }

    public String getId(String key) {
        return CommonUtils.getFingerprint(key.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isAccessKeyExpired(AccessKey accessKey) {
        return isAccessKeyExpired(Instant.now(), accessKey);
    }

    public boolean isAccessKeyExpired(Instant now, AccessKey accessKey) {
        Duration ttl = accessKey.ttl();
        Instant created = accessKey.created();
        Instant expiredInstant = created.plus(ttl);
        return now.isAfter(expiredInstant);
    }

    @Override
    public void purge() {
        Instant now = Instant.now();
        Set<String> expiredKeys = accessKeyStore.getAll()
                .entrySet()
                .stream()
                .filter(it -> isAccessKeyExpired(now, it.getValue()))
                .map(it -> it.getKey())
                .collect(Collectors.toSet());

        if (!ObjectUtils.isEmpty(expiredKeys)) {
            log.info("Found expired keys {}. Removing", expiredKeys.size());
            accessKeyStore.remove(expiredKeys);
        }
    }

    public record KeyEnvelope(String id, String key) {
    }
}
