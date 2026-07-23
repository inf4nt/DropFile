package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.Purgeable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class GarbageCollectorFacade {

    private final List<Purgeable> purgeables;

    public void purge() {
        purgeables.forEach(it -> it.purge());
    }

    // TODO add Scheduled to run it hourly
}
