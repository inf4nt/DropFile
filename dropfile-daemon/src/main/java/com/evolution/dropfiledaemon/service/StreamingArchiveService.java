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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Component
public class StreamingArchiveService {

    private static final String INNER_ZIP_NAME = "inner.zip";

    private final FileHelper fileHelper;

    private final CompressionLevel secureZipCompressionLevel;

    private final int daemonQuickShareInsecureCompressLevel;

    private final SafePathResolverHelper safePathResolverHelper;

    public StreamingArchiveService(FileHelper fileHelper,
                                   DaemonApplicationProperties applicationProperties,
                                   SafePathResolverHelper safePathResolverHelper) {
        this.fileHelper = fileHelper;
        this.safePathResolverHelper = safePathResolverHelper;
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
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Source is not a regular file: " + source);
        }

        InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                CloseShieldOutputStream.stream(outputStreamArgument)
        );

        ZipOutputStream outerZos = createOuterZipStream(password, outputStream);
        CloseShieldOutputStream closeShieldOuterZos = CloseShieldOutputStream.stream(outerZos);
        ZipOutputStream innerZos = new ZipOutputStream(closeShieldOuterZos);

        ZipParameters innerParams = createZip4jInnerParams(innerZipName, Files.size(source), secureZipCompressionLevel);
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

        writeDirectoryToZip4j(source, innerZos, secureZipCompressionLevel);

        // INTENTIONAL DESIGN: No try-with-resources
        innerZos.close();
        outerZos.closeEntry();
        outerZos.close();
    }

    public void insecureCompressedFile(Path source, OutputStream outputStreamArgument) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Source is not a regular file: " + source);
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

        CompressionLevel compressionLevel = Arrays.stream(CompressionLevel.values())
                .filter(it -> it.getLevel() == daemonQuickShareInsecureCompressLevel)
                .findAny()
                .orElseThrow();
        ZipOutputStream zos = new ZipOutputStream(stream);

        writeDirectoryToZip4j(source, zos, compressionLevel);

        zos.flush();
        zos.close();
    }

    public void insecureFile(Path source, OutputStream outputStreamArgument) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Source is not a regular file: " + source);
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

        ZipOutputStream zos = new ZipOutputStream(stream);

        writeDirectoryToZip4j(source, zos, CompressionLevel.NO_COMPRESSION);

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

    private ZipParameters createZip4jInnerParams(String entryName, long size, CompressionLevel compressionLevel) {
        ZipParameters innerParams = new ZipParameters();
        innerParams.setFileNameInZip(entryName);
        if (compressionLevel == CompressionLevel.NO_COMPRESSION) {
            innerParams.setCompressionMethod(CompressionMethod.STORE);
            innerParams.setCompressionLevel(CompressionLevel.NO_COMPRESSION);
        } else {
            innerParams.setCompressionMethod(CompressionMethod.DEFLATE);
            innerParams.setCompressionLevel(compressionLevel);
        }
        if (size >= 0) {
            innerParams.setEntrySize(size);
        }
        return innerParams;
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

    private Path validateFileBeforeTransfer(Path file, Path realSourceDir) throws IOException {
        Path realFile = file.toRealPath();

        if (!realFile.startsWith(realSourceDir)) {
            throw new SecurityException(
                    "File escaped published directory (possible TOCTOU attack): " + file
            );
        }

        if (safePathResolverHelper.isSensitiveDaemonPath(realFile)) {
            throw new SecurityException(
                    "Publishing sensitive daemon path is restricted: " + realFile
            );
        }

        return realFile;
    }

    private List<PathWithAttributes> getPublishableFiles(Path realSourceDir) throws IOException {
        if (safePathResolverHelper.isSensitiveDaemonPath(realSourceDir)) {
            throw new SecurityException(
                    "Publishing sensitive daemon path is restricted: " + realSourceDir
            );
        }

        try (Stream<Path> stream = Files.walk(realSourceDir)) {
            return stream
                    .map(file -> {
                        try {
                            BasicFileAttributes attributes = Files.readAttributes(
                                    file,
                                    BasicFileAttributes.class,
                                    LinkOption.NOFOLLOW_LINKS
                            );
                            return new PathWithAttributes(file, attributes);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .filter(item -> item.attributes().isRegularFile())
                    .filter(item -> !safePathResolverHelper.isSensitiveDaemonPath(item.path()))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void writeDirectoryToZip4j(Path sourceDir, ZipOutputStream zos, CompressionLevel compressionLevel) throws IOException {
        Path realSourceDir = sourceDir.toRealPath();

        List<PathWithAttributes> publishableFiles = getPublishableFiles(realSourceDir);

        String rootDirName = realSourceDir.getFileName().toString() + "/";

        ZipParameters dirParams = new ZipParameters();
        dirParams.setFileNameInZip(rootDirName);
        zos.putNextEntry(dirParams);
        zos.closeEntry();

        for (PathWithAttributes item : publishableFiles) {
            Path file = item.path();
            Path realFile = validateFileBeforeTransfer(file, realSourceDir);

            String relativeSubPath = realSourceDir
                    .relativize(realFile)
                    .toString()
                    .replace('\\', '/');

            String zipEntryPath = rootDirName + relativeSubPath;

            ZipParameters params = createZip4jInnerParams(
                    zipEntryPath,
                    item.attributes().size(),
                    compressionLevel
            );

            zos.putNextEntry(params);
            try {
                fileHelper.transferTo(realFile, zos);
            } finally {
                zos.closeEntry();
            }
        }
    }

    private record PathWithAttributes(Path path, BasicFileAttributes attributes) {
    }
}