package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class ApplicationFingerprintSupplierImpl implements ApplicationFingerprintSupplier {

    private final FileProvider fileProvider;

    volatile private String fingerprint;

    public ApplicationFingerprintSupplierImpl(FileProvider fileProvider) {
        this.fileProvider = fileProvider;
    }

    @SneakyThrows
    @Override
    public String get() {
        if (fingerprint == null) {
            synchronized (this) {
                if (fingerprint == null) {
                    fingerprint = getFingerprint();
                }
            }
        }
        return fingerprint;
    }

    @SneakyThrows
    private String getFingerprint() {
        Path filePath = fileProvider.getFilePath();
        if (Files.notExists(filePath)) {
            Path parent = filePath.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectory(parent);
            }
            Files.createFile(filePath);
        }

        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE);
             FileLock lock = fileChannel.lock()) {
            int sha256Length = 64;
            long fileChannelSize = fileChannel.size();
            if (fileChannelSize != 0 && fileChannelSize != sha256Length) {
                throw new IllegalArgumentException(String.format(
                        "Unexpected fingerprint length. Expected %s actual %s", sha256Length, fileChannelSize
                ));
            }
            if (fileChannel.size() == 0) {
                String fingerprint = CommonUtils.getFingerprint(UUID.randomUUID().toString().getBytes());
                fileChannel.write(ByteBuffer.wrap(fingerprint.getBytes()));
            }
            lock.release();
        }
        return Files.readString(filePath);
    }
}
