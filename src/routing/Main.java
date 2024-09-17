package routing;

import java.io.IOException;
import java.util.Scanner;

/**
 * main class.
 * @author uylsn
 */
public final class Main {
    private Main() {

    }

    /**
     * for app starting.
     * @param args yes.
     */
    public static void main(String[] args) {
        NetworkManager networkManager = new NetworkManager();
        Scanner scanner = new Scanner(System.in);
        String command;


        while (true) {
            command = scanner.nextLine();

            if (command.equalsIgnoreCase("quit")) {
                break;
            }

            try {
                networkManager.processCommand(command);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error, " + e.getMessage());
            }
        }

        scanner.close();
    }
}
