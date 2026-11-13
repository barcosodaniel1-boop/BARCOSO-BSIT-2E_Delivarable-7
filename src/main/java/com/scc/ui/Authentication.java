package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Optional;
import java.util.Scanner;

/** Handles login, registration and role routing. No SQL/JDBC in this class. */
public class Authentication {

    private static final String[] ROLES = {"CUSTOMER", "BAKER", "ADMIN"};

    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SweetOrder: Bakeshop Cake Ordering ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register ---");
        System.out.print("Full name: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.println("Select role:");
        for (int i = 0; i < ROLES.length; i++) {
            System.out.println((i + 1) + ". " + ROLES[i]);
        }
        System.out.print("Role: ");
        String choice = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(choice) - 1;
        } catch (NumberFormatException e) {
            idx = -1;
        }
        if (idx < 0 || idx >= ROLES.length) {
            System.out.println("Invalid role.");
            return;
        }

        if (userDao.register(fullName, username, password, ROLES[idx])) {
            System.out.println("Registration successful! Status: PENDING (wait for admin approval).");
        } else {
            System.out.println("Registration failed.");
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.authenticate(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }
        User user = result.get();
        switch (user.status()) {
            case "PENDING" -> System.out.println("Your account is still PENDING approval.");
            case "REJECTED" -> System.out.println("Your account was REJECTED. Contact the bakeshop.");
            default -> routeByRole(user);
        }
    }

    private void routeByRole(User user) {
        System.out.println("\nWelcome, " + user.fullName() + " [" + user.role() + "]");
        switch (user.role()) {
            case "ADMIN" -> new AdminMenu(userDao, scanner, user).show();
            case "BAKER" -> new BakerMenu(scanner, user).show();
            case "CUSTOMER" -> new CustomerMenu(scanner, user).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }
}