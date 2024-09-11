package routing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.lang.System;
import java.io.*;

public class NetworkManager {
    private final Map<String, Subnet> subnets = new HashMap<>();

    public void processCommand(String command) throws Exception {
        String[] parts = command.split("\\s+");

        switch (parts[0]) {
            case "load":
                if (parts[1].equals("network")) {
                    loadNetwork(parts[2]);
                } else {
                    throw new Exception("Invalid load command");
                }
                break;

            case "list":
                switch (parts[1]) {
                    case "subnets" -> listSubnets();
                    case "range" -> listRange(parts[2]);
                    case "systems" -> listSystems(parts[2]);
                    default -> throw new Exception("Invalid list command");
                }
                break;

            case "add":
                if (parts[1].equals("computer")) {
                    addComputer(parts[2], parts[3]);
                } else if (parts[1].equals("connection")) {
                    addConnection(parts[2], parts[3], Integer.parseInt(parts[4]));
                } else {
                    throw new Exception("Invalid add command");
                }
                break;

            case "send":
                if (parts[1].equals("packet")) {
                    sendPacket(parts[2], parts[3]);
                } else {
                    throw new Exception("Invalid send command");
                }
                break;

            default:
                throw new Exception("Unknown command");
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

    private void listRange(String subnet) throws UnknownHostException {
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

    private void addConnection(String ip1, String ip2, int weight) {
        // Placeholder for adding a connection between two systems.
        System.out.println("Connection added between " + ip1 + " and " + ip2 + " with weight " + weight);
    }

    private void sendPacket(String fromIp, String toIp) {
        // Placeholder for sending a packet from one system to another.
        System.out.println("Packet sent from " + fromIp + " to " + toIp);
    }

    // Method to load the network from a file
    public void loadNetwork(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        Subnet currentSubnet = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim(); // Remove leading and trailing whitespace

            // Check if we're starting a new subnet (subgraph)
            if (line.startsWith("subgraph")) {
                // Extract the subnet address and netmask from CIDR notation
                String[] parts = line.split("\\s+");
                String cidr = parts[1];
                String[] cidrParts = cidr.split("/"); // Split the CIDR notation into base address and netmask
                String baseAddress = cidrParts[0];
                int netmask = Integer.parseInt(cidrParts[1]);

                currentSubnet = new Subnet(baseAddress, netmask); // Create a new Subnet object
                subnets.put(baseAddress, currentSubnet); // Add to the list of subnets
                System.out.println("Created subnet: " + baseAddress + "/" + netmask);

                // Check for the end of a subnet definition
            } else if (line.equals("end")) {
                currentSubnet = null; // We're done with this subnet

                // Process connections inside the subnet (with weights) or between subnets (without weights)
            } else if (line.contains("<-->")) {
                if (currentSubnet != null) {
                    // Intra-subnet connection (with weight)
                    String[] parts = line.split("\\|");
                    String connectionPart = parts[0].trim();
                    String system1 = connectionPart.split("<-->")[0].trim();
                    String system2 = parts[2].trim();
                    int weight = Integer.parseInt(parts[1].trim()); // Extract the weight

                    // Add connection to the current subnet
                    currentSubnet.addConnection(system1, system2, weight);
                    System.out.println("Added connection: " + system1 + " <-->|" + weight + "| " + system2);
                } else {
                    // Inter-subnet connection (no weight)
                    String[] parts = line.split("<-->");
                    String router1 = parts[0].trim();
                    String router2 = parts[1].trim();

                    // Handle inter-subnet connection (Router-to-Router)
                    System.out.println("Added inter-subnet connection between " + router1 + " and " + router2);
                }

                // Process systems inside the subnet (computers or routers)
            } else if (line.contains("[")) {
                // Systems are defined like: A_Router[192.168.1.1]
                String[] parts = line.split("\\[|\\]");
                String systemName = parts[0].trim();
                String ipAddress = parts[1].trim();

                if (currentSubnet != null) {
                    boolean isRouter = systemName.toLowerCase().contains("router");
                    NetworkSystem networkSystem = new NetworkSystem(ipAddress, isRouter); // Create a NetworkSystem (Computer or Router)
                    currentSubnet.addNetworkSystem(networkSystem); // Add to the current subnet
                    System.out.println("Added networkSystem: " + systemName + " (" + ipAddress + ")");
                }
            }
        }

        reader.close();
        System.out.println("Network loaded successfully from: " + path);
    }
}