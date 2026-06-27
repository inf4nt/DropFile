package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.file.FileProviderInitializationProcedure;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(2)
@RequiredArgsConstructor
@Component
public class FileProviderApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final List<FileProviderInitializationProcedure> procedures;

    @Override
    public void execute() throws Exception {
        procedures.forEach(it -> it.init());
    }
}
