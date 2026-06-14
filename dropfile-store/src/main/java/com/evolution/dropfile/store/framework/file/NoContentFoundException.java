package com.evolution.dropfile.store.framework.file;

import java.nio.file.Path;

public class NoContentFoundException extends Exception {

    public NoContentFoundException(Path destination) {
        super(String.format("Destination file %s does not exist or empty", destination.toAbsolutePath()));
    }
}
