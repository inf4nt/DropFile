package com.evolution.dropfile.common.function;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface OutputStreamConsumer {

    void accept(OutputStream outputStream) throws IOException;
}
