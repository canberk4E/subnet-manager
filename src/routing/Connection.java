package routing;

/**
 * Represents a connection between two network systems within a subnet.
 * Each connection has two systems and a weight (typically representing latency or cost).
 * @author uylsn
 */
public class Connection {
    private final NetworkSystem networkSystem1;
    private final NetworkSystem networkSystem2;
    private final int weight;
    /**
     * Constructor to create a connection between two network systems.
     *
     * @param networkSystem1 The first network system in the connection.
     * @param networkSystem2 The second network system in the connection.
     * @param weight The weight or cost of the connection.
     */
    public Connection(NetworkSystem networkSystem1, NetworkSystem networkSystem2, int weight) {
        this.networkSystem1 = networkSystem1;
        this.networkSystem2 = networkSystem2;
        this.weight = weight;
    }

    /**
     * Retrieves the first network system in the connection.
     *
     * @return The first network system.
     */
    public NetworkSystem getSystem1() {
        return networkSystem1;
    }

    /**
     * Retrieves the second network system in the connection.
     *
     * @return The second network system.
     */
    public NetworkSystem getSystem2() {
        return networkSystem2;
    }

    /**
     * Retrieves the weight or cost of the connection.
     *
     * @return The weight of the connection.
     */
    public int getWeight() {
        return weight;
    }
}
