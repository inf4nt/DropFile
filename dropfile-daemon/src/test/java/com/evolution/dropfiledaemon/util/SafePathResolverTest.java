package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import com.evolution.dropfiledaemon.service.SafePathResolverHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafePathResolverTest {

    private SafePathResolverHelper underTest;

    private DirectoryProvider directoryProvider;

    @BeforeEach
    public void before() {
        directoryProvider = mock(DirectoryProvider.class);
        underTest = new SafePathResolverHelper(directoryProvider);
    }

    @ParameterizedTest
    @MethodSource("provideSanitizationCases")
    void shouldSanitizeFilenameCorrectly(String input, String expected) {
        assertEquals(expected, underTest.sanitizeFilename(input));
    }

    private static Stream<Arguments> provideSanitizationCases() {
        return Stream.of(
                Arguments.of("file.txt", "file.txt"),
                Arguments.of("path/to/file.txt", "file.txt"),
                Arguments.of("C:\\path\\to\\file.txt", "file.txt"),
                Arguments.of("archive.tar.gz", "archive.tar.gz"),
                Arguments.of("   my report v2.pdf   ", "my report v2.pdf"),
                Arguments.of("    a", "a"),
                Arguments.of("    a    ", "a"),
                Arguments.of("    a", "a"),
                Arguments.of("    a b", "a b"),

                Arguments.of("file*name?.txt", "file_name_.txt"),
                Arguments.of("foo:bar|baz.txt", "foo_bar_baz.txt"),
                Arguments.of("a>b<c.jpg", "a_b_c.jpg"),
                Arguments.of("\"quote\".txt", "_quote_.txt"),

                Arguments.of(".hidden.txt", "hidden.txt"),
                Arguments.of("...veryhidden.txt", "veryhidden.txt"),
                Arguments.of("....config.spec.json", "config.spec.json"),
                Arguments.of(".a", "a"),
                Arguments.of("...........test.txt", "test.txt"),

                Arguments.of("../../file.txt", "file.txt"),
                Arguments.of("../../../../../etc/target.txt", "target.txt"),
                Arguments.of("..\\..\\..\\Windows\\System32\\system32.dll", "system32.dll"),
                Arguments.of("....//....//etc/passwd", "passwd"),
                Arguments.of(".//.//./secret.key", "secret.key"),
                Arguments.of("foo/bar\\..//..\\file.conf", "file.conf"),
                Arguments.of("folder/sub/..hidden.png", "hidden.png"),

                Arguments.of("    a \nb", "a b"),
                Arguments.of("    a \r\nb", "a b"),
                Arguments.of("  \r  a \rb", "a b"),
                Arguments.of("a\tb", "ab"),
                Arguments.of("\t\r\n data.csv \n\t", "data.csv"),
                Arguments.of("file\f\u000B.txt", "file.txt"),
                Arguments.of("first line\nsecond line.txt", "first linesecond line.txt"),
                Arguments.of("clean\u0007\u0008", "clean"),
                Arguments.of("test\u001Bb.log", "testb.log"),

                Arguments.of("документ.pdf", "документ.pdf"),
                Arguments.of("photo_🔥.jpg", "photo_🔥.jpg"),
                Arguments.of("___a___b___", "___a___b___"),
                Arguments.of("file[1].txt", "file[1].txt"),
                Arguments.of("file$1&2.txt", "file$1&2.txt"),
                Arguments.of("user@host#1.txt", "user@host#1.txt"),
                Arguments.of("data;v1,final.csv", "data;v1,final.csv"),
                Arguments.of("test!=1.log", "test!=1.log"),
                Arguments.of("v1.0.0+build-12.jar", "v1.0.0+build-12.jar"),
                Arguments.of("C:\\CON.txt", "CON.txt"),

                Arguments.of("folder/".repeat(50) + "final_file.bin", "final_file.bin")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\t", "\n", "\r", "", " ", "   ", "\r\n", "\t\n\r ", "//", "/",
            "\\", ".", "..", "...", "....", "///", "\\\\\\", "./", ".\\", "../", "..\\", "\u0001\u0002\u0003",
            "///\\\\\\", "../../..//..\\", "file\u0000name.txt", "file.txt\u0000", "\u0000file.txt", ". . ."})
    void shouldThrowException(String input) {
        assertThrows(IllegalArgumentException.class, () -> underTest.sanitizeFilename(input));
    }

    @Test
    void validateSensitiveDaemonPath_ShouldThrowNullPointerException_WhenPathIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> underTest.validateSensitiveDaemonPath(null)
        );
    }

    @Test
    void validateSensitiveDaemonPath_ShouldThrowSecurityException_WhenPathIsExactConfigDirectory() {
        Path configDir = Path.of("daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        assertThrows(
                SecurityException.class,
                () -> underTest.validateSensitiveDaemonPath(configDir)
        );
    }

    @Test
    void validateSensitiveDaemonPath_ShouldThrowSecurityException_WhenPathIsNestedInsideConfigDirectory() {
        Path configDir = Path.of("daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        Path nestedPath = Path.of("daemon-config", "subfolder", "settings.json");

        assertThrows(
                SecurityException.class,
                () -> underTest.validateSensitiveDaemonPath(nestedPath)
        );
    }

    @Test
    void validateSensitiveDaemonPath_ShouldThrowSecurityException_WhenPathIsUnnormalizedTraversalIntoConfigDir() {
        Path configDir = Path.of("daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        Path traversalPath = Path.of("other-dir", "..", "daemon-config", "secret.key");

        assertThrows(
                SecurityException.class,
                () -> underTest.validateSensitiveDaemonPath(traversalPath)
        );
    }

    @Test
    void validateSensitiveDaemonPath_ShouldThrowSecurityException_WhenPathIsParentOfConfigDirectory() {
        Path configDir = Path.of("parent-dir", "daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        Path parentDir = Path.of("parent-dir");

        assertThrows(
                SecurityException.class,
                () -> underTest.validateSensitiveDaemonPath(parentDir)
        );
    }

    @Test
    void validateSensitiveDaemonPath_ShouldAllowAccess_WhenPathIsOutsideConfigDirectory() {
        Path configDir = Path.of("daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        Path safePath = Path.of("user-downloads", "document.pdf");

        assertDoesNotThrow(() -> underTest.validateSensitiveDaemonPath(safePath));
    }

    @Test
    void validateSensitiveDaemonPath_ShouldAllowAccess_WhenPathSharesPrefixNameButIsSiblingDirectory() {
        Path configDir = Path.of("daemon-config");
        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        Path siblingPath = Path.of("daemon-config-public", "file.txt");

        assertDoesNotThrow(() -> underTest.validateSensitiveDaemonPath(siblingPath));
    }

    @Test
    void isSensitiveDaemonPath_ShouldReturnTrue_WhenFileIsConfigDirectoryOrInsideIt(@TempDir Path tempDir) throws Exception {
        Path configDir = Files.createDirectory(tempDir.resolve("conf"));
        Path appYml = Files.createFile(configDir.resolve("application.yml"));

        Path keysDir = Files.createDirectory(configDir.resolve("keys"));
        Path privateKey = Files.createFile(keysDir.resolve("private.key"));

        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        assertTrue(underTest.isSensitiveDaemonPath(configDir));
        assertTrue(underTest.isSensitiveDaemonPath(appYml));
        assertTrue(underTest.isSensitiveDaemonPath(privateKey));
    }

    @Test
    void isSensitiveDaemonPath_ShouldReturnFalse_WhenFileIsOutsideConfigDirectory(@TempDir Path tempDir) throws Exception {
        Path configDir = Files.createDirectory(tempDir.resolve("conf"));

        Path publicDir = Files.createDirectory(tempDir.resolve("public"));
        Path imagePng = Files.createFile(publicDir.resolve("image.png"));

        Path confPublicDir = Files.createDirectory(tempDir.resolve("conf-public"));
        Path fileTxt = Files.createFile(confPublicDir.resolve("file.txt"));

        when(directoryProvider.getDirectoryPath()).thenReturn(configDir);

        assertFalse(underTest.isSensitiveDaemonPath(imagePng));
        assertFalse(underTest.isSensitiveDaemonPath(fileTxt));
    }
}
