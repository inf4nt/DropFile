package com.evolution.dropfile.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class FileHelper {

    private static final String SHA256 = "SHA-256";

    private final int bufferSize;

    public FileHelper(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be greater than zero");
        }
        this.bufferSize = bufferSize;
    }

    public InputStream readStream(File file, long skip, int take) throws IOException {
        FileChannel fileChannel = null;
        try {
            fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
            fileChannel.position(skip);
            InputStream inputStream = Channels.newInputStream(fileChannel);
            return new WatchdogInputStream(inputStream, take);
        } catch (IOException e) {
            if (fileChannel != null) {
                fileChannel.close();
            }
            throw e;
        }
    }

    public void write(Path path, byte[] data) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
             InputStream inputStream = new ByteArrayInputStream(data)) {
            write(channel, inputStream, 0, data.length);
        }
    }

    public void write(FileChannel fileChannel,
                      InputStream inputStream,
                      long position,
                      long length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        long offset = position;
        long bytesRemainingToRead = length;

        byte[] transferArray = new byte[bufferSize];
        int bytesRead;

        while (bytesRemainingToRead > 0 &&
                (bytesRead = inputStream.read(transferArray, 0, (int) Math.min(transferArray.length, bytesRemainingToRead))) != -1) {

            CommonUtils.isInterrupted();

            buffer.clear();
            buffer.put(transferArray, 0, bytesRead);
            buffer.flip();

            while (buffer.hasRemaining()) {
                int written = fileChannel.write(buffer, offset);
                if (written <= 0) {
                    throw new IOException("FileChannel.write wrote zero or negative numbers of bytes");
                }
                offset += written;
            }

            bytesRemainingToRead -= bytesRead;
        }

        if (bytesRemainingToRead > 0) {
            throw new IOException("Stream ended prematurely. Missing " + bytesRemainingToRead + " bytes from manifest");
        }
    }

    public String sha256(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(SHA256);
        byte[] buffer = new byte[bufferSize];
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(channel)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                CommonUtils.isInterrupted();

                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
