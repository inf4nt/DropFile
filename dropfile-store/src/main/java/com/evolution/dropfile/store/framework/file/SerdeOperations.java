package com.evolution.dropfile.store.framework.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface SerdeOperations<V> {

    Map<String, V> deserialize(InputStream inputStream) throws IOException;

    byte[] serialize(Map<String, V> values) throws IOException;
}
