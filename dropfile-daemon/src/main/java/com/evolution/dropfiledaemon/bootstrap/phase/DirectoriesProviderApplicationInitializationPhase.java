package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import com.evolution.dropfiledaemon.bootstrap.ApplicationInitializationPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Order(1)
@RequiredArgsConstructor
@Component
public class DirectoriesProviderApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final List<DirectoryProvider> directoryProviders;

    @Override
    public void execute() throws Exception {
        for (DirectoryProvider directoryProvider : directoryProviders) {
            Path directoryPath = directoryProvider.getDirectoryPath();
            if (Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        }
    }
}
