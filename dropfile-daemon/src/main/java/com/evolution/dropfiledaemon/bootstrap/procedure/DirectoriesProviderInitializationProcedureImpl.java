package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.framework.file.DirectoriesProviderInitializationProcedure;
import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DirectoriesProviderInitializationProcedureImpl
        implements DirectoriesProviderInitializationProcedure {

    private final List<DirectoryProvider> directoryProviders;

    @SneakyThrows
    @Override
    public void init() {
        for (DirectoryProvider directoryProvider : directoryProviders) {
            Path directoryPath = directoryProvider.getDirectoryPath();
            if (Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        }
    }
}
