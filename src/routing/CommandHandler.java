package routing;

import java.io.IOException;

/**
 * Handles the processing of user commands for the NetworkManager.
 * Extracted from NetworkManager for cleaner separation of concerns.
 * @author uylsn
 */
public class CommandHandler {

    private final NetworkManager networkManager;
    /**
     * Constructor to initialize CommandHandler with the NetworkManager instance.
     *
     * @param networkManager the instance of NetworkManager
     */
    public CommandHandler(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Processes user commands and delegates actions to the appropriate handlers.
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
        if (parts.length != 3 || !parts[1].equals("network")) {
            System.out.println("Error, Invalid 'load' command format. Expected 'load network <file_path>'.");
        } else {
            networkManager.loadNetwork(parts[2]);
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
            case "subnets" -> {
                if (parts.length == 2) {
                    networkManager.listSubnets();
                } else {
                    System.out.println("Error, wrong format for list subnets.");
                }
            }
            case "range" -> {
                if (parts.length < 3) {
                    System.out.println("Error, Missing subnet argument for 'list range'. Expected 'list range <subnet>'.");
                } else {
                    networkManager.listRange(parts[2]);
                }
            }
            case "systems" -> {
                if (parts.length != 3) {
                    System.out.println("Error, Missing subnet argument for 'list systems'. Expected 'list systems <subnet>'.");
                } else {
                    networkManager.listSystems(parts[2]);
                }
            }
            default -> System.out.println("Error, Invalid 'list' command. Expected 'subnets', 'range', or 'systems'.");
        }
    }

    /**
     * Handles the 'add' command to add a computer or connection to the network.
     * @param parts array of command arguments.
     */
    private void handleAddCommand(String[] parts) {
        if (parts.length < 4 || (!parts[1].equals("computer") && !parts[1].equals("connection")) || parts[2].equals(parts[3])
                || networkManager.subnets.isEmpty()) {
            System.out.println("Error, Invalid 'add' command format.");
            return;
        }
        if (parts[1].equals("computer") && parts.length == 4) {
            String subnetAddress = parts[2].split("/")[0];
            String computerIp = parts[3];
            if (!CIDR.belongsToSubnet(computerIp, parts[2])) {
                System.out.println("Error, IP address does not belong to the specified subnet.");
                return;
            }
            networkManager.addComputer(subnetAddress, computerIp);
        } else {
            if (!CIDR.isValidIp(parts[2]) || !CIDR.isValidIp(parts[3])) {
                System.out.println("Error, Invalid IP address format.");
                return;
            }
            NetworkSystem system1 = networkManager.connectionManager.findSystemByIp(parts[2]);
            NetworkSystem system2 = networkManager.connectionManager.findSystemByIp(parts[3]);
            if (system1 != null && system2 != null && system1.isRouter() && system2.isRouter()) {
                networkManager.connectionManager.addConnection(parts[2], parts[3], 0);
            } else if (parts.length == 5) {
                try {
                    int weight = Integer.parseInt(parts[4]);
                    networkManager.connectionManager.addConnection(parts[2], parts[3], weight);
                } catch (NumberFormatException e) {
                    System.out.println("Error, Invalid weight format.");
                }
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
        if (!parts[1].equals("packet") || parts.length != 4) {
            System.out.println("Error, Invalid 'send packet' command format. Expected 'send packet <from_ip> <to_ip>'.");
        } else {
            networkManager.connectionManager.sendPacket(parts[2], parts[3]);
        }
    }

    /**
     * Handles the 'remove' command to remove a computer or connection.
     *
     * @param parts array of command arguments.
     */
    private void handleRemoveCommand(String[] parts) {
        if (parts.length != 4 || networkManager.subnets.isEmpty()) {
            System.out.println("Error, Invalid remove command format.");
            return;
        }
        if (parts[1].equals("connection")) {
            networkManager.connectionManager.removeConnection(parts[2], parts[3]);
        } else if (parts[1].equals("computer")) {
            String subnetAddress = parts[2].split("/")[0];
            boolean removed = networkManager.removeComputer(subnetAddress, parts[3]);
            if (!removed) {
                System.out.println("Error, System " + parts[3] + " not found in the subnet.");
            }
        } else {
            System.out.println("Error, Invalid remove command. Expected 'connection' or 'computer'.");
        }
    }
}
