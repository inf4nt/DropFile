package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.store.store.KeyValueStore;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonFileKeyValueStore<V> implements KeyValueStore<V> {

    private static final Integer READ_BUFFER_SIZE = Integer.MAX_VALUE;

    private final FileProvider fileProvider;

    private final JsonSerde<V> jsonSerde;

    public JsonFileKeyValueStore(FileProvider fileProvider,
                                 JsonSerde<V> jsonSerde) {
        this.fileProvider = fileProvider;
        this.jsonSerde = jsonSerde;
    }

    @Override
    public synchronized V save(String key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        validate(key, value);
        return writeKeyValue(key, value);
    }

    @Override
    public synchronized V remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return removeByKey(key);
    }

    @Override
    public synchronized void removeAll() {
        removeAllStore();
    }

    @Override
    public synchronized Map<String, V> getAll() {
        return readAll();
    }

    @SneakyThrows
    private Map<String, V> readAll() {
        try (FileChannel channel = FileChannel.open(fileProvider.getFile().toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();
            Map<String, V> stringVMap = readChannel(channel);
            lock.release();
            return stringVMap;
        }
    }

    @SneakyThrows
    private Map<String, V> readChannel(FileChannel channel) {
        if (channel.size() == 0) {
            return Collections.emptyMap();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int bufferSize = READ_BUFFER_SIZE;
            if (bufferSize > channel.size()) {
                bufferSize = (int) channel.size();
            }
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            while (channel.read(buffer) != -1) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    out.write(buffer.get());
                }

                buffer.clear();
            }

            byte[] allBytes = out.toByteArray();
            Map<String, V> deserialize = jsonSerde.deserialize(allBytes);
            return deserialize;
        }
    }

    @SneakyThrows
    private V writeKeyValue(String key, V value) {
        try (FileChannel channel = FileChannel.open(fileProvider.getFile().toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            Map<String, V> values = readChannel(channel);
            values = new LinkedHashMap<>(values);

            values.put(key, value);
            byte[] byteArray = jsonSerde.serialize(values);
            writeData(channel, byteArray);

            lock.release();
            return value;
        }
    }

    @SneakyThrows
    private V removeByKey(String key) {
        try (FileChannel channel = FileChannel.open(
                fileProvider.getFile().toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            Map<String, V> values = readChannel(channel);
            if (values.isEmpty()) {
                lock.release();
                return null;
            }

            values = new LinkedHashMap<>(values);
            V removedValue = values.remove(key);
            if (removedValue == null) {
                lock.release();
                return null;
            }
            byte[] byteArray = jsonSerde.serialize(values);
            writeData(channel, byteArray);

            lock.release();

            return removedValue;
        }
    }

    @SneakyThrows
    private void removeAllStore() {
        Path filePath = fileProvider.getFile().toPath();
        try (FileChannel channel = FileChannel.open(filePath,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            byte[] byteArray = jsonSerde.serialize(new LinkedHashMap<>());
            writeData(channel, byteArray);

            lock.release();
        }
    }

    @SneakyThrows
    private void writeData(FileChannel fileChannel, byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);

        fileChannel.truncate(0);
        fileChannel.position(0);

        while (buffer.hasRemaining()) {
            fileChannel.write(buffer);
        }

        fileChannel.force(true);
    }
}
