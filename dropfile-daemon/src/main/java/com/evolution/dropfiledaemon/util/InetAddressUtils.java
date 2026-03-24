package com.evolution.dropfiledaemon.util;

import lombok.SneakyThrows;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class InetAddressUtils {

    @SneakyThrows
    public static InetAddress getBestLocalAddress() {
        List<InetAddress> wifi = new ArrayList<>();
        List<InetAddress> ethernet = new ArrayList<>();
        List<InetAddress> others = new ArrayList<>();

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                continue;
            }

            String name = iface.getName().toLowerCase();
            String display = iface.getDisplayName().toLowerCase();

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                if (!(addr instanceof Inet4Address)) {
                    continue;
                }
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                    continue;
                }

                if (isWifi(name, display)) {
                    wifi.add(addr);
                } else if (isEthernet(name, display)) {
                    ethernet.add(addr);
                } else {
                    others.add(addr);
                }
            }
        }

        if (!wifi.isEmpty()) {
            return wifi.getFirst();
        }
        if (!ethernet.isEmpty()) {
            return ethernet.getFirst();
        }
        if (!others.isEmpty()) {
            return others.getFirst();
        }

        throw new IllegalStateException("No suitable IPv4 address found");
    }

//    @SneakyThrows
//    public static InetAddress getBestLocalAddress() {
//        List<InetAddress> wifi = new ArrayList<>();
//        List<InetAddress> ethernet = new ArrayList<>();
//        List<InetAddress> others = new ArrayList<>();
//
//        Enumeration<NetworkInterface> interfaces =
//                NetworkInterface.getNetworkInterfaces();
//
//        while (interfaces.hasMoreElements()) {
//            NetworkInterface iface = interfaces.nextElement();
//
//            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
//                continue;
//            }
//
//            String name = iface.getName().toLowerCase();
//            String display = iface.getDisplayName().toLowerCase();
//
//            Enumeration<InetAddress> addresses = iface.getInetAddresses();
//            while (addresses.hasMoreElements()) {
//                InetAddress addr = addresses.nextElement();
//
//                if (!(addr instanceof Inet4Address)) {
//                    continue;
//                }
//                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
//                    continue;
//                }
//
//                if (isWifi(name, display)) {
//                    wifi.add(addr);
//                } else if (isEthernet(name, display)) {
//                    ethernet.add(addr);
//                } else {
//                    others.add(addr);
//                }
//            }
//        }
//
//        if (!wifi.isEmpty()) {
//            return wifi.getFirst();
//        }
//        if (!ethernet.isEmpty()) {
//            return ethernet.getFirst();
//        }
//        if (!others.isEmpty()) {
//            return others.getFirst();
//        }
//
//        throw new IllegalStateException("No suitable IPv4 address found");
//    }

    private static boolean isWifi(String name, String display) {
        return display.contains("wi-fi")
                || display.contains("wifi")
                || display.contains("wlan")
                || display.contains("wireless")
                || name.startsWith("wlan")
                || name.startsWith("wl");
    }

    private static boolean isEthernet(String name, String display) {
        return display.contains("ethernet")
                || display.contains("eth")
                || name.startsWith("eth")
                || name.startsWith("en");
    }
}
