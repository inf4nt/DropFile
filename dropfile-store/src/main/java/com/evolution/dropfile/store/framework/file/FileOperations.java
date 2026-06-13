package com.evolution.dropfile.store.framework.file;

import java.io.IOException;
import java.nio.file.Path;

public interface FileOperations {

    void removeAll(Path destination) throws IOException;

    void write(Path destination, byte[] bytes) throws IOException;

    byte[] read(Path destination) throws IOException;
}
