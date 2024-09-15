package routing;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * This class represents a subnet in the network. It manages network systems (computers and routers),
 * handles connections between systems, and provides utilities for subnet-specific tasks such as IP
 * validation and shortest path calculation.
 * @author uylsn
 */
public class Subnet {
    private final String baseAddress; // Base address of the subnet
    private final int netmask; // Netmask for the subnet
    private final List<NetworkSystem> networkSystems = new ArrayList<>(); // List of all network systems (computers, routers) in the subnet
    private final Map<String, Map<String, Integer>> connections = new HashMap<>(); // Store connections between systems and their weights

    /**
     * Constructor to create a new subnet with the specified base address and netmask.
     * @param baseAddress the base IP address of the subnet
     * @param netmask     the netmask of the subnet
     */
    public Subnet(String baseAddress, int netmask) {
        this.baseAddress = baseAddress;
        this.netmask = netmask;
    }

    /**
     * Returns the base IP address of the subnet.
     *
     * @return the base address of the subnet
     */
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Returns the netmask of the subnet.
     *
     * @return the netmask of the subnet
     */
    public int getNetmask() {
        return netmask;
    }

    /**
     * Returns the IP range of the subnet in CIDR notation.
     * @return the range of the subnet as a string
     */
    public String getRange() {
        return baseAddress + "/" + netmask;
    }

    /**
     * Adds a network system (computer or router) to the subnet.
     * @param networkSystem the network system to add
     */
    public void addNetworkSystem(NetworkSystem networkSystem) {
        networkSystems.add(networkSystem);
        connections.put(networkSystem.ip(), new HashMap<>()); // Initialize empty connections for each system
    }

    /**
     * Returns the list of network systems within the subnet.
     * @return a list of network systems
     */
    public List<NetworkSystem> getNetworkSystems() {
        return networkSystems;
    }

    /**
     * Adds a connection between two systems within the subnet with a specified weight.
     * @param system1 the IP address of the first system
     * @param system2 the IP address of the second system
     * @param weight  the weight of the connection
     */
    public void addConnection(String system1, String system2, int weight) {
        NetworkSystem ns1 = findNetworkSystem(system1);
        NetworkSystem ns2 = findNetworkSystem(system2);

        if (ns1 == null || ns2 == null) {
            System.out.println("Error, One or both systems not found.");
            return;
        }

        // Check if the connection already exists
        if (connections.containsKey(system1) && connections.get(system1).containsKey(system2)) {
            System.out.println("Error, The systems are already connected. Connection: " + system1 + " <--> " + system2);
            return;
        }

        // Intra-subnet connection: requires weight
        if (weight == 0) {
            System.out.println("Error, Intra-subnet connections require a weight.");
            return;
        }

        // Add the connection with the weight between the systems
        connections.computeIfAbsent(system1, k -> new HashMap<>()).put(system2, weight);
        connections.computeIfAbsent(system2, k -> new HashMap<>()).put(system1, weight);

    }
    /**
     * Removes a connection between two systems within the subnet.
     * @param system1 the IP address of the first system
     * @param system2 the IP address of the second system
     */
    public void removeConnection(String system1, String system2) {
        // Check if both systems exist in the connections map
        if (connections.containsKey(system1) && connections.get(system1).containsKey(system2)) {
            // Remove the connection
            connections.get(system1).remove(system2);
            connections.get(system2).remove(system1);

            // Clean up any empty maps
            if (connections.get(system1).isEmpty()) {
                connections.remove(system1);
            }
            if (connections.get(system2).isEmpty()) {
                connections.remove(system2);
            }

        } else {
            System.out.println("Error, No connection exists between " + system1 + " and " + system2 + ".");
        }

    }
    /**
     * Removes a computer from the subnet.
     * @param ip the IP address of the computer to remove
     * @return true if the computer was removed, false if the system is a router or not found
     */
    public boolean removeComputer(String ip) {
        // Find the system to remove
        NetworkSystem systemToRemove = findNetworkSystem(ip);
        if (systemToRemove.isRouter()) {
            return false;
        }

        // Remove the system from the network systems list
        networkSystems.remove(systemToRemove);

        // Remove any connections involving this system
        connections.remove(ip); // Remove connections where this system is the key
        for (Map<String, Integer> connectionMap : connections.values()) {
            connectionMap.remove(ip); // Remove references to this system in other connections
        }

        return true;
    }

