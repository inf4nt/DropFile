package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfile.common.FileHelper;
import lombok.RequiredArgsConstructor;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@Component
@RequiredArgsConstructor
public class SecureZipService {

    private static final String INNER_ZIP_NAME = "inner.zip";

    private final FileHelper fileHelper;

    public void zip(OutputStream outputStream,
                    File file,
                    String innerZipName,
                    String password) throws IOException {
        try (ZipOutputStream outerZos = new ZipOutputStream(
                new CloseShieldOutputStream(outputStream), password.toCharArray())) {

            ZipParameters outerParams = new ZipParameters();
            outerParams.setFileNameInZip(INNER_ZIP_NAME);
            outerParams.setEncryptionMethod(EncryptionMethod.AES);
            outerParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            outerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            outerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
            outerParams.setEncryptFiles(true);

            outerZos.putNextEntry(outerParams);

            try (ZipOutputStream innerZos = new ZipOutputStream(new CloseShieldOutputStream(outerZos))) {
                ZipParameters innerParams = new ZipParameters();
                innerParams.setFileNameInZip(innerZipName);
                innerParams.setCompressionMethod(CompressionMethod.STORE);
                innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
                innerParams.setEntrySize(file.length());

                innerZos.putNextEntry(innerParams);

                fileHelper.transferTo(file.toPath(), innerZos);

                innerZos.closeEntry();
            }
            outerZos.closeEntry();
        }
    }
}
