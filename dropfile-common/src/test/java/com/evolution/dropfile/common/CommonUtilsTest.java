package com.evolution.dropfile.common;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommonUtilsTest {

    @Test
    public void nonce12() {
        byte[] nonce12V1 = CommonUtils.nonce12();
        assertThat(nonce12V1.length, is(12));
        assertThat(
                IntStream.range(0, nonce12V1.length)
                        .noneMatch(i -> nonce12V1[i] == 0),
                is(true)
        );

        byte[] nonce12V2 = CommonUtils.nonce12();
        assertThat(nonce12V2.length, is(12));
        assertThat(
                IntStream.range(0, nonce12V2.length)
                        .noneMatch(i -> nonce12V2[i] == 0),
                is(true)
        );

        assertThat(Arrays.equals(nonce12V1, nonce12V2), is(false));
    }

    @Test
    public void toDisplaySize() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtils.toDisplaySize(-1));

        assertThat(
                CommonUtils.toDisplaySize(0),
                is("0B")
        );

        assertThat(
                CommonUtils.toDisplaySize(1023),
                is("1023B")
        );

        assertThat(
                CommonUtils.toDisplaySize(12 * 1024),
                is("12.00KB")
        );

        assertThat(
                CommonUtils.toDisplaySize(123 * 5_794),
                is("695.96KB")
        );

        assertThat(
                CommonUtils.toDisplaySize(1023 * 1024),
                is("1023.00KB")
        );

        assertThat(
                CommonUtils.toDisplaySize(1 * 1024 * 1024),
                is("1.00MB")
        );

        assertThat(
                CommonUtils.toDisplaySize(13 * 395_794),
                is("4.91MB")
        );

        assertThat(
                CommonUtils.toDisplaySize(1023 * 1024 * 1024),
                is("1023.00MB")
        );

        assertThat(
                CommonUtils.toDisplaySize(5_418_395_794L),
                is("5.05GB")
        );

        assertThat(
                CommonUtils.toDisplaySize(14 * 5_418_395_794L),
                is("70.65GB")
        );

        assertThat(
                CommonUtils.toDisplaySize(1023 * 5_418_395_794L),
                is("5162.34GB")
        );
    }

    @Test
    public void executeSafety() {
        assertDoesNotThrow(() -> {
            CommonUtils.executeSafety(() -> {
                throw new Exception();
            });
        });
    }

    @Test
    public void getFingerprint() {
        for (int i = 0; i < 100; i++) {
            assertThat(
                    CommonUtils.getFingerprint("abc".getBytes()),
                    is("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
            );
        }

        for (int i = 0; i < 100; i++) {
            assertThat(
                    CommonUtils.getFingerprint("abcdefg".getBytes()),
                    is("7d1a54127b222502f5b79b5fb0803061152a44f92b37e23c6527baf665d4da9a")
            );
        }
    }

    @Test
    public void toURI() {
        assertThat(
                CommonUtils.toURI("google.com"),
                is(URI.create("http://google.com"))
        );

        assertThat(
                CommonUtils.toURI("http://google.com"),
                is(URI.create("http://google.com"))
        );

        assertThat(
                CommonUtils.toURI("https://google.com"),
                is(URI.create("https://google.com"))
        );

        assertThat(
                CommonUtils.toURI("google.com", 8080),
                is(URI.create("http://google.com:8080"))
        );

        assertThat(
                CommonUtils.toURI("http://google.com", 8081),
                is(URI.create("http://google.com:8081"))
        );

        assertThat(
                CommonUtils.toURI("https://google.com", 8082),
                is(URI.create("https://google.com:8082"))
        );
    }

    @Test
    public void encodeBase64() {
        assertThat(
                CommonUtils.encodeBase64("123abc".getBytes()),
                is("MTIzYWJj")
        );
        assertThat(
                CommonUtils.encodeBase64("abcdefg".getBytes()),
                is("YWJjZGVmZw")
        );
    }

    @Test
    public void decodeBase64() {
        assertThat(
                CommonUtils.decodeBase64("MTIzYWJj"),
                is("123abc".getBytes())
        );
        assertThat(
                CommonUtils.decodeBase64("YWJjZGVmZw"),
                is("abcdefg".getBytes())
        );
    }
}
