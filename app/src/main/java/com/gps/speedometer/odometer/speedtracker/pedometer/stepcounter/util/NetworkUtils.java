package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();

        for (byte aByte : bytes) {
            int intVal = aByte & 0xff;

            if (intVal < 0x10)
                stringBuilder.append("0");

            stringBuilder.append(Integer.toHexString(intVal).toUpperCase());
        }
        return stringBuilder.toString();
    }

    @SuppressLint("DefaultLocale")
    public static String convertInet4Address(int address) {
        return String.format("%d.%d.%d.%d", (address & 0xff), (address >> 8 & 0xff), (address >> 16 & 0xff), (address >> 24 & 0xff));
    }

    public static String getAddressPrefix(String ipv4Address) {
        return ipv4Address.substring(0, ipv4Address.lastIndexOf(".") + 1);
    }

    public static List<String> getMACAddressList(String interfaceName) {
        List<String> macAddressList = new ArrayList<>();

        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface networkInterface : interfaceList) {
                if (interfaceName != null) {
                    if (!networkInterface.getName().equalsIgnoreCase(interfaceName))
                        continue;
                }

                byte[] hardwareAddress = networkInterface.getHardwareAddress();

                if (hardwareAddress == null)
                    continue;

                StringBuilder stringBuilder = new StringBuilder();

                for (byte partedHardwareAddress : hardwareAddress)
                    stringBuilder.append(String.format("%02X:", partedHardwareAddress));

                if (stringBuilder.length() > 0)
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);

                macAddressList.add(stringBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return macAddressList;
    }

    public static int getAllowedKeyManagement(WifiConfiguration wifiConfiguration) {
        String keyManagement = wifiConfiguration.allowedKeyManagement.toString();

        try {
            return Integer.valueOf(keyManagement.substring(1, keyManagement.length() - 1));
        } catch (Exception e) {
        }

        return -1;
    }

    @SuppressLint("NewApi")
    public static byte[] getUTF8Bytes(String string) {
        try {
            return string.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressLint("NewApi")
    public static String loadFileAsString(String filename) throws IOException {
        final int BUFLEN = 1024;

        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8 = false;
            int read, count = 0;

            while ((read = is.read(bytes)) != -1) {
                if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    isUTF8 = true;
                    baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }

                count += read;
            }
            return isUTF8 ? new String(baos.toByteArray(), StandardCharsets.UTF_8) : new String(baos.toByteArray());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean ping(String ipAddress, int timeout) {
        try {
            return InetAddress.getByName(ipAddress).isReachable(timeout);
        } catch (IOException e1) {
            e1.printStackTrace();
            try {
                Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 100 " + ipAddress);
                int status = process.waitFor();
                return status == 0;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static boolean ping(String ipAddress) {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 100 " + ipAddress);
            int status = process.waitFor();
            return status == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean testSocket(String ip, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
        Socket socket = new Socket();

        try {
            socket.bind(null);
            socket.connect(socketAddress);
            socket.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }
}
