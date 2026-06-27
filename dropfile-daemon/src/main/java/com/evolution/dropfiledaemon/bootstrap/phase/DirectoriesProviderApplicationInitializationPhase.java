package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.file.DirectoriesProviderInitializationProcedure;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(1)
@RequiredArgsConstructor
@Component
public class DirectoriesProviderApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final List<DirectoriesProviderInitializationProcedure> procedures;

    @Override
    public void execute() throws Exception {
        procedures.forEach(it -> it.init());
    }
}