    /**
     * Gets the last possible IP address in the subnet (broadcast address).
     *
     * @return the last IP address as a string
     */
    public String getLastIp() {
        // Convert the base address to a byte array using CIDR
        byte[] ipBytes = CIDR.convertIpToByteArray(baseAddress);

        // Calculate the number of host bits (32 - netmask)
        int hostBits = 32 - netmask;

        // Set the host bits to 1 to get the last IP (broadcast address)
        for (int i = ipBytes.length - 1; i >= 0 && hostBits > 0; i--) {
            int bitsToSet = Math.min(8, hostBits);  // Max 8 bits per byte
            ipBytes[i] |= (byte) ((1 << bitsToSet) - 1);     // Set the rightmost 'bitsToSet' bits to 1
            hostBits -= bitsToSet;
        }

        // Convert the byte array back to a string representation of the IP using CIDR
        return CIDR.convertByteArrayToIp(ipBytes);
    }

    /**
     * Checks whether the provided IP address is within the subnet.
     * @param ip the IP address to check
     * @return true if the IP is within the subnet, false otherwise
     */
    public boolean containsIp(String ip) {

        // Validate IP address format using the CIDR class
        if (!CIDR.isValidIp(ip) || !CIDR.isValidIp(baseAddress)) {
            System.out.println("Error, Invalid IP address format.");
            return false;
        }

        // Convert the base address and the given IP address to byte arrays using CIDR
        byte[] baseAddressBytes = CIDR.convertIpToByteArray(baseAddress);
        byte[] ipBytes = CIDR.convertIpToByteArray(ip);

        // Ensure that the byte arrays are of the correct length (IPv4 length should be 4 bytes)
        if (baseAddressBytes.length != 4 || ipBytes.length != 4) {
            System.out.println("Error, Invalid IP address length.");
            return false;
        }

        // Calculate the network mask based on the subnet's netmask
        int shift = 32 - netmask;
        int mask = ~((1 << shift) - 1);

        // Convert byte arrays to integers using CIDR
        int baseAddressInt = CIDR.byteArrayToInt(baseAddressBytes);
        int ipInt = CIDR.byteArrayToInt(ipBytes);

        // Check if the given IP is within the subnet's range
        return (baseAddressInt & mask) == (ipInt & mask);
    }
    /**
     * Finds the shortest path between two IP addresses in the subnet.
     * @param fromIp the source IP address
     * @param toIp   the destination IP address
     * @return a list of IP addresses representing the path, or null if no path was found
     */
    public List<String> findShortestPath(String fromIp, String toIp) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();

        // Custom priority queue without using Comparator.comparingInt
        PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(String ip1, String ip2) {
                return Integer.compare(distances.get(ip1), distances.get(ip2));
            }
        });

        Set<String> visited = new HashSet<>();

        for (NetworkSystem system : networkSystems) {
            distances.put(system.ip(), Integer.MAX_VALUE);
        }
        distances.put(fromIp, 0);
        pq.add(fromIp);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            visited.add(current);

            if (current.equals(toIp)) {
                break;
            }

            Map<String, Integer> neighbors = connections.get(current);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> neighbor : neighbors.entrySet()) {
                    if (!visited.contains(neighbor.getKey())) {
                        int newDist = distances.get(current) + neighbor.getValue();
                        if (newDist < distances.get(neighbor.getKey())) {
                            distances.put(neighbor.getKey(), newDist);
                            previousNodes.put(neighbor.getKey(), current);
                            pq.add(neighbor.getKey());
                        }
                    }
                }
            }
        }

        // Reconstruct the shortest path from `fromIp` to `toIp`
        List<String> path = new LinkedList<>();
        for (String at = toIp; at != null; at = previousNodes.get(at)) {
            path.add(0, at);
        }

        return path.size() == 1 ? null : path;  // Return null if no path was found
    }


    /**
     * Returns the router IP for the current subnet.
     *
     * @return the router IP address, or null if no router is found
     */
    public String getRouterIp() {
        for (NetworkSystem system : networkSystems) {
            if (system.isRouter()) {
                return system.ip();
            }
        }
        return null;  // Return null if no router is found
    }


    /**
     * Finds an IP address by the network system's name.
     * @param name the name of the network system
     * @return the IP address of the system, or null if not found
     */
    public String findIpByName(String name) {
        for (NetworkSystem system : networkSystems) {
            if (system.ip().contains(name)) {
                return system.ip();
            }
        }
        return null; // Return null if the device name is not found
    }


    /**
     * Finds a network system by IP address within the subnet.
     * @param ip the IP address of the network system
     * @return the NetworkSystem object if found, or null if not found
     */
    public NetworkSystem findNetworkSystem(String ip) {
        for (NetworkSystem system : networkSystems) {
            if (system.ip().equals(ip)) {
                return system;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return baseAddress + "/" + netmask;
    }
}
