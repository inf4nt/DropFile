package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.function.OutputStreamConsumer;
import com.evolution.dropfile.common.io.FileHelper;
import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;

@RequiredArgsConstructor
public class FileSystemOperations implements FileOperations {

    private final FileHelper fileHelper;

    @Override
    public void removeAll(Path destination) throws IOException {
        if (!Files.isRegularFile(destination)) {
            throw new IllegalArgumentException("FileSystemOperations remove all failed. The given destination is not a regular file: %s".formatted(destination));
        }

        Path temporaryFilePath = null;
        try {
            temporaryFilePath = createTemporaryFilePath(destination);
            Files.move(temporaryFilePath, destination,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Throwable throwable) {
            if (temporaryFilePath != null) {
                try {
                    Files.deleteIfExists(temporaryFilePath);
                } catch (Throwable deleteThrowable) {
                    throwable.addSuppressed(deleteThrowable);
                }
            }
            if (throwable instanceof IOException ioException) {
                throw ioException;
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    @Override
    public InputStream read(Path destination) throws NoContentFoundException, IOException {
        if (Files.notExists(destination) || Files.size(destination) == 0) {
            throw new NoContentFoundException(destination);
        }
        if (!Files.isRegularFile(destination)) {
            throw new IllegalArgumentException("FileSystemOperations read failed. Destination is not a regular file: %s".formatted(destination));
        }

        FileChannel fileChannel = null;
        try {
            fileChannel = FileChannel.open(destination, StandardOpenOption.READ);
            return Channels.newInputStream(fileChannel);
        } catch (Throwable throwable) {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (Throwable fileChannelThrowable) {
                    throwable.addSuppressed(fileChannelThrowable);
                }
            }
            if (throwable instanceof IOException ioException) {
                throw ioException;
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    public void write(Path destination, OutputStreamConsumer outputStreamConsumer) throws IOException {
        if (!Files.isRegularFile(destination)) {
            throw new IllegalArgumentException("FileSystemOperations write failed. Destination is not a regular file: %s".formatted(destination));
        }

        Path temporaryFilePath = null;
        try {
            temporaryFilePath = createTemporaryFilePath(destination);
            fileHelper.outputStreamConsumer(temporaryFilePath, outputStreamConsumer);
            Files.move(temporaryFilePath, destination,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Throwable throwable) {
            if (temporaryFilePath != null) {
                try {
                    Files.deleteIfExists(temporaryFilePath);
                } catch (Throwable deleteThrowable) {
                    throwable.addSuppressed(deleteThrowable);
                }
            }
            if (throwable instanceof IOException ioException) {
                throw ioException;
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    private Path createTemporaryFilePath(Path destination) throws IOException {
        String filename = destination.getFileName().toString();
        String temporaryFileName = CommonFileUtils.getTemporaryFileName(filename);
        Path parent = destination.getParent();

        if (parent != null && Files.notExists(parent)) {
            throw new FileNotFoundException("FileSystemOperations action failed. Unable to create temporary file %s. Parent does not exist %s".formatted(
                    temporaryFileName, parent
            ));
        }

        Path temporaryFilePath = (parent != null) ? parent.resolve(temporaryFileName) : Paths.get(temporaryFileName);

        if (Files.exists(temporaryFilePath)) {
            throw new FileAlreadyExistsException("FileSystemOperations action failed. Temporary file already exists %s".formatted(temporaryFileName));
        }

        return Files.createFile(temporaryFilePath);
    }
}
