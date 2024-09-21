package routing;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Manages the connections within and between subnets in a network.
 * Handles the addition, removal, and routing of packets between network systems.
 * @author uylsn
 */
public class ConnectionManager {

    private final Map<String, Subnet> subnets;
    private final Map<String, Map<String, Integer>> interSubnetConnections;
    private final Map<String, Set<String>> directInterSubnetConnections;

    /**
     * Constructor to initialize the ConnectionManager.
     *
     * @param subnets                Map of subnets by their base address.
     * @param interSubnetConnections  Map containing the connections between different subnets (routers).
     */
    public ConnectionManager(Map<String, Subnet> subnets, Map<String, Map<String, Integer>> interSubnetConnections) {
        this.subnets = subnets;
        this.interSubnetConnections = interSubnetConnections;
        this.directInterSubnetConnections = new HashMap<>();
    }

    /**
     * Adds a connection between two systems. If they are in the same subnet, a weighted connection is created.
     * If they are in different subnets, a connection between routers is created.
     *
     * @param ip1    The IP address of the first system.
     * @param ip2    The IP address of the second system.
     * @param weight The weight of the connection, applicable only for intra-subnet connections.
     */
    public void addConnection(String ip1, String ip2, int weight) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        if (subnet1 == null || subnet2 == null) {
            return;
        }

