//package com.evolution.dropfiledaemon.facade;
//
//import com.evolution.dropfile.common.CommonUtils;
//import com.evolution.dropfile.store.app.AppConfigStore;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Component;
//
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//
//@RequiredArgsConstructor
//@Component
//public class ApiLinkFacade {
//
//    private final Environment environment;
//
//    private final AppConfigStore appConfigStore;
//
//    public InetAddress getBestLocalAddress() {
//        try {
//            List<InetAddress> wifi = new ArrayList<>();
//            List<InetAddress> ethernet = new ArrayList<>();
//            List<InetAddress> others = new ArrayList<>();
//
//            Enumeration<NetworkInterface> interfaces =
//                    NetworkInterface.getNetworkInterfaces();
//
//            while (interfaces.hasMoreElements()) {
//                NetworkInterface iface = interfaces.nextElement();
//
//                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
//                    continue;
//                }
//
//                String name = iface.getName().toLowerCase();
//                String display = iface.getDisplayName().toLowerCase();
//
//                Enumeration<InetAddress> addresses = iface.getInetAddresses();
//                while (addresses.hasMoreElements()) {
//                    InetAddress addr = addresses.nextElement();
//
//                    if (!(addr instanceof Inet4Address)) continue;
//                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
//
//                    if (isWifi(name, display)) {
//                        wifi.add(addr);
//                    } else if (isEthernet(name, display)) {
//                        ethernet.add(addr);
//                    } else {
//                        others.add(addr);
//                    }
//                }
//            }
//
//            if (!wifi.isEmpty()) return wifi.get(0);
//            if (!ethernet.isEmpty()) return ethernet.get(0);
//            if (!others.isEmpty()) return others.get(0);
//
//            throw new IllegalStateException("No suitable IPv4 address found");
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to resolve local IP address", e);
//        }
//    }
//
//    private boolean isWifi(String name, String display) {
//        return display.contains("wi-fi")
//                || display.contains("wifi")
//                || display.contains("wlan")
//                || display.contains("wireless")
//                || name.startsWith("wlan")
//                || name.startsWith("wl");
//    }
//
//    private boolean isEthernet(String name, String display) {
//        return display.contains("ethernet")
//                || display.contains("eth")
//                || name.startsWith("eth")
//                || name.startsWith("en");
//    }
//
//
//    public Object add() {
//        InetAddress address = getBestLocalAddress();
//        String host = address.getHostAddress();
//
//        URI uri = CommonUtils.toURI(host, appConfigStore.getRequired().daemonAppConfig().daemonPort());
//
//        return uri.resolve("/link/download");
//    }
//}
