package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.io.CloseShieldOutputStream;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.common.io.InterruptibleOutputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Component
public class StreamingArchiveService {

    private static final String INNER_ZIP_NAME = "inner.zip";

    private final FileHelper fileHelper;

    private final CompressionLevel secureZipCompressionLevel;

    private final int daemonQuickShareInsecureCompressLevel;

    public StreamingArchiveService(FileHelper fileHelper, DaemonApplicationProperties applicationProperties) {
        this.fileHelper = fileHelper;
        int compressLevel = getCompressLevel(applicationProperties.daemonQuickShareSecureCompressLevel);
        this.secureZipCompressionLevel = Arrays.stream(CompressionLevel.values())
                .filter(it -> it.getLevel() == compressLevel)
                .findAny()
                .orElseThrow();
        this.daemonQuickShareInsecureCompressLevel = getCompressLevel(applicationProperties.daemonQuickShareInsecureCompressLevel);
    }

    public void secureZipFile(Path source,
                              String innerZipName,
                              String password,
                              OutputStream outputStreamArgument) throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a file, but got a directory: " + source);
        }

        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        ZipOutputStream outerZos = createOuterZipStream(password, outputStream);
        CloseShieldOutputStream closeShieldOuterZos = CloseShieldOutputStream.stream(outerZos);
        ZipOutputStream innerZos = new ZipOutputStream(closeShieldOuterZos);

        ZipParameters innerParams = createZip4jInnerParams(innerZipName, Files.size(source));
        innerZos.putNextEntry(innerParams);
        fileHelper.transferTo(source, innerZos);

        // INTENTIONAL DESIGN: No try-with-resources to prevent writing Central Directory on partial stream errors
        innerZos.closeEntry();
        innerZos.close();
        outerZos.closeEntry();
        outerZos.close();
    }

    public void secureZipDirectory(Path source,
                                   String password,
                                   OutputStream outputStreamArgument) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a directory, but got a file: " + source);
        }

        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        ZipOutputStream outerZos = createOuterZipStream(password, outputStream);
        CloseShieldOutputStream closeShieldOuterZos = CloseShieldOutputStream.stream(outerZos);
        ZipOutputStream innerZos = new ZipOutputStream(closeShieldOuterZos);

        writeDirectoryToZip4j(source, innerZos);

        // INTENTIONAL DESIGN: No try-with-resources
        innerZos.close();
        outerZos.closeEntry();
        outerZos.close();
    }

    public void insecureCompressedZipFile(Path source, OutputStream outputStreamArgument) throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a file, but got a directory: " + source);
        }

        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        GZIPOutputStream gzipOut = createConfiguredGzipStream(stream, daemonQuickShareInsecureCompressLevel);
        fileHelper.transferTo(source, gzipOut);

        // INTENTIONAL DESIGN: Do NOT close on error
        gzipOut.close();
    }

    public void insecureCompressedZipDirectory(Path source, OutputStream outputStreamArgument) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a directory, but got a file: " + source);
        }

        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        GZIPOutputStream gzipOut = createConfiguredGzipStream(stream, daemonQuickShareInsecureCompressLevel);

        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(gzipOut);
        zos.setLevel(java.util.zip.Deflater.NO_COMPRESSION);

        writeDirectoryToStandardZip(source, zos);

        zos.finish();
        gzipOut.close();
    }

    public void insecureZipFile(Path source, OutputStream outputStreamArgument) throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a file, but got a directory: " + source);
        }

        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        fileHelper.transferTo(source, outputStream);
        outputStream.flush();
    }

    public void insecureZipDirectory(Path source, OutputStream outputStreamArgument) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Expected a directory, but got a file: " + source);
        }

        InterruptibleOutputStream stream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(stream);
        zos.setLevel(java.util.zip.Deflater.NO_COMPRESSION);

        writeDirectoryToStandardZip(source, zos);

        zos.finish();
        zos.close();
    }

    private ZipOutputStream createOuterZipStream(String password, OutputStream outputStream) throws IOException {
        ZipOutputStream outerZos = new ZipOutputStream(outputStream, password.toCharArray());
        ZipParameters outerParams = new ZipParameters();
        outerParams.setFileNameInZip(INNER_ZIP_NAME);
        outerParams.setEncryptionMethod(EncryptionMethod.AES);
        outerParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        outerParams.setCompressionMethod(CompressionMethod.DEFLATE);
        outerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
        outerParams.setEncryptFiles(true);

        outerZos.putNextEntry(outerParams);
        return outerZos;
    }

    private ZipParameters createZip4jInnerParams(String entryName, long size) {
        ZipParameters innerParams = new ZipParameters();
        innerParams.setFileNameInZip(entryName);
        if (secureZipCompressionLevel.getLevel() == 0) {
            innerParams.setCompressionMethod(CompressionMethod.STORE);
            innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
        } else {
            innerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            innerParams.setCompressionLevel(secureZipCompressionLevel);
        }
        if (size >= 0) {
            innerParams.setEntrySize(size);
        }
        return innerParams;
    }

    private void writeDirectoryToZip4j(Path sourceDir, ZipOutputStream zos) throws IOException {
        Path rootDir = sourceDir.getParent() != null ? sourceDir.getParent() : sourceDir.getFileSystem().getPath("");
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String relativePath = rootDir.relativize(file).toString().replace('\\', '/');
                    ZipParameters params = createZip4jInnerParams(relativePath, Files.size(file));
                    zos.putNextEntry(params);
                    fileHelper.transferTo(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void writeDirectoryToStandardZip(Path sourceDir, java.util.zip.ZipOutputStream zos) throws IOException {
        Path rootDir = sourceDir.getParent() != null ? sourceDir.getParent() : sourceDir.getFileSystem().getPath("");
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String relativePath = rootDir.relativize(file).toString().replace('\\', '/');
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(relativePath);
                    zos.putNextEntry(entry);
                    fileHelper.transferTo(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
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
