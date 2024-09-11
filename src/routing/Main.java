package routing;

import java.util.Scanner;
import java.lang.System;

public class Main {

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
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }
}
