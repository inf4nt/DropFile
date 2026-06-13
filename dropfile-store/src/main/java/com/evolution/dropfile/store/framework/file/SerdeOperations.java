package com.evolution.dropfile.store.framework.file;

import java.io.IOException;
import java.util.Map;

public interface SerdeOperations<V> {

    Map<String, V> deserialize(byte[] bytes) throws IOException;

    byte[] serialize(Map<String, V> values) throws IOException;
}
