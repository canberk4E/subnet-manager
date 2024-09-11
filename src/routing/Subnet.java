package routing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Subnet {

    private final String baseAddress;
    private final int netmask;
    private final List<NetworkSystem> networkSystems = new ArrayList<>();

    public Subnet(String baseAddress, int netmask) {
        this.baseAddress = baseAddress;
        this.netmask = netmask;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public int getNetmask() {
        return netmask;
    }

    public String getRange() {
        return baseAddress + "/" + netmask;
    }

    public void addNetworkSystem(NetworkSystem networkSystem) {
        networkSystems.add(networkSystem);
    }

    public List<NetworkSystem> getNetworkSystems() {
        return networkSystems;
    }

    public void addConnection(String system1, String system2, int weight) {
    }

    public String getLastIp() {
        // Convert the base address to a byte array
        byte[] ipBytes = convertIpToByteArray(baseAddress);

        // Calculate the number of host bits (32 - netmask)
        int hostBits = 32 - netmask;

        // Set the host bits to 1 to get the last IP (broadcast address)
        for (int i = ipBytes.length - 1; i >= 0 && hostBits > 0; i--) {
            int bitsToSet = Math.min(8, hostBits);  // Max 8 bits per byte
            ipBytes[i] |= (byte) ((1 << bitsToSet) - 1);     // Set the rightmost 'bitsToSet' bits to 1
            hostBits -= bitsToSet;
        }

        // Convert the byte array back to a string representation of the IP
        return convertByteArrayToIp(ipBytes);
    }

    // Method to convert an IP address string into a byte array
    private byte[] convertIpToByteArray(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        byte[] byteArray = new byte[4];

        for (int i = 0; i < 4; i++) {
            int partAsInt = Integer.parseInt(parts[i]);
            byteArray[i] = (byte) partAsInt;
        }

        return byteArray;
    }

    // Method to convert a byte array back to a string representation of the IP address
    private String convertByteArrayToIp(byte[] byteArray) {
        StringBuilder ipAddress = new StringBuilder();

        for (int i = 0; i < byteArray.length; i++) {
            ipAddress.append(byteArray[i] & 0xFF); // Unsigned byte conversion
            if (i < byteArray.length - 1) {
                ipAddress.append(".");
            }
        }

        return ipAddress.toString();
    }

    @Override
    public String toString() {
        return baseAddress + "/" + netmask;
    }
}