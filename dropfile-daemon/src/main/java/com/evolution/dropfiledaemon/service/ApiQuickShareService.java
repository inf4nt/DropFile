package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.store.quickshare.QuickShare;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApiQuickShareService implements Purgeable {

    private final QuickShareStore quickShareStore;

    public boolean isTtlExpired(QuickShare quickShare) {
        return isTtlExpired(Instant.now(), quickShare);
    }

    public boolean isTtlExpired(Instant now, QuickShare quickShare) {
        Instant expiredAt = quickShare.created().plus(quickShare.ttl());
        return now.isAfter(expiredAt);
    }

    @Override
    public void purge() {
        Instant now = Instant.now();
        Set<String> expiredTTLKeys = new HashSet<>();
        Set<String> expiredKeys = new HashSet<>();

        for (Map.Entry<String, QuickShare> entry : quickShareStore.getAll()
                .entrySet()) {
            if (entry.getValue().expired()) {
                expiredKeys.add(entry.getKey());
            } else if (isTtlExpired(now, entry.getValue())) {
                expiredTTLKeys.add(entry.getKey());
            }
        }

        if (!ObjectUtils.isEmpty(expiredTTLKeys)) {
            log.info("Removing expired TTL quickshare entries size: {}", expiredTTLKeys.size());
            quickShareStore.remove(expiredTTLKeys);
        }

        if (!ObjectUtils.isEmpty(expiredKeys)) {
            log.info("Removing expired quickshare entries size: {}", expiredKeys.size());
            quickShareStore.remove(expiredKeys);
        }
    }
}
