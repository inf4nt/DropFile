package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.store.framework.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CacheResetFacade {

    private final List<Cacheable> cacheables;

    public void reset() {
        cacheables.forEach(it -> it.reset());
    }
}
