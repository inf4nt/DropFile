package com.evolution.dropfiledaemon.util;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.*;

public class SecureZipUtils {

    public static void zip(OutputStream outputStream,
                           File file,
                           String aliasFileName,
                           String password) throws IOException {

        try (ZipOutputStream outerZos = new ZipOutputStream(outputStream, password.toCharArray())) {

            ZipParameters outerParams = new ZipParameters();
            outerParams.setFileNameInZip("inner.zip");
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

                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(innerZos);
                }
                innerZos.closeEntry();
            }
            outerZos.closeEntry();
        }
    }
}