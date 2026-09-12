package com.evolution.dropfiledaemon.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafePathResolverTest {

    @ParameterizedTest
    @MethodSource("provideSanitizationCases")
    void shouldSanitizeFilenameCorrectly(String input, String expected) {
        assertEquals(expected, SafePathResolver.sanitizeFilename(input));
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
        assertThrows(IllegalArgumentException.class, () -> SafePathResolver.sanitizeFilename(input));
    }
}