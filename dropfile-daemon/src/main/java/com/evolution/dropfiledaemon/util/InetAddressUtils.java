package com.evolution.dropfiledaemon.util;

import lombok.SneakyThrows;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

public class InetAddressUtils {

    public record BestLocalAddress(String name, String ifaceNameDisplay, InetAddress inetAddress) {}

    @SneakyThrows
    public static BestLocalAddress getBestLocalAddress() {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                continue;
            }

            String name = iface.getName().toLowerCase();
            String display = iface.getDisplayName().toLowerCase();

            String ifaceNameDisplay = name + ":" + display;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                if (!(addr instanceof Inet4Address)) {
                    continue;
                }
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                    continue;
                }

                String wifiMatch = getWifi(name, display);
                if (wifiMatch != null) {
                    return new BestLocalAddress(wifiMatch, ifaceNameDisplay, addr);
                }

                String ethernetMatch = getEthernet(name, display);
                if (ethernetMatch != null) {
                    return new BestLocalAddress(ethernetMatch, ifaceNameDisplay, addr);
                }
            }
        }

        throw new IllegalStateException("No suitable IPv4 address found");
    }

    private static String getWifi(String name, String display) {
        if (display.contains("wi-fi")) {
            return "wifi";
        }
        if (display.contains("wifi")) {
            return "wifi";
        }
        if (display.contains("wlan")) {
            return "wlan";
        }
        if (display.contains("wireless")) {
            return "wireless";
        }
        if (name.contains("wlan")) {
            return "wlan";
        }
        if (name.contains("wl")) {
            return "wl";
        }
        return null;
    }

    private static String getEthernet(String name, String display) {
        if (display.contains("ethernet")) {
            return "ethernet";
        }
        if (display.contains("eth")) {
            return "eth";
        }
        if (name.contains("wlan")) {
            return "wlan";
        }
        if (name.contains("wl")) {
            return "wl";
        }
        return null;
    }
}
