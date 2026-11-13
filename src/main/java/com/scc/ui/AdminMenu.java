package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public class AdminMenu {

    private static final Set<String> VALID_ROLES = Set.of("CUSTOMER", "BAKER", "ADMIN");
    private static final Set<String> VALID_STATUSES = Set.of("APPROVED", "PENDING");

    private final UserDao userDao;
    private final Scanner scanner;
    private final User admin;

    public AdminMenu(UserDao userDao, Scanner scanner, User admin) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.admin = admin;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ADMIN MENU (" + admin.username() + ") ===");
            System.out.println("1. Review pending accounts");
            System.out.println("2. Update user role and status");
            System.out.println("0. Logout");
            System.out.print("Choose: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> reviewPending();
                case "2" -> updateUser();
                case "0" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void reviewPending() {
        List<User> pending = userDao.findByStatus("PENDING");
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        for (User u : pending) {
            System.out.printf("[%d] %s (%s) - %s%n", u.id(), u.fullName(), u.username(), u.role());
        }
        System.out.print("Enter user ID to review (blank to cancel): ");
        String idText = scanner.nextLine().trim();
        if (idText.isEmpty()) {
            return;
        }
        try {
            int id = Integer.parseInt(idText);
            System.out.print("(A)pprove or (R)eject? ");
            String action = scanner.nextLine().trim().toUpperCase();
            if (action.equals("A")) {
                System.out.println(userDao.updateStatus(id, "APPROVED") ? "Approved." : "Update failed.");
            } else if (action.equals("R")) {
                System.out.println(userDao.updateStatus(id, "REJECTED") ? "Rejected." : "Update failed.");
            } else {
                System.out.println("Cancelled.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    // ---------- Update user role & status ----------

    private void updateUser() {
        System.out.println("\n--- Update User Role and Status ---");
        printUserTable(userDao.findAll());

        int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        // Security rule: an admin cannot modify their own active account.
        if (targetUserId == admin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        Optional<User> target = userDao.findById(targetUserId);
        if (target.isEmpty()) {
            System.out.println("No user found with ID " + targetUserId + ".");
            return;
        }
        System.out.printf("Selected: %s (%s) - current role: %s, status: %s%n",
                target.get().fullName(), target.get().username(),
                target.get().role(), target.get().status());

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
            System.out.printf("SUCCESS: User ID %d updated -> role: %s, status: %s%n",
                    targetUserId, newRole, newStatus);
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    private void printUserTable(List<User> users) {
        String line = "+------+----------------------+------------------+----------+----------+";
        System.out.println(line);
        System.out.printf("| %-4s | %-20s | %-16s | %-8s | %-8s |%n",
                "ID", "Full Name", "Username", "Role", "Status");
        System.out.println(line);
        for (User u : users) {
            System.out.printf("| %-4d | %-20s | %-16s | %-8s | %-8s |%n",
                    u.id(), truncate(u.fullName(), 20), truncate(u.username(), 16),
                    u.role(), u.status());
        }
        System.out.println(line);
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "~";
    }

    // ---------- Input validation wrappers ----------

    /** Keeps asking until the user types a whole number. Never throws on bad input. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number (digits only).");
            }
        }
    }

    /** Keeps asking until a valid role is typed. Returns null if the user enters 0 (cancel). */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + ROLES_TEXT + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Valid roles are: " + ROLES_TEXT);
        }
    }

    /** Keeps asking until a valid status is typed. Returns null if the user enters 0 (cancel). */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + STATUS_TEXT + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Valid statuses are: " + STATUS_TEXT);
        }
    }

    private static final String ROLES_TEXT = "(CUSTOMER / BAKER / ADMIN)";
    private static final String STATUS_TEXT = "(APPROVED / PENDING)";
}