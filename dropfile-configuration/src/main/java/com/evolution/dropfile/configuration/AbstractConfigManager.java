package com.evolution.dropfile.configuration;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public abstract class AbstractConfigManager {

    private static final Integer READ_BUFFER_SIZE = 8024;

    private static final String WINDOWS_HOME_DIR = "DropFile";

    private static final String HOME_DIR = ".dropfile";

    @SneakyThrows
    protected byte[] readPath(Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FileLock lock = channel.lock();

            int bufferSize = READ_BUFFER_SIZE;
            if (bufferSize > channel.size()) {
                bufferSize = (int) channel.size();
            }
            ByteBuffer buff = ByteBuffer.allocate(bufferSize);

            while (channel.read(buff) > 0) {
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }

            lock.release();
            return out.toByteArray();
        }
    }

    @SneakyThrows
    protected void writePath(Path path, byte[] data) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();
            channel.write(ByteBuffer.wrap(data), 0);
            lock.release();
        }
    }

    @SneakyThrows
    protected void createFiles(File configFile) {
        if (Files.exists(configFile.toPath())) {
            return;
        }

        File parentFile = configFile.getParentFile();
        if (Files.notExists(parentFile.toPath())) {
            Files.createDirectories(parentFile.toPath());
        }
        Files.createFile(configFile.toPath());
    }

    protected Path resolveHomeDirectory() {
        return Paths.get(System.getProperty("user.home"))
                .resolve(HOME_DIR);
    }

    protected Path resolveProtectedHomeDirectory() {
        if (isWindows()) {
            String basePath = System.getenv("LOCALAPPDATA");
            return Path.of(basePath, WINDOWS_HOME_DIR);
        }
        if (isLinux() || isMacOs()) {
            return resolveHomeDirectory();
        }
        throw new UnsupportedOperationException(
                "Unsupported operating system " + getOs()
        );
    }

    private boolean isMacOs() {
        return getOs().toLowerCase().startsWith("mac");
    }

    private boolean isLinux() {
        return getOs().toLowerCase().startsWith("linux");
    }

    private boolean isWindows() {
        return getOs().toLowerCase().startsWith("windows");
    }

    private String getOs() {
        return System.getProperty("os.name");
    }
}
