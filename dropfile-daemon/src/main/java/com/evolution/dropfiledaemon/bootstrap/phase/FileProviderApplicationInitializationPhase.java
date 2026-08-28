package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfiledaemon.bootstrap.phase.api.ApplicationInitializationPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Order(2)
@RequiredArgsConstructor
@Component
public class FileProviderApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final List<FileProvider> fileProviders;

    @Override
    public void execute() throws Exception {
        for (FileProvider fileProvider : fileProviders) {
            Path filePath = fileProvider.getFilePath();
            if (Files.notExists(filePath)) {
                Path parent = filePath.getParent();
                if (Files.notExists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(filePath);
            }
        }
    }
}
