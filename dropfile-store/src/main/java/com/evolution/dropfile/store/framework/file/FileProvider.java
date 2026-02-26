package com.evolution.dropfile.store.framework.file;

import java.io.File;
import java.nio.file.Path;

public interface FileProvider {

    String getFilename();

    File getOrCreateFile();

    Path getHomePath();

    Path getFilePath();
}
