package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.InterruptibleOutputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

@Component
public class StreamingArchiveService {

    private static final String INNER_ZIP_NAME = "inner.zip";

    private final FileHelper fileHelper;

    private final CompressionLevel secureZipCompressionLevel;

    private final int daemonQuickShareInsecureCompressLevel;

    public StreamingArchiveService(FileHelper fileHelper,
                                   DaemonApplicationProperties applicationProperties) {
        this.fileHelper = fileHelper;
        int compressLevel = getCompressLevel(applicationProperties.daemonQuickShareSecureCompressLevel);
        this.secureZipCompressionLevel = Arrays.stream(CompressionLevel.values())
                .filter(it -> it.getLevel() == compressLevel)
                .findAny().orElseThrow();
        this.daemonQuickShareInsecureCompressLevel = getCompressLevel(applicationProperties.daemonQuickShareInsecureCompressLevel);
    }

    /**
     * Streams an encrypted ZIP file directly to the client output stream.
     * <p>
     * CRITICAL ARCHITECTURAL WARNING — DO NOT REFACTOR TO TRY-WITH-RESOURCES
     */
    public void secureZip(OutputStream outputStreamArgument,
                          Path source,
                          String innerZipName,
                          String password) throws IOException {
        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        ZipOutputStream outerZos = new ZipOutputStream(outputStream, password.toCharArray());
        ZipParameters outerParams = new ZipParameters();
        outerParams.setFileNameInZip(INNER_ZIP_NAME);
        outerParams.setEncryptionMethod(EncryptionMethod.AES);
        outerParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        outerParams.setCompressionMethod(CompressionMethod.DEFLATE);
        outerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
        outerParams.setEncryptFiles(true);

        outerZos.putNextEntry(outerParams);

        CloseShieldOutputStream closeShieldOuterZos = CloseShieldOutputStream.stream(outerZos);
        ZipOutputStream innerZos = new ZipOutputStream(closeShieldOuterZos);

        ZipParameters innerParams = new ZipParameters();
        innerParams.setFileNameInZip(innerZipName);
        if (secureZipCompressionLevel.getLevel() == 0) {
            innerParams.setCompressionMethod(CompressionMethod.STORE);
            innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
        } else {
            innerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            innerParams.setCompressionLevel(secureZipCompressionLevel);
        }
        innerParams.setEntrySize(Files.size(source));

        innerZos.putNextEntry(innerParams);

        fileHelper.transferTo(source, innerZos);

        // INTENTIONAL DESIGN: Do NOT wrap OutputStream in try-with-resources.
        // Finalization (Central Directory writing) MUST only occur if transferTo completes without errors.
        // On timeout/interrupt, bypassing these calls ensures the client gets a truncated file rather than a corrupt archive.
        innerZos.closeEntry();
        innerZos.close();
        outerZos.closeEntry();
        outerZos.close();
    }

    public void gzip(Path source, OutputStream outputStream) throws IOException {
        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStream)
        );

        GZIPOutputStream gzipOut = createConfiguredGzipStream(
                stream,
                daemonQuickShareInsecureCompressLevel
        );

        fileHelper.transferTo(source, gzipOut);

        // INTENTIONAL DESIGN: Do NOT wrap OutputStream in try-with-resources.
        // Finalization (GZIP Trailer writing: CRC32 & ISIZE) MUST only occur
        // if transferTo completes without errors. On timeout/interrupt,
        // bypassing close() ensures the client gets a truncated file without a valid GZIP trailer.
        gzipOut.close();
    }

    private int getCompressLevel(Integer compressLevel) {
        if (compressLevel == null || compressLevel == -1) {
            return 0;
        }
        if (compressLevel >= 0 && compressLevel <= 9) {
            return compressLevel;
        }
        throw new IllegalArgumentException("Invalid compress level " + compressLevel);
    }

    private GZIPOutputStream createConfiguredGzipStream(OutputStream out, int level) throws IOException {
        return new GZIPOutputStream(out) {
            {
                this.def.setLevel(level);
            }
        };
    }
}
