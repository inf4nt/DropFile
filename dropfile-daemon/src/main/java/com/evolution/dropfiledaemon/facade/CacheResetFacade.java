package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.store.framework.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CacheResetFacade {

    private final List<Cacheable> cacheables;

    // TODO drop Cacheable and make a refactoring for all  Cacheable
    private final List<Purgeable> purgeables;

    public void reset() {
        cacheables.forEach(it -> it.reset());
        purgeables.forEach(it -> it.purge());
    }
}
