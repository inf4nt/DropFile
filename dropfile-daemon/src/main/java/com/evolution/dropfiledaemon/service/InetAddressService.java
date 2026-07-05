package com.evolution.dropfiledaemon.service;

import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Component
public class InetAddressService {

    @Nullable
    @SneakyThrows
    public ConnectionAddress getConnectionAddress() {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        List<BestLocalAddress> wifi = new ArrayList<>();
        List<BestLocalAddress> ethernet = new ArrayList<>();

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
                    BestLocalAddress bestLocalAddress = new BestLocalAddress(ifaceNameDisplay, addr);
                    wifi.add(bestLocalAddress);
                }

                String ethernetMatch = getEthernet(name, display);
                if (ethernetMatch != null) {
                    BestLocalAddress bestLocalAddress = new BestLocalAddress(ifaceNameDisplay, addr);
                    ethernet.add(bestLocalAddress);
                }
            }
        }

        if (!wifi.isEmpty() || !ethernet.isEmpty()) {
            return new ConnectionAddress(wifi.stream().findFirst().orElse(null), ethernet.stream().findFirst().orElse(null));
        }

        return null;
    }

    private String getWifi(String name, String display) {
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

    public record BestLocalAddress(String ifaceNameDisplay, InetAddress inetAddress) {
    }

    public record ConnectionAddress(@Nullable BestLocalAddress wireless, @Nullable BestLocalAddress ethernet) {
    }

}
