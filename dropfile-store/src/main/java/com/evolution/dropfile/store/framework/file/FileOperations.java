package com.evolution.dropfile.store.framework.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileOperations {

    void removeAll(Path destination) throws IOException;

    void write(Path destination, InputStream inputStream) throws IOException;

    InputStream read(Path destination) throws NoContentFoundException, IOException;
}
