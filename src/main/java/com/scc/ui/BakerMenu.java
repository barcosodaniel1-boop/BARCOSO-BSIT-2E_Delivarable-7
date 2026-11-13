package com.yourdomain.ui;

import com.yourdomain.model.User;

import java.util.Scanner;

public class BakerMenu {

    private final Scanner scanner;
    private final User user;

    public BakerMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== BAKER MENU (" + user.username() + ") ===");
            System.out.println("(Features coming in the next deliverable)");
            System.out.println("0. Logout");
            System.out.print("Choose: ");
            if (scanner.nextLine().trim().equals("0")) {
                running = false;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }
}