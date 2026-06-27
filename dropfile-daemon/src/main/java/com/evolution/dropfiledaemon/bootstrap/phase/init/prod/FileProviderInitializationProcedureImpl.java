package com.evolution.dropfiledaemon.bootstrap.phase.init.prod;

import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderInitializationProcedure;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class FileProviderInitializationProcedureImpl
        implements FileProviderInitializationProcedure {

    private final List<FileProvider> fileProviders;

    @SneakyThrows
    @Override
    public void init() {
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
