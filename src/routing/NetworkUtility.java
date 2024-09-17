package routing;

/**
 * Utility class for network-related helper methods.
 * @author uylsn
 */
public final class NetworkUtility {
    private NetworkUtility() {

    }

    /**
     * Converts an IP address from its string format to a long for easier numerical sorting.
     * @param ipAddress The IP address in string format (e.g., "192.168.1.1").
     * @return A long representing the IP address.
     */
    public static long convertIpToLong(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        long ipAsLong = 0;
        for (String ipPart : ipParts) {
            ipAsLong = (ipAsLong << 8) + Integer.parseInt(ipPart);
        }
        return ipAsLong;
    }

    /**
     * Handles the case where a device is missing from the device map.
     * @param deviceName The name of the missing device.
     * @return Error string.
     */
    public static String handleMissingDevice(String deviceName) {
        System.out.println("Error, Device '" + deviceName + "' not found in the map.");
        return "";
    }

    /**
     * Checks if the IPs are in the same subnet.
     * @return boolean value
     * @param ip1 first ip
     * @param ip2 second ip
     * @param connectionManager object for the connection Manager class.
     */
    public static boolean areIpsInSameSubnet(String ip1, String ip2, ConnectionManager connectionManager) {
        Subnet subnet1 = connectionManager.findSubnetForIp(ip1);
        Subnet subnet2 = connectionManager.findSubnetForIp(ip2);
        if (subnet1 == null || subnet2 == null || !subnet1.equals(subnet2)) {
            System.out.println("Error, IPs '" + ip1 + "' and '" + ip2 + "' are not in the same subnet.");
            return false;
        }
        return true;
    }
    /**
     * Checks if the IPs are in the different subnet.
     * @return boolean value
     * @param ip1 first ip
     * @param ip2 second ip
     * @param connectionManager object for the connection Manager class.
     */
    public static boolean areIpsInDifferentSubnets(String ip1, String ip2, ConnectionManager connectionManager) {
        Subnet subnet1 = connectionManager.findSubnetForIp(ip1);
        Subnet subnet2 = connectionManager.findSubnetForIp(ip2);
        if (subnet1 == null || subnet2 == null || subnet1.equals(subnet2)) {
            System.out.println("Error, Inter-subnet connection requires IPs from different subnets.");
            return false;
        }
        return true;
    }
}