        if (subnet1.equals(subnet2)) {
            subnet1.addConnection(ip1, ip2, weight);
        } else {
            NetworkSystem router1 = subnet1.findNetworkSystem(ip1);
            NetworkSystem router2 = subnet2.findNetworkSystem(ip2);

            if (router1 != null && router1.isRouter() && router2 != null && router2.isRouter()) {
                directInterSubnetConnections.computeIfAbsent(ip1, k -> new HashSet<>()).add(ip2);
                directInterSubnetConnections.computeIfAbsent(ip2, k -> new HashSet<>()).add(ip1);

                interSubnetConnections.computeIfAbsent(ip1, k -> new HashMap<>()).put(ip2, 0);
                interSubnetConnections.computeIfAbsent(ip2, k -> new HashMap<>()).put(ip1, 0);
            }
        }
    }

    /**
     * Removes a connection between two systems. It removes both intra-subnet and inter-subnet connections.
     *
     * @param ip1 The IP address of the first system.
     * @param ip2 The IP address of the second system.
     */
    public void removeConnection(String ip1, String ip2) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        // Case 1: Both systems are in the same subnet
        if (subnet1 != null && subnet1.equals(subnet2)) {
            // Check if there is an actual connection to remove
            if (subnet1.hasConnection(ip1, ip2)) {
                subnet1.removeConnection(ip1, ip2);
            } else {
                System.out.println("Error, No connection exists between " + ip1 + " and " + ip2 + ".");
            }
            return;
        }
        // Case 2: Systems are in different subnets
        if (subnet1 != null && subnet2 != null) {
            // Check if there's a direct inter-subnet connection
            if (directInterSubnetConnections.containsKey(ip1) && directInterSubnetConnections.get(ip1).contains(ip2)) {
                directInterSubnetConnections.get(ip1).remove(ip2);
                if (directInterSubnetConnections.get(ip1).isEmpty()) {
                    directInterSubnetConnections.remove(ip1);
                }

                directInterSubnetConnections.get(ip2).remove(ip1);
                if (directInterSubnetConnections.get(ip2).isEmpty()) {
                    directInterSubnetConnections.remove(ip2);
                }

                interSubnetConnections.computeIfPresent(ip1, (k, v) -> {
                    v.remove(ip2);
                    return v.isEmpty() ? null : v;
                });
                interSubnetConnections.computeIfPresent(ip2, (k, v) -> {
                    v.remove(ip1);
                    return v.isEmpty() ? null : v;
                });

            } else {
                // No direct connection exists between the two IPs
                System.out.println("Error, No connection exists between " + ip1 + " and " + ip2 + ".");
            }
        } else {
            // At least one system is not in any subnet
            System.out.println("Error, One or both IP addresses are not in any subnet.");
        }
    }

    /**
     * Sends a packet from one system to another. It determines whether the systems are in the same subnet or not.
     * If the systems are in the same subnet, it uses Source Routing to precompute the entire path within the subnet.
     * If they are in different subnets, it uses Source Routing to find the path from the sender to its router,
     * between the routers of the subnets, and finally from the receiver's router to the destination.
     * @param fromIp The IP address of the sender system.
     * @param toIp   The IP address of the receiver system.
     */
    public void sendPacket(String fromIp, String toIp) {
        Subnet subnetFrom = findSubnetForIp(fromIp);
        Subnet subnetTo = findSubnetForIp(toIp);
        if (subnetFrom == null || subnetTo == null) {
            System.out.println("Error, One or both IP addresses are not in any subnet.");
            return;
        }
        if (subnetFrom.equals(subnetTo)) {
            List<String> path = subnetFrom.findShortestPath(fromIp, toIp);
            if (path != null) {
                printPath(path);
            } else {
                System.out.println("Error, No path found within the subnet.");
            }
        } else {
            String senderRouterIp = subnetFrom.getRouterIp();
            List<String> pathToRouter;
            if (fromIp.equals(senderRouterIp)) {
                pathToRouter = new ArrayList<>();
                pathToRouter.add(fromIp); // Path is just the router itself
            } else {
                pathToRouter = findShortestPath(fromIp, senderRouterIp);
            }
            if (senderRouterIp == null || pathToRouter == null) {
                System.out.println("Error, No path found to the router in the sender's subnet.");
                return;
            }
            String receiverRouterIp = subnetTo.getRouterIp();
            List<String> routerToRouterPath = findShortestInterSubnetPath(senderRouterIp, receiverRouterIp);

            if (routerToRouterPath == null || routerToRouterPath.isEmpty()) {
                System.out.println("Error, No router-to-router path found between the subnets.");
                return;
            }

            // Step 3: Check if the destination is the router itself
            if (receiverRouterIp.equals(toIp)) {
                // The destination is the router, so the entire path is from sender to its router, to receiver's router
                pathToRouter.addAll(routerToRouterPath);
                printPath(pathToRouter);
                return;
            }

            List<String> pathToReceiver = findShortestPath(receiverRouterIp, toIp);

            if (pathToReceiver == null || pathToReceiver.isEmpty()) {
                System.out.println("Error, No path found from the router to the receiver in the destination subnet.");
                return;
            }

            // Concatenate the three parts of the path and print the result
            pathToRouter.addAll(routerToRouterPath);
            pathToRouter.addAll(pathToReceiver);
            printPath(pathToRouter);
        }
    }





    /**
     * Finds the shortest path between two systems within the same subnet using Dijkstra's algorithm.
     *
     * @param fromIp The IP address of the source system.
     * @param toIp   The IP address of the destination system.
     * @return The list of IP addresses representing the shortest path.
     */
    public List<String> findShortestPath(String fromIp, String toIp) {
        Subnet subnet = findSubnetForIp(fromIp);
        if (subnet == null) {
            return null;
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(String ip1, String ip2) {
                return Integer.compare(distances.get(ip1), distances.get(ip2));
            }
        });
        Set<String> visited = new HashSet<>();

        List<NetworkSystem> networkSystems = subnet.getAllNetworkSystems();

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

            Map<String, Integer> neighbors = subnet.getConnections(current);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> neighbor : neighbors.entrySet()) {
                    String neighborIp = neighbor.getKey();
                    int weight = neighbor.getValue();

                    if (!visited.contains(neighborIp)) {
                        int newDist = distances.get(current) + weight;
                        if (newDist < distances.get(neighborIp)) {
                            distances.put(neighborIp, newDist);
                            previousNodes.put(neighborIp, current);
                            pq.add(neighborIp);
                        }
                    }
                }
            }
        }

        List<String> path = new LinkedList<>();
        for (String at = toIp; at != null; at = previousNodes.get(at)) {
            path.add(0, at);
        }

        return path.size() == 1 ? null : path;
    }

    /**
     * Finds the shortest inter-subnet path between two routers using Dijkstra's algorithm.
     *
     * @param fromRouterIp The IP address of the source router.
     * @param toRouterIp   The IP address of the destination router.
     * @return The list of router IP addresses representing the shortest path.
     */
    private List<String> findShortestInterSubnetPath(String fromRouterIp, String toRouterIp) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(String ip1, String ip2) {
                int distanceComparison = Integer.compare(distances.get(ip1), distances.get(ip2));

                // If the distances are equal, prioritize alphabetically smaller router IP
                if (distanceComparison == 0) {
                    return ip1.compareTo(ip2);  // Alphabetical comparison
                }

                return distanceComparison;
            }
        });
        Set<String> visited = new HashSet<>();

        // Initialize distances
        for (String routerIp : directInterSubnetConnections.keySet()) {
            distances.put(routerIp, Integer.MAX_VALUE);
        }
        distances.put(fromRouterIp, 0);  // Start from the source router
        pq.add(fromRouterIp);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            visited.add(current);

            if (current.equals(toRouterIp)) {
                break;  // Reached the destination
            }

            Set<String> neighbors = directInterSubnetConnections.get(current);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        int weight = getWeight(current, neighbor);  // Retrieve actual weight
                        int newDist = distances.get(current) + weight;
                        if (newDist < distances.get(neighbor)) {
                            distances.put(neighbor, newDist);
                            previousNodes.put(neighbor, current);
                            pq.add(neighbor);
                        }
                    }
                }
            }
        }

        // Reconstruct the shortest path
        List<String> path = new LinkedList<>();
        for (String at = toRouterIp; at != null; at = previousNodes.get(at)) {
            path.add(0, at);
        }

        return path.size() == 1 ? null : path;  // Return null if no path found
    }


    /**
     * Retrieves the weight of the connection between two routers.
     *
     * @param fromRouterIp The IP address of the source router.
     * @param toRouterIp   The IP address of the destination router.
     * @return The weight of the connection, or Integer.MAX_VALUE if no connection exists.
     */
    private int getWeight(String fromRouterIp, String toRouterIp) {
        Map<String, Integer> connectionsFrom = interSubnetConnections.get(fromRouterIp);
        if (connectionsFrom != null && connectionsFrom.containsKey(toRouterIp)) {
            return connectionsFrom.get(toRouterIp);
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Prints the path of the packet, ensuring there are no duplicate consecutive IPs.
     *
     * @param path The list of IP addresses representing the path.
     */
    private void printPath(List<String> path) {
        List<String> distinctPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            if (i == 0 || !path.get(i).equals(path.get(i - 1))) {
                distinctPath.add(path.get(i));
            }
        }

        System.out.println(String.join(" ", distinctPath));
    }
    /**
     * Finds the subnet that contains the given IP address.
     *
     * @param ip The IP address to search for.
     * @return The Subnet containing the IP address, or null if not found.
     */
    Subnet findSubnetForIp(String ip) {
        for (Map.Entry<String, Subnet> entry : subnets.entrySet()) {
            Subnet subnet = entry.getValue();
            if (subnet.containsIp(ip)) {
                return subnet;
            }
        }
        return null;
    }
    /**
     * Finds a network system by its IP address.
     *
     * @param ip The IP address of the system.
     * @return The NetworkSystem with the given IP address, or null if not found.
     */
    public NetworkSystem findSystemByIp(String ip) {
        for (Subnet subnet : subnets.values()) {
            NetworkSystem system = subnet.findNetworkSystem(ip);
            if (system != null) {
                return system;
            }
        }
        return null;
    }
}

