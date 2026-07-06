package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
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
import java.util.Arrays;

@Component
public class SecureZipService {

    private static final String INNER_ZIP_NAME = "inner.zip";

    private final FileHelper fileHelper;

    private final CompressionLevel compressionLevel;

    public SecureZipService(FileHelper fileHelper, DaemonApplicationProperties applicationProperties) {
        this.fileHelper = fileHelper;
        this.compressionLevel = getSecureCompressLevel(applicationProperties.daemonQuickShareSecureCompressLevel);
    }

    public void zip(OutputStream outputStream,
                    File file,
                    String innerZipName,
                    String password) throws IOException {
        CloseShieldOutputStream closeShieldOutputStream = new CloseShieldOutputStream(outputStream);
        try (ZipOutputStream outerZos = new ZipOutputStream(closeShieldOutputStream, password.toCharArray())) {

            ZipParameters outerParams = new ZipParameters();
            outerParams.setFileNameInZip(INNER_ZIP_NAME);
            outerParams.setEncryptionMethod(EncryptionMethod.AES);
            outerParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            outerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            outerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
            outerParams.setEncryptFiles(true);

            outerZos.putNextEntry(outerParams);

            CloseShieldOutputStream closeShieldOuterZos = new CloseShieldOutputStream(outerZos);
            try (ZipOutputStream innerZos = new ZipOutputStream(closeShieldOuterZos)) {
                ZipParameters innerParams = new ZipParameters();
                innerParams.setFileNameInZip(innerZipName);
                if (compressionLevel.getLevel() == 0) {
                    innerParams.setCompressionMethod(CompressionMethod.STORE);
                    innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
                } else {
                    innerParams.setCompressionMethod(CompressionMethod.DEFLATE);
                    innerParams.setCompressionLevel(compressionLevel);
                }
                innerParams.setEntrySize(file.length());

                innerZos.putNextEntry(innerParams);

                fileHelper.transferTo(file.toPath(), innerZos);

                innerZos.closeEntry();
            }
            outerZos.closeEntry();
        }
    }

    private CompressionLevel getSecureCompressLevel(Integer compressLevel) {
        if (compressLevel == null || compressLevel < 0) {
            return CompressionLevel.NO_COMPRESSION;
        }
        return Arrays.stream(CompressionLevel.values()).filter(it -> it.getLevel() == compressLevel).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported compress level " + compressLevel));
    }
}
