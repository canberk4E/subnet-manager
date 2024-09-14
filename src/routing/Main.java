package routing;

import java.io.IOException;
import java.util.Scanner;


public final class Main {
    private Main() {

    }

    public static void main(String[] args) {
        NetworkManager networkManager = new NetworkManager();
        Scanner scanner = new Scanner(System.in);
        String command;

        System.out.println("Welcome to the Routing Network System. Type 'quit' to exit.");

        while (true) {
            System.out.print("> ");
            command = scanner.nextLine();

            if (command.equalsIgnoreCase("quit")) {
                System.out.println("Exiting...");
                break;
            }

            try {
                networkManager.processCommand(command);
            } catch (IOException | InterruptedException e) {
                System.out.println("Error: I/O issue - " + e.getMessage());
            } // Removed RuntimeException and IllegalArgumentException catch blocks
        }

        scanner.close();
    }
}