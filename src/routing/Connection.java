package routing;

public class Connection {
    private final NetworkSystem networkSystem1;
    private final NetworkSystem networkSystem2;
    private final int weight;

    public Connection(NetworkSystem networkSystem1, NetworkSystem networkSystem2, int weight) {
        this.networkSystem1 = networkSystem1;
        this.networkSystem2 = networkSystem2;
        this.weight = weight;
    }

    public NetworkSystem getSystem1() {
        return networkSystem1;
    }

    public NetworkSystem getSystem2() {
        return networkSystem2;
    }

    public int getWeight() {
        return weight;
    }
}