package routing;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * network manager.
 * @author uylsn
 */
public class NetworkManager {
    final Map<String, Subnet> subnets = new HashMap<>();
    final ConnectionManager connectionManager;
    final Map<String, String> deviceNameToIpMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> interSubnetConnections = new HashMap<>();
    private final CommandHandler commandHandler;

    /**
     * Constructor for NetworkManager.
     * Initializes the connection manager and network data structures.
     */
    public NetworkManager() {
        this.connectionManager = new ConnectionManager(subnets, interSubnetConnections, deviceNameToIpMap); // Pass deviceNameToIpMap
        this.commandHandler = new CommandHandler(this);
    }
    /**
     * Processes user commands and executes the appropriate actions.
     * @param command the user input command as a String.
     * @throws IOException          if there's an issue reading files.
     * @throws InterruptedException if there's an issue with process interruptions.
     */
    public void processCommand(String command) throws IOException, InterruptedException {
        commandHandler.processCommand(command);
    }
    /**
     * Handles the 'load' command to load a network from a file.
     */
    public void listSubnets() {
        if (subnets.isEmpty()) {
            System.out.println("Error, No subnets found.");
        } else {
            List<Map.Entry<String, Subnet>> subnetList = new ArrayList<>(subnets.entrySet());
            subnetList.sort(Comparator.comparing(entry -> NetworkUtility.convertIpToLong(entry.getValue().getBaseAddress())));
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Subnet> subnet : subnetList) {
                result.append(subnet.getValue().toString()).append(" ");
            }
            System.out.println(result.toString().trim());
        }
    }
    /**
     * Lists the range of a specific, subnet.
     * @param subnet the subnet in CIDR notation.
     * @throws IllegalArgumentException
     */
    void listRange(String subnet) throws IllegalArgumentException {
        String[] parts = subnet.split("/");
        String firstIp = parts[0];
        Subnet sn = subnets.get(firstIp);
        if (sn == null || !subnet.contains("/")) {
            System.out.println("Error, Subnet not found.");
            return;
        }
        System.out.println(firstIp + " " + sn.getLastIp());
    }
    /**
     * Lists all systems in a specific subnet.
     * @param subnet the subnet in CIDR notation.
     */
    void listSystems(String subnet) {
        String[] parts = subnet.split("/");
        String base = parts[0];
        Subnet sn = subnets.get(base);
        if (sn == null) {
            System.out.println("Error, Subnet not found.");
            return;
        }
        if (CIDR.isValidIp(subnet) || NetworkUtility.isIpNetMaskValid(parts[1])) {
            System.out.println("Error, Subnet not found.");
            return;
        }
        StringBuilder result = new StringBuilder();
        for (NetworkSystem networkSystem : sn.getNetworkSystems()) {
            result.append(networkSystem.ip()).append(" ");
        }
        System.out.println(result.toString().trim());
    }
    /**
     * Adds a computer to a specific subnet.
     * @param subnetAddress the subnet address to which the computer will be added.
     * @param ipAddress     the IP address of the computer to be added.
     */
    void addComputer(String subnetAddress, String ipAddress) {
        Subnet sn = subnets.get(subnetAddress);
        if (deviceNameToIpMap.containsValue(ipAddress)) {
            System.out.println("Error, Computer with IP " + ipAddress + " already exists in the system.");
            return;
        }
        if (sn != null) {
            NetworkSystem computer = new NetworkSystem(ipAddress, false);
            sn.addNetworkSystem(computer);
            deviceNameToIpMap.put("Computer_" + ipAddress, ipAddress); // Add with a unique key
        } else {
            System.out.println("Error, Subnet not found.");
        }
    }

    /**
     * Removes a computer from the subnet.
     * @param subnetAddress the subnet address from which the computer will be removed
     * @param ip the IP address of the computer to remove
     * @return true if the computer was removed, false if the system is a router or not found
     */
    boolean removeComputer(String subnetAddress, String ip) {
        Subnet targetSubnet = subnets.get(subnetAddress);
        if (targetSubnet == null) {
            return false;
        }
        NetworkSystem systemToRemove = targetSubnet.findNetworkSystem(ip);
        if (systemToRemove == null) {
            return false;  // System not found, return false for error handling in handleRemoveCommand
        }
        if (systemToRemove.isRouter()) {
            System.out.println("Error, Cannot remove router " + ip + ".");
            return false;
        }
        targetSubnet.removeComputer(ip);  // Remove the computer
        return true;
    }
    /**
     * Loads the network from a file.
     * @param path the file path containing network data.
     * @throws IOException if there's an issue reading the file.
     */
    public void loadNetwork(String path) throws IOException {
        subnets.clear();
        deviceNameToIpMap.clear();
        interSubnetConnections.clear();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        Subnet currentSubnet = null;
        List<String> interSubnetConnections = new ArrayList<>();
        Map<Subnet, List<String>> intraSubnetConnections = new HashMap<>();
        System.out.println("graph");
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("subgraph")) {
                String[] parts = line.split("\\s+");
                String cidr = parts[1];
                String[] cidrParts = cidr.split("/");
                String baseAddress = cidrParts[0];
                int netmask = Integer.parseInt(cidrParts[1]);
                currentSubnet = new Subnet(baseAddress, netmask);
                subnets.put(baseAddress, currentSubnet);
                intraSubnetConnections.put(currentSubnet, new ArrayList<>());
                System.out.println("    subgraph " + baseAddress + "/" + netmask);
            } else if (line.equals("end")) {
                if (currentSubnet != null) {
                    for (String connection : intraSubnetConnections.get(currentSubnet)) {
                        System.out.println(connection);
                    }
                }
                System.out.println("    end");
                currentSubnet = null;
            } else if (line.contains("<-->")) {
                if (currentSubnet != null) {
                    intraSubnetConnections.get(currentSubnet).add(processConnection(line, true));
                } else {
                    interSubnetConnections.add(line);
                }
            } else if (line.contains("[")) {
                String[] parts = line.split("\\[|\\]");
                String systemName = parts[0].trim();
                String ipAddress = parts[1].trim();
                if (currentSubnet != null) {
                    boolean isRouter = systemName.toLowerCase().contains("router");
                    NetworkSystem networkSystem = new NetworkSystem(ipAddress, isRouter);
                    currentSubnet.addNetworkSystem(networkSystem);
                    deviceNameToIpMap.put(systemName, ipAddress);
                    System.out.println("        " + systemName + "[" + ipAddress + "]");
                }
            }
        }
        reader.close();
        if (!interSubnetConnections.isEmpty()) {
            for (String connection : interSubnetConnections) {
                System.out.println(processConnection(connection, false));
            }
        }
    }

    /**
     * Processes the connection line and returns the connection string.
     * @param line         the connection line.
     * @param isIntraSubnet flag to indicate whether the connection is intra-subnet.
     * @return the formatted connection string.
     */
    private String processConnection(String line, boolean isIntraSubnet) {
        StringBuilder connectionBuilder = new StringBuilder();
        if (line.contains("<-->")) {
            String[] parts = line.split("<-->");
            if (parts.length == 2) {
                String system1 = parts[0].trim();
                String rest = parts[1].trim();
                String ip1 = deviceNameToIpMap.get(system1);
                if (ip1 == null) {
                    return NetworkUtility.handleMissingDevice(system1);
                }
                if (rest.contains("|")) {
                    return handleIntraSubnetConnection(system1, ip1, rest, connectionBuilder);
                } else {
                    return handleInterSubnetConnection(system1, ip1, rest, isIntraSubnet, connectionBuilder);
                }
            }
            System.out.println("Error, Invalid connection format.");
        }
        return connectionBuilder.toString();
    }
    /**
     * Handles the intra-subnet connection processing.
     */
    private String handleIntraSubnetConnection(String system1, String ip1, String rest, StringBuilder connectionBuilder) {
        String[] weightParts = rest.split("\\|");
        if (weightParts.length != 3) {
            return NetworkUtility.handleInvalidConnectionFormat();
        }
        String system2 = weightParts[2].trim();
        String ip2 = deviceNameToIpMap.get(system2);

        if (ip2 == null) {
            return NetworkUtility.handleMissingDevice(system2);
        }
        if (!NetworkUtility.areIpsInSameSubnet(ip1, ip2, connectionManager)) {
            return "";
        }
        try {
            int weight = Integer.parseInt(weightParts[1].trim());
            connectionBuilder.append("        ").append(system1).append(" <-->|")
                    .append(weight).append("| ").append(system2);
            connectionManager.addConnection(ip1, ip2, weight);
        } catch (NumberFormatException e) {
            System.out.println("Error, Invalid weight format.");
            return "";
        }
        return connectionBuilder.toString();
    }
    /**
     * Handles the inter-subnet connection.
     */
    private String handleInterSubnetConnection(String system1, String ip1, String rest,
                                               boolean isIntraSubnet, StringBuilder connectionBuilder) {
        String system2 = rest.trim();
        String ip2 = deviceNameToIpMap.get(system2);
        if (ip2 == null) {
            return NetworkUtility.handleMissingDevice(system2);
        }
        if (!NetworkUtility.areIpsInDifferentSubnets(ip1, ip2, connectionManager)) {
            return "";
        }
        if (!isIntraSubnet) {
            connectionBuilder.append("    ").append(system1).append(" <--> ").append(system2);
            connectionManager.addConnection(ip1, ip2, 0);
        }
        return connectionBuilder.toString();
    }
}
