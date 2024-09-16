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
    private final Map<String, Subnet> subnets = new HashMap<>();
    private final Map<String, String> deviceNameToIpMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> interSubnetConnections = new HashMap<>();

    // Use an instance of ConnectionManager for managing connections
    private final ConnectionManager connectionManager;

    /**
     * Constructor for NetworkManager.
     * Initializes the connection manager and network data structures.
     */
    public NetworkManager() {
        this.connectionManager = new ConnectionManager(subnets, interSubnetConnections);
    }

    /**
     * Processes user commands and executes the appropriate actions.
     *
     * @param command the user input command as a String.
     * @throws IOException          if there's an issue reading files.
     * @throws InterruptedException if there's an issue with process interruptions.
     */
    public void processCommand(String command) throws IOException, InterruptedException {
        String[] parts = command.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }

        switch (parts[0]) {
            case "load":
                handleLoadCommand(parts);
                break;
            case "list":
                handleListCommand(parts);
                break;
            case "add":
                handleAddCommand(parts);
                break;
            case "send":
                handleSendCommand(parts);
                break;
            case "remove":
                handleRemoveCommand(parts);
                break;
            default:
                System.out.println("Error, Unknown command '" + parts[0] + "'. Please try again.");
                break;
        }
    }

    /**
     * Handles the 'load' command to load a network from a file.
     *
     * @param parts array of command arguments.
     * @throws IOException if there's an issue reading the file.
     */
    private void handleLoadCommand(String[] parts) throws IOException {
        if (parts.length < 3 || !parts[1].equals("network")) {
            System.out.println("Error, Invalid 'load' command format. Expected 'load network <file_path>'.");
        } else {
            loadNetwork(parts[2]);
        }
    }

    /**
     * Handles the 'list' command to list network information.
     *
     * @param parts array of command arguments.
     */
    private void handleListCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error, Missing argument for 'list' command.");
            return;
        }
        switch (parts[1]) {
            case "subnets" -> listSubnets();
            case "range" -> {
                if (parts.length < 3) {
                    System.out.println("Error, Missing subnet argument for 'list range'. Expected 'list range <subnet>'.");
                } else {
                    listRange(parts[2]);
                }
            }
            case "systems" -> {
                if (parts.length < 3) {
                    System.out.println("Error, Missing subnet argument for 'list systems'. Expected 'list systems <subnet>'.");
                } else {
                    listSystems(parts[2]);
                }
            }
            default -> System.out.println("Error, Invalid 'list' command. Expected 'subnets', 'range', or 'systems'.");
        }
    }

    /**
     * Handles the 'add' command to add a computer or connection to the network.
     *
     * @param parts array of command arguments.
     */
    private void handleAddCommand(String[] parts) {
        if (parts.length < 4 || (!parts[1].equals("computer") && !parts[1].equals("connection"))) {
            System.out.println("Error, Invalid 'add' command format.");
            return;
        }

        if (parts[1].equals("computer")) {
            String subnetAddress = parts[2].split("/")[0];
            addComputer(subnetAddress, parts[3]);
        } else {
            if (parts.length == 5) {
                connectionManager.addConnection(parts[2], parts[3], Integer.parseInt(parts[4]));
            } else if (parts.length == 4) {
                connectionManager.addConnection(parts[2], parts[3], 0);
            } else {
                System.out.println("Error, Invalid 'add connection' command format. Expected 'add connection <ip1> <ip2> <weight>'.");
            }
        }
    }

    /**
     * Handles the 'send' command to send a packet between systems.
     *
     * @param parts array of command arguments.
     */
    private void handleSendCommand(String[] parts) {
        if (!parts[1].equals("packet") || parts.length != 4)  {
            System.out.println("Error, Invalid 'send packet' command format. Expected 'send packet <from_ip> <to_ip>'.");
        } else {
            connectionManager.sendPacket(parts[2], parts[3]);
        }
    }

    /**
     * Handles the 'remove' command to remove a computer or connection.
     *
     * @param parts array of command arguments.
     */
    private void handleRemoveCommand(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Error, Invalid remove command format.");
            return;
        }

        if (parts[1].equals("connection")) {
            connectionManager.removeConnection(parts[2], parts[3]);
        } else if (parts[1].equals("computer")) {
            String subnetAddress = parts[2].split("/")[0];
            removeComputer(subnetAddress, parts[3]);
        } else {
            System.out.println("Error, Invalid remove command. Expected 'connection' or 'computer'.");
        }
    }
    private void listSubnets() {
        if (subnets.isEmpty()) {
            System.out.println("Error, No subnets found.");
        } else {
            List<Map.Entry<String, Subnet>> subnetList = new ArrayList<>(subnets.entrySet());

            // Sort subnets numerically by their IP address
            subnetList.sort(Comparator.comparing(entry -> convertIpToLong(entry.getValue().getBaseAddress())));

            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Subnet> subnet : subnetList) {
                result.append(subnet.getValue().toString()).append(" ");
            }
            System.out.println(result.toString().trim());
        }
    }
    /**
     * Converts an IP address from its string format to a long for easier numerical sorting.
     * @param ipAddress The IP address in string format (e.g., "192.168.1.1").
     * @return A long representing the IP address.
     */
    private long convertIpToLong(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        long ipAsLong = 0;
        for (int i = 0; i < ipParts.length; i++) {
            ipAsLong = (ipAsLong << 8) + Integer.parseInt(ipParts[i]);
        }
        return ipAsLong;
    }
    /**
     * Lists the range of a specific subnet.
     *
     * @param subnet the subnet in CIDR notation.
     */
    private void listRange(String subnet) throws IllegalArgumentException {
        String[] parts = subnet.split("/");
        String firstIp = parts[0];
        Subnet sn = subnets.get(firstIp);

        if (sn == null) {
            System.out.println("Error, Subnet not found.");
            return;
        }

        System.out.println(firstIp + " " + sn.getLastIp());
    }

    /**
     * Lists all systems in a specific subnet.
     *
     * @param subnet the subnet in CIDR notation.
     */
    private void listSystems(String subnet) {
        String[] parts = subnet.split("/");
        String base = parts[0];
        Subnet sn = subnets.get(base);

        if (sn == null) {
            System.out.println("Error, Subnet not found.");
            return;
        }
        StringBuilder result = new StringBuilder();
        for (NetworkSystem networkSystem : sn.getNetworkSystems()) {
            result.append(networkSystem.ip()).append(" ");
        }

        // Trim the trailing space and print the result
        System.out.println(result.toString().trim());
    }


    /**
     * Adds a computer to a specific subnet.
     *
     * @param subnetAddress the subnet address to which the computer will be added.
     * @param ipAddress     the IP address of the computer to be added.
     */
    private void addComputer(String subnetAddress, String ipAddress) {
        Subnet sn = subnets.get(subnetAddress);
        if (sn != null) {
            NetworkSystem computer = new NetworkSystem(ipAddress, false);
            sn.addNetworkSystem(computer);
        } else {
            System.out.println("Error, Subnet not found.");
        }
    }

    /**
     * Removes a computer from a specific subnet.
     *
     * @param subnetAddress the subnet address from which the computer will be removed.
     * @param ipAddress     the IP address of the computer to be removed.
     */
    private void removeComputer(String subnetAddress, String ipAddress) {
        Subnet targetSubnet = subnets.get(subnetAddress.trim());
        if (targetSubnet != null) {
            boolean removed = targetSubnet.removeComputer(ipAddress);
            if (!removed) {
                System.out.println("Error, Computer " + ipAddress + " not found in subnet " + subnetAddress.trim());
            }
        } else {
            System.out.println("Error, Subnet " + subnetAddress + " not found.");
        }
    }

    /**
     * Loads the network from a file.
     *
     * @param path the file path containing network data.
     * @throws IOException if there's an issue reading the file.
     */
    public void loadNetwork(String path) throws IOException {
        // Clear the existing network data before loading a new network
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
     *
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
                    System.out.println("Error, Device " + system1 + " not found in map.");
                    return "";
                }

                if (rest.contains("|")) {
                    String[] weightParts = rest.split("\\|");
                    if (weightParts.length == 3) {
                        String system2 = weightParts[2].trim();
                        String ip2 = deviceNameToIpMap.get(system2);

                        if (ip2 == null) {
                            System.out.println("Error, Device " + system2 + " not found in map.");
                            return "";
                        }

                        int weight = Integer.parseInt(weightParts[1].trim());
                        connectionBuilder.append("        ").append(system1).append(" <-->|")
                                .append(weight).append("| ").append(system2);
                        connectionManager.addConnection(ip1, ip2, weight);
                    }
                } else {
                    String system2 = rest.trim();
                    String ip2 = deviceNameToIpMap.get(system2);

                    if (ip2 == null) {
                        System.out.println("Error, Device " + system2 + " not found in map.");
                        return "";
                    }

                    if (!isIntraSubnet) {
                        connectionBuilder.append("    ").append(system1).append(" <--> ").append(system2);
                        connectionManager.addConnection(ip1, ip2, 0);
                    }
                }
            }
        }

        return connectionBuilder.toString();
    }
}
