package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.bootstrap.BootstrapStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(3)
@RequiredArgsConstructor
@Component
public class BootStrapStoreApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final List<BootstrapStoreInitializationProcedure> procedures;

    @Override
    public void execute() throws Exception {
        procedures.forEach(it -> it.init());
    }
}
