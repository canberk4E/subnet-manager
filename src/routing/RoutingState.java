package routing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Holds routing state information for the Dijkstra algorithm, including hop counts, previous nodes,
 * and hop preferences used to determine the shortest inter-subnet path between routers.
 * @author uylsn
 */
class RoutingState {
    Map<String, String> previousNodes = new HashMap<>();
    Map<String, Integer> hopCounts = new HashMap<>();
    Map<String, String> firstHops = new HashMap<>();
    Map<String, String> secondHops = new HashMap<>();
    PriorityQueue<String> pq;

    RoutingState() {
        pq = new PriorityQueue<>(createRouterComparator());
    }

    /**
     * Creates a comparator to compare routers based on hop count, first hop, second hop,
     * and lexicographical order of router IP addresses.
     *
     * @return A comparator that orders routers by hop count and other tie-breaking rules.
     */
    private Comparator<String> createRouterComparator() {
        return (ip1, ip2) -> compareRouters(ip1, ip2);
    }

    private int compareRouters(String ip1, String ip2) {
        int hopComparison = compareHopCounts(ip1, ip2);
        if (hopComparison != 0) {
            return hopComparison;
        }

        int firstHopComparison = compareFirstHops(ip1, ip2);
        if (firstHopComparison != 0) {
            return firstHopComparison;
        }

        int secondHopComparison = compareSecondHops(ip1, ip2);
        if (secondHopComparison != 0) {
            return secondHopComparison;
        }

        return compareIpAddresses(ip1, ip2);
    }

    private int compareHopCounts(String ip1, String ip2) {
        return Integer.compare(hopCounts.get(ip1), hopCounts.get(ip2));
    }

    private int compareFirstHops(String ip1, String ip2) {
        String firstHop1 = firstHops.get(ip1);
        String firstHop2 = firstHops.get(ip2);
        return firstHop1.compareTo(firstHop2);
    }

    private int compareSecondHops(String ip1, String ip2) {
        String secondHop1 = secondHops.getOrDefault(ip1, "");
        String secondHop2 = secondHops.getOrDefault(ip2, "");
        return secondHop1.compareTo(secondHop2);
    }

    private int compareIpAddresses(String ip1, String ip2) {
        return ip1.compareTo(ip2);
    }
}
