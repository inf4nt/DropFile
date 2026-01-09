package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.store.store.KeyValueStore;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonFileKeyValueStore<V> implements KeyValueStore<String, V> {

    private static final Integer READ_BUFFER_SIZE = 8024;

    private final FileProvider fileProvider;

    private final JsonSerde<V> jsonSerde;

    public JsonFileKeyValueStore(FileProvider fileProvider,
                                 JsonSerde<V> jsonSerde) {
        this.fileProvider = fileProvider;
        this.jsonSerde = jsonSerde;
    }

    @Override
    public V save(String key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        validate(key, value);
        return writeKeyValue(key, value);
    }

    @Override
    public V remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return removeByKey(key);
    }

    @Override
    public void removeAll() {
        removeAllStore();
    }

    @Override
    public Map<String, V> getAll() {
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
            ByteBuffer buff = ByteBuffer.allocate(bufferSize);

            while (channel.read(buff) > 0) {
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }

            byte[] allBytes = out.toByteArray();
            Map<String, V> deserialize = jsonSerde.deserialize(allBytes);
            return deserialize;
        }
    }

    @SneakyThrows
    private V writeKeyValue(String key, V value) {
        try (FileChannel channel = FileChannel.open(fileProvider.getFile().toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            Map<String, V> values = readChannel(channel);
            values = new LinkedHashMap<>(values);

            values.put(key, value);
            byte[] byteArray = jsonSerde.serialize(values);

            channel.write(ByteBuffer.wrap(byteArray), 0);
            lock.release();
            return value;
        }
    }

    @SneakyThrows
    private V removeByKey(String key) {
        try (FileChannel channel = FileChannel.open(fileProvider.getFile().toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
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

            channel.write(ByteBuffer.wrap(byteArray), 0);

            lock.release();

            return removedValue;
        }
    }

    @SneakyThrows
    private void removeAllStore() {
        try (FileChannel channel = FileChannel.open(fileProvider.getFile().toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            byte[] byteArray = jsonSerde.serialize(new LinkedHashMap<>());

            channel.write(ByteBuffer.wrap(byteArray), 0);

            lock.release();
        }
    }
}
