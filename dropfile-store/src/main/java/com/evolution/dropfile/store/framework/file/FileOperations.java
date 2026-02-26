package com.evolution.dropfile.store.framework.file;

import java.nio.file.Path;
import java.util.Map;

public interface FileOperations<V> {

    void removeAll(Path destination);

    void write(Path destination, Map<String, V> values);

    Map<String, V> read(Path destination);
}
