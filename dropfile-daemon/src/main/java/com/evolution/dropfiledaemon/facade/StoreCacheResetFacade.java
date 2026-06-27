package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.store.framework.Cacheable;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class StoreCacheResetFacade {

    private final List<KeyValueStore> keyValueStores;

    private final List<SingleValueStore> singleValueStores;

    public void reset() {
        Stream.concat(keyValueStores.stream(), singleValueStores.stream())
                .forEach(store -> {
                    if (store instanceof Cacheable cacheable) {
                        cacheable.reset();
                    }
                });
    }
}
