package routing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {
    private final Map<String, Subnet> subnets = new HashMap<>();
    private final Map<String, String> deviceNameToIpMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> interSubnetConnections = new HashMap<>();

    // Use an instance of ConnectionManager for managing connections
    private final ConnectionManager connectionManager;

    public NetworkManager() {
        this.connectionManager = new ConnectionManager(subnets, interSubnetConnections);
    }

    public void processCommand(String command) throws IOException, InterruptedException {
        String[] parts = command.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            //it prints itself out?  System.out.println("Error, Empty command. Please try again.");
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

    private void handleLoadCommand(String[] parts) throws IOException {
        if (parts.length < 3 || !parts[1].equals("network")) {
            System.out.println("Error, Invalid 'load' command format. Expected 'load network <file_path>'.");
        } else {
            loadNetwork(parts[2]);
        }
    }

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

    private void handleSendCommand(String[] parts) {
        if (parts.length < 4 || !parts[1].equals("packet")) {
            System.out.println("Error, Invalid 'send packet' command format. Expected 'send packet <from_ip> <to_ip>'.");
        } else {
            connectionManager.sendPacket(parts[2], parts[3]);
        }
    }

    private void handleRemoveCommand(String[] parts) {
        if (parts.length < 4) {
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
        List<Map.Entry<String, Subnet>> subnetList = new ArrayList<>(subnets.entrySet());
        subnetList.sort(Comparator.comparing(entry -> entry.getValue().getBaseAddress()));

        for (Map.Entry<String, Subnet> subnet : subnetList) {
            System.out.print(subnet.getValue() + " ");
        }
        System.out.println();
    }

    private void listRange(String subnet) throws IllegalArgumentException {
        String[] parts = subnet.split("/");
        String firstIp = parts[0];
        Subnet sn = subnets.get(firstIp);

        if (sn == null) {
            System.out.println("Subnet not found.");
            return;
        }

        System.out.println(firstIp + " " + sn.getLastIp());
    }

    private void listSystems(String subnet) {
        String[] parts = subnet.split("/");
        String base = parts[0];
        Subnet sn = subnets.get(base);

        if (sn == null) {
            System.out.println("Subnet not found.");
            return;
        }

        for (NetworkSystem networkSystem : sn.getNetworkSystems()) {
            System.out.print(networkSystem.ip() + " ");
        }
        System.out.println();
    }

    private void addComputer(String subnetAddress, String ipAddress) {
        Subnet sn = subnets.get(subnetAddress);
        if (sn != null) {
            NetworkSystem computer = new NetworkSystem(ipAddress, false);
            sn.addNetworkSystem(computer);
            System.out.println("Computer added to subnet " + subnetAddress);
        } else {
            System.out.println("Subnet not found.");
        }
    }

    private void removeComputer(String subnetAddress, String ipAddress) {
        System.out.println("Trying to remove computer from subnet: " + subnetAddress);
        Subnet targetSubnet = subnets.get(subnetAddress.trim());
        if (targetSubnet != null) {
            boolean removed = targetSubnet.removeComputer(ipAddress);
            if (removed) {
                System.out.println("Computer " + ipAddress + " removed from subnet " + subnetAddress.trim());
            } else {
                System.out.println("Computer " + ipAddress + " not found in subnet " + subnetAddress.trim());
            }
        } else {
            System.out.println("Subnet " + subnetAddress + " not found.");
            System.out.println("Available subnets: " + subnets.keySet());  // Print available subnets for debugging
        }
    }

    // Method to load the network from a file (remains unchanged)
    public void loadNetwork(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        Subnet currentSubnet = null;
        List<String> connectionBuffer = new ArrayList<>();  // Buffer to store connections to be processed later
        System.out.println("graph");
        while ((line = reader.readLine()) != null) {
            line = line.trim(); // Remove leading and trailing whitespace
            // Check if we're starting a new subnet (subgraph)
            if (line.startsWith("subgraph")) {
                String[] parts = line.split("\\s+");
                String cidr = parts[1];
                String[] cidrParts = cidr.split("/"); // Split the CIDR notation into base address and netmask
                String baseAddress = cidrParts[0];
                int netmask = Integer.parseInt(cidrParts[1]);
                currentSubnet = new Subnet(baseAddress, netmask); // Create a new Subnet object
                subnets.put(baseAddress, currentSubnet); // Add to the list of subnets
                System.out.println("subgraph " + baseAddress + "/" + netmask); // Output the start of a subnet
            } else if (line.equals("end")) {
                System.out.println("end");  // End of a subnet
                currentSubnet = null; // We're done with this subnet

            } else if (line.contains("<-->")) {
                // This is a connection line, so we add it to the buffer
                connectionBuffer.add(line);

            } else if (line.contains("[")) {
                // Systems are defined like: A_Router[192.168.1.1]
                String[] parts = line.split("\\[|\\]");
                String systemName = parts[0].trim();
                String ipAddress = parts[1].trim();

                if (currentSubnet != null) {
                    boolean isRouter = systemName.toLowerCase().contains("router");
                    NetworkSystem networkSystem = new NetworkSystem(ipAddress, isRouter); // Create a NetworkSystem (Computer or Router)
                    currentSubnet.addNetworkSystem(networkSystem); // Add to the current subnet
                    // Map the system name to its IP address
                    deviceNameToIpMap.put(systemName, ipAddress);
                    System.out.println(systemName + "[" + ipAddress + "]");  // Print the system in the correct format
                }
            }
        }

        // Process buffered connections after all systems have been loaded
        for (String connectionLine : connectionBuffer) {
            processConnection(connectionLine);  // This will now work as the buffer is filled
        }

        reader.close();
        System.out.println("Existing connections after loading the network:");
        for (Subnet subnet : subnets.values()) {
            subnet.printAllConnections(); // Call the method to print connections for each subnet
        }

        // Now print the inter-subnet connections
        printInterSubnetConnections();

        System.out.println("Network loaded successfully from: " + path);
    }

    private void processConnection(String line) {
        // Check if the line contains a connection "<-->"
        if (line.contains("<-->")) {
            String[] parts = line.split("<-->");  // Split the line into systems at "<-->"
            if (parts.length == 2) {
                String system1 = parts[0].trim();
                String rest = parts[1].trim();  // Contains either system2 and weight, or just system2

                // Check if the system names can be resolved to IPs
                String ip1 = deviceNameToIpMap.get(system1);
                if (ip1 == null) {
                    System.out.println("Error, Device " + system1 + " not found in map.");
                    return;
                }
                // Check if the rest contains a weight (intra-subnet connection)
                if (rest.contains("|")) {
                    String[] weightParts = rest.split("\\|");  // Split the system2 and weight by "|"
                    // Ensure the weight and system2 are correctly split
                    if (weightParts.length == 3) {
                        String system2 = weightParts[2].trim();
                        String ip2 = deviceNameToIpMap.get(system2);  // Resolve system2 to its IP

                        if (ip2 == null) {
                            System.out.println("Error, Device " + system2 + " not found in map.");
                            return;
                        }
                        try {
                            int weight = Integer.parseInt(weightParts[1].trim());  // Parse the weight
                            System.out.println(system1 + " <-->|" + weight + "| " + system2);  // Print the connection with weight
                            System.out.println("Resolved IPs - system1: " + ip1 + ", system2: " + ip2);
                            connectionManager.addConnection(ip1, ip2, weight);  // Add the connection with weight
                        } catch (NumberFormatException e) {
                            System.out.println("Error, Invalid weight format in connection: " + line);
                        }
                    } else {
                        System.out.println("Error, Invalid connection format (missing system or weight): " + line);
                    }
                } else {
                    String system2 = rest.trim();
                    String ip2 = deviceNameToIpMap.get(system2);  // Resolve system2 to its IP

                    if (ip2 == null) {
                        System.out.println("Error, Device " + system2 + " not found in map.");
                        return;
                    }

                    System.out.println(system1 + " <--> " + system2);  // Print the connection without weight
                    System.out.println("Resolved IPs - system1: " + ip1 + ", system2: " + ip2);
                    connectionManager.addConnection(ip1, ip2, 0);  // Add the connection without weight
                }
            } else {
                System.out.println("Error, Invalid connection format (missing system parts): " + line);
            }
        } else {
            System.out.println("Error, Invalid connection format (missing '<-->'): " + line);
        }
        System.out.println();
    }

    private void printInterSubnetConnections() {
        System.out.println("Debug: All inter-subnet connections:");
        for (Map.Entry<String, Map<String, Integer>> entry : interSubnetConnections.entrySet()) {
            String ip1 = entry.getKey();
            for (Map.Entry<String, Integer> connection : entry.getValue().entrySet()) {
                String ip2 = connection.getKey();
                System.out.println("  " + ip1 + " <--> " + ip2);
            }
        }
        System.out.println();  // Add a blank line for readability
    }
}
