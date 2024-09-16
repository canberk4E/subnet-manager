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
 * Responsible for adding/removing connections and sending packets between systems.
 * @author uylsn
 */
public class ConnectionManager {

    private final Map<String, Subnet> subnets;
    private final Map<String, Map<String, Integer>> interSubnetConnections;

    /**
     * Constructor for ConnectionManager.
     *
     * @param subnets                Map of subnets by base address.
     * @param interSubnetConnections  Map of inter-subnet connections between routers.
     */
    public ConnectionManager(Map<String, Subnet> subnets, Map<String, Map<String, Integer>> interSubnetConnections) {
        this.subnets = subnets;
        this.interSubnetConnections = interSubnetConnections;
    }

    /**
     * Adds a connection between two IPs. Connections within the same subnet will have a weight,
     * while connections between routers in different subnets will have no weight.
     *
     * @param ip1    The IP address of the first system.
     * @param ip2    The IP address of the second system.
     * @param weight The weight of the connection (only applicable for intra-subnet).
     */
    public void addConnection(String ip1, String ip2, int weight) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        if (subnet1 == null || subnet2 == null) {
            System.out.println("Error, One or both IPs not found in any subnet.");
            return;
        }

        if (subnet1.equals(subnet2)) {
            subnet1.addConnection(ip1, ip2, weight);
        } else {
            NetworkSystem router1 = subnet1.findNetworkSystem(ip1);
            NetworkSystem router2 = subnet2.findNetworkSystem(ip2);

            if (router1 != null && router1.isRouter() && router2 != null && router2.isRouter()) {
                interSubnetConnections.computeIfAbsent(ip1, k -> new HashMap<>()).put(ip2, 0);
                interSubnetConnections.computeIfAbsent(ip2, k -> new HashMap<>()).put(ip1, 0);
            } else {
                System.out.println("Error, Inter-subnet connections are only allowed between routers.");
            }
        }
    }

    /**
     * Removes a connection between two IPs. If the connection is intra-subnet, it is removed within the subnet.
     * For inter-subnet connections, the connection is removed between routers.
     * @param ip1 The IP address of the first system.
     * @param ip2 The IP address of the second system.
     */
    public void removeConnection(String ip1, String ip2) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        if (subnet1 == null || subnet2 == null) {
            System.out.println("Error, One or both IPs not found in any subnet.");
            return;
        }

        if (subnet1.equals(subnet2)) {
            subnet1.removeConnection(ip1, ip2);
        } else {
            NetworkSystem router1 = subnet1.findNetworkSystem(ip1);
            NetworkSystem router2 = subnet2.findNetworkSystem(ip2);

            if (router1 != null && router1.isRouter() && router2 != null && router2.isRouter()) {
                interSubnetConnections.computeIfPresent(ip1, (k, v) -> {
                    v.remove(ip2);
                    return v.isEmpty() ? null : v;
                });
                interSubnetConnections.computeIfPresent(ip2, (k, v) -> {
                    v.remove(ip1);
                    return v.isEmpty() ? null : v;
                });
            } else {
                System.out.println("Error, Inter-subnet connections can only be removed between routers.");
            }
        }
    }

    /**
     * Sends a packet from one system to another. The method determines whether the packet
     * is being sent within the same subnet or across different subnets and handles accordingly.
     *
     * @param fromIp The IP address of the sender.
     * @param toIp   The IP address of the receiver.
     */
    public void sendPacket(String fromIp, String toIp) {
        Subnet subnetFrom = findSubnetForIp(fromIp);
        Subnet subnetTo = findSubnetForIp(toIp);

        if (subnetFrom == null || subnetTo == null) {
            System.out.println("Error, One or both IP addresses are not in any subnet.");
            return;
        }

        if (subnetFrom.equals(subnetTo)) {
            // Intra-subnet packet routing
            List<String> path = subnetFrom.findShortestPath(fromIp, toIp);
            if (path != null) {
                printPath(path);
            } else {
                System.out.println("Error, No path found within the subnet.");
            }
        } else {
            // Inter-subnet packet routing
            String senderRouterIp = subnetFrom.getRouterIp();
            List<String> pathToRouter = subnetFrom.findShortestPath(fromIp, senderRouterIp);

            if (senderRouterIp == null || pathToRouter == null) {
                System.out.println("Error, No path found to the router in the sender's subnet.");
                return;
            }

            String receiverRouterIp = subnetTo.getRouterIp();
            List<String> routerToRouterPath = findShortestInterSubnetPath(senderRouterIp, receiverRouterIp);
            List<String> pathToReceiver = subnetTo.findShortestPath(receiverRouterIp, toIp);

            if (routerToRouterPath == null || routerToRouterPath.isEmpty()) {
                System.out.println("Error, No router-to-router path found between the subnets.");
                return;
            }

            if (pathToReceiver == null || pathToReceiver.isEmpty()) {
                System.out.println("Error, No path found from the router to the receiver in the destination subnet.");
                return;
            }

            pathToRouter.addAll(routerToRouterPath);
            pathToRouter.addAll(pathToReceiver);
            printPath(pathToRouter);
        }
    }



    /**
     * Finds the shortest path between two routers in different subnets based on inter-subnet connections.
     *
     * @param fromRouterIp The IP address of the starting router.
     * @param toRouterIp   The IP address of the destination router.
     * @return The list of IP addresses representing the shortest path between the two routers, or null if no path is found.
     */
    private List<String> findShortestInterSubnetPath(String fromRouterIp, String toRouterIp) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(String ip1, String ip2) {
                return Integer.compare(distances.get(ip1), distances.get(ip2));
            }
        });
        Set<String> visited = new HashSet<>();

        // Initialize distances
        for (String routerIp : interSubnetConnections.keySet()) {
            distances.put(routerIp, Integer.MAX_VALUE);
        }
        distances.put(fromRouterIp, 0);
        pq.add(fromRouterIp);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            visited.add(current);

            if (current.equals(toRouterIp)) {
                break;
            }

            Map<String, Integer> neighbors = interSubnetConnections.get(current);
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

        List<String> path = new LinkedList<>();
        for (String at = toRouterIp; at != null; at = previousNodes.get(at)) {
            path.add(0, at);
        }

        return path.size() == 1 ? null : path;
    }



    /**
     * Prints the path taken by the packet.
     * @param path List of IP addresses representing the path.
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
     * Finds the subnet for a given IP address.
     * @param ip The IP address to locate.
     * @return The Subnet containing the IP address, or null if not found.
     */
    private Subnet findSubnetForIp(String ip) {
        // Use explicit iteration to avoid functional constructs
        for (Map.Entry<String, Subnet> entry : subnets.entrySet()) {
            Subnet subnet = entry.getValue();
            if (subnet.containsIp(ip)) {
                return subnet;
            }
        }
        return null;
    }


}
