package com.evolution.dropfile.configuration;


import java.net.URI;
import java.util.UUID;

public class CommonUtils {

    public static String random() {
        return UUID.randomUUID().toString()
                .replaceAll("-", "")
                .substring(0, 8);
    }

    public static URI toURI(String address) {
        if (!address.startsWith("http://") || !address.startsWith("https://")) {
            return URI.create("http://" + address);
        }
        return URI.create(address);
    }
}
