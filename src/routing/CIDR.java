package routing;
/**
 * This class provides utility methods for converting IP addresses between
 * string and byte array formats, as well as performing other CIDR-related operations.
 * @author uylsn
 */
public final class CIDR {
    private CIDR() {

    }
    /**
     * Converts an IP address string into a byte array.
     *
     * @param ipAddress the IP address to convert
     * @return a byte array representing the IP address
     */
    public static byte[] convertIpToByteArray(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        byte[] byteArray = new byte[4];

        for (int i = 0; i < 4; i++) {
            int partAsInt = Integer.parseInt(parts[i]);
            byteArray[i] = (byte) partAsInt;
        }

        return byteArray;
    }

    /**
     * Converts a byte array back to a string representation of the IP address.
     * @param byteArray the byte array representing the IP address
     * @return the string representation of the IP address
     */
    public static String convertByteArrayToIp(byte[] byteArray) {
        StringBuilder ipAddress = new StringBuilder();

        for (int i = 0; i < byteArray.length; i++) {
            ipAddress.append(byteArray[i] & 0xFF); // Unsigned byte conversion
            if (i < byteArray.length - 1) {
                ipAddress.append(".");
            }
        }

        return ipAddress.toString();
    }

    /**
     * Converts a byte array to an integer representation of an IP address.
     *
     * @param byteArray the byte array to convert
     * @return the integer representation of the byte array
     */
    public static int byteArrayToInt(byte[] byteArray) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (byteArray[i] & 0xFF) << ((3 - i) * 8);
        }
        return result;
    }

    /**
     * Validates if the given IP address is in the correct format.
     *
     * @param ip the IP address to validate
     * @return true if the IP is valid, false otherwise
     */
    public static boolean isValidIp(String ip) {
        // Simple IPv4 validation using a regex
        String ipPattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";

        if (!ip.matches(ipPattern)) {
            return false;
        }

        // Ensure each octet is between 0 and 255
        String[] octets = ip.split("\\.");
        for (String octet : octets) {
            int value = Integer.parseInt(octet);
            if (value < 0 || value > 255) {
                return false;
            }
        }

        return true;
    }
}
