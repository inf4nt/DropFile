package com.evolution.dropfile.common;


import java.net.URI;
import java.util.UUID;

public class CommonUtils {

    public static String random() {
        return UUID.randomUUID().toString()
                .replaceAll("-", "")
                .substring(0, 8);
    }

    public static URI toURI(String host) {
        if (!host.startsWith("http://") || !host.startsWith("https://")) {
            return URI.create("http://" + host);
        }
        return URI.create(host);
    }

    public static URI toURI(String host, Integer port) {
        if (port == null) {
            return toURI(host);
        }
        return toURI(host + ":" + port);
    }
}
