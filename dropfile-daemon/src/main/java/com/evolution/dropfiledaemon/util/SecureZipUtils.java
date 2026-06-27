package com.evolution.dropfiledaemon.util;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class SecureZipUtils {

    private static final String INNER_ZIP_NAME = "inner.zip";

    public static void zip(OutputStream outputStream,
                           File file,
                           String aliasFileName,
                           String password) throws IOException {

        OutputStream shieldOutputStream = new FilterOutputStream(outputStream) {
            @Override
            public void close() {
            }
        };

        try (ZipOutputStream outerZos = new ZipOutputStream(shieldOutputStream, password.toCharArray())) {

            ZipParameters outerParams = new ZipParameters();
            outerParams.setFileNameInZip(INNER_ZIP_NAME);
            outerParams.setEncryptionMethod(EncryptionMethod.AES);
            outerParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            outerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            outerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
            outerParams.setEncryptFiles(true);

            outerZos.putNextEntry(outerParams);

            OutputStream shield = new FilterOutputStream(outerZos) {
                @Override
                public void close() {
                    // Do not let innerZos to close outerZos
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    out.write(b, off, len);
                }
            };
            try (ZipOutputStream innerZos = new ZipOutputStream(shield)) {
                ZipParameters innerParams = new ZipParameters();
                innerParams.setFileNameInZip(aliasFileName);
                innerParams.setCompressionMethod(CompressionMethod.STORE);
                innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
                innerParams.setEntrySize(file.length());

                innerZos.putNextEntry(innerParams);

                try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                     InputStream inputStream = Channels.newInputStream(fileChannel)) {
                    inputStream.transferTo(innerZos);
                }
                innerZos.closeEntry();
            }
            outerZos.closeEntry();
        }
    }
}