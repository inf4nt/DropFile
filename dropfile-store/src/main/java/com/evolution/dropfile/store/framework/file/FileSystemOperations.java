package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.FileHelper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@RequiredArgsConstructor
public class FileSystemOperations implements FileOperations {

    private final FileHelper fileHelper;

    @Override
    public void removeAll(Path destination) throws IOException {
        Path temporaryFilePath = null;
        try {
            temporaryFilePath = getOrCreateTemporaryFilePath(destination);
            Files.move(temporaryFilePath, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (temporaryFilePath != null) {
                Files.deleteIfExists(temporaryFilePath);
            }
        }
    }

    @Override
    public void write(Path destination, InputStream inputStream) throws IOException {
        Path temporaryFilePath = null;
        try {
            temporaryFilePath = getOrCreateTemporaryFilePath(destination);
            fileHelper.write(temporaryFilePath, inputStream);
            Files.move(temporaryFilePath, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (temporaryFilePath != null) {
                Files.deleteIfExists(temporaryFilePath);
            }
        }
    }

    @Override
    public InputStream read(Path destination) throws NoContentFoundException, IOException {
        if (Files.notExists(destination) || Files.size(destination) == 0) {
            throw new NoContentFoundException(destination);
        }

        FileChannel fileChannel = FileChannel.open(destination, StandardOpenOption.READ);
        try {
            return Channels.newInputStream(fileChannel);
        } catch (Exception e) {
            fileChannel.close();
            throw new IOException(e);
        }
    }

    @SneakyThrows
    private Path getOrCreateTemporaryFilePath(Path destination) {
        String filename = destination.toFile().getName();
        String temporaryFileName = CommonFileUtils.getTemporaryFileName(filename);
        Path parent = destination.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        Path temporaryFilePath = parent.resolve(temporaryFileName);
        if (Files.notExists(temporaryFilePath)) {
            Files.createFile(temporaryFilePath);
        }
        return temporaryFilePath;
    }
}
