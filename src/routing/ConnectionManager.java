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


public class ConnectionManager {

    private final Map<String, Subnet> subnets;
    private final Map<String, Map<String, Integer>> interSubnetConnections;

    public ConnectionManager(Map<String, Subnet> subnets, Map<String, Map<String, Integer>> interSubnetConnections) {
        this.subnets = subnets;
        this.interSubnetConnections = interSubnetConnections;
    }

    public void addConnection(String ip1, String ip2, int weight) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        if (subnet1 == null || subnet2 == null) {
            System.out.println("Error: One or both IPs not found in any subnet.");
            return;
        }

        if (subnet1.equals(subnet2)) {
            subnet1.addConnection(ip1, ip2, weight);
        } else {
            NetworkSystem router1 = subnet1.findNetworkSystem(ip1);
            NetworkSystem router2 = subnet2.findNetworkSystem(ip2);

            if (router1 != null && router1.isRouter() && router2 != null && router2.isRouter()) {
                System.out.println("Inter-subnet connection added between routers: " + ip1 + " <--> " + ip2);
                interSubnetConnections.computeIfAbsent(ip1, k -> new HashMap<>()).put(ip2, 0);
                interSubnetConnections.computeIfAbsent(ip2, k -> new HashMap<>()).put(ip1, 0);
            } else {
                System.out.println("Error: Inter-subnet connections are only allowed between routers.");
            }
        }
    }

    public void removeConnection(String ip1, String ip2) {
        Subnet subnet1 = findSubnetForIp(ip1);
        Subnet subnet2 = findSubnetForIp(ip2);

        if (subnet1 == null || subnet2 == null) {
            System.out.println("Error: One or both IPs not found in any subnet.");
            return;
        }

        if (subnet1.equals(subnet2)) {
            subnet1.removeConnection(ip1, ip2);
        } else {
            NetworkSystem router1 = subnet1.findNetworkSystem(ip1);
            NetworkSystem router2 = subnet2.findNetworkSystem(ip2);

            if (router1 != null && router1.isRouter() && router2 != null && router2.isRouter()) {
                System.out.println("Inter-subnet connection removed between routers: " + ip1 + " <--> " + ip2);
                interSubnetConnections.computeIfPresent(ip1, (k, v) -> {
                    v.remove(ip2);
                    return v.isEmpty() ? null : v;
                });
                interSubnetConnections.computeIfPresent(ip2, (k, v) -> {
                    v.remove(ip1);
                    return v.isEmpty() ? null : v;
                });
            } else {
                System.out.println("Error: Inter-subnet connections can only be removed between routers.");
            }
        }
    }

    public void sendPacket(String fromIp, String toIp) {
        Subnet subnetFrom = findSubnetForIp(fromIp);
        Subnet subnetTo = findSubnetForIp(toIp);

        if (subnetFrom == null || subnetTo == null) {
            System.out.println("Error: One or both IP addresses are not in any subnet.");
            return;
        }

        if (subnetFrom.equals(subnetTo)) {
            System.out.println("Intra-subnet packet delivery:");
            List<String> path = subnetFrom.findShortestPath(fromIp, toIp);
            if (path != null) {
                printPath(path);
            } else {
                System.out.println("No path found within the subnet.");
            }
        } else {
            System.out.println("Inter-subnet packet delivery:");
            String senderRouterIp = subnetFrom.getRouterIp();
            List<String> pathToRouter = subnetFrom.findShortestPath(fromIp, senderRouterIp);
            String receiverRouterIp = subnetTo.getRouterIp();
            List<String> routerToRouterPath = findShortestInterSubnetPath(senderRouterIp, receiverRouterIp);
            List<String> pathToReceiver = subnetTo.findShortestPath(receiverRouterIp, toIp);

            if (pathToRouter != null && routerToRouterPath != null && pathToReceiver != null) {
                pathToRouter.addAll(routerToRouterPath);
                pathToRouter.addAll(pathToReceiver);
                printPath(pathToRouter);
            } else {
                System.out.println("No complete path found for inter-subnet delivery.");
            }
        }
    }

    private List<String> findShortestInterSubnetPath(String fromRouterIp, String toRouterIp) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));
        Set<String> visited = new HashSet<>();

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

    private void printPath(List<String> path) {
        System.out.println("Packet path:");
        List<String> distinctPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            if (i == 0 || !path.get(i).equals(path.get(i - 1))) {
                distinctPath.add(path.get(i));
            }
        }

        for (String hop : distinctPath) {
            System.out.print(hop + " ");
        }
        System.out.println();
    }

    private Subnet findSubnetForIp(String ip) {
        for (Subnet subnet : subnets.values()) {
            if (subnet.containsIp(ip)) {
                return subnet;
            }
        }
        return null;
    }
}
