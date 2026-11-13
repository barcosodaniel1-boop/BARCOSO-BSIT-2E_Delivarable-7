package com.yourdomain.dao;

import com.yourdomain.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String COLS = "id, full_name, username, role, status";

    /** Registers a new account with status PENDING. Returns false on failure/duplicate. */
    public boolean register(String fullName, String username, String password, String role) {
        return insert(fullName, username, password, role, "PENDING");
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
            return false;
        }
    }

    /** Returns the user if username/password match, otherwise empty. */
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT " + COLS + ", password_hash FROM users WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && verify(password, rs.getString("password_hash"))) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> findByStatus(String status) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM users WHERE status = ? ORDER BY id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
            return false;
        }
    }

    /** Returns every user in the system, ordered by ID. */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM users ORDER BY id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
        }
        return list;
    }

    /** Returns the user with the given ID, or empty if none exists. */
    public Optional<User> findById(int userId) {
        String sql = "SELECT " + COLS + " FROM users WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Updates a user's role and status in one statement.
     * Inputs are converted to UPPERCASE before being sent to the database.
     */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        if (newRole == null || newStatus == null) {
            return false;
        }
        String sql = "UPDATE users SET role = ?, status = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newRole.trim().toUpperCase());
            ps.setString(2, newStatus.trim().toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
            return false;
        }
    }

    /** Seeds admin / admin123 (APPROVED) so someone can approve new accounts. */
    public void ensureDefaultAdmin() {
        if (!usernameExists("admin")) {
            insert("System Administrator", "admin", "admin123", "ADMIN", "APPROVED");
        }
    }

    // ---------- helpers ----------

    private boolean insert(String fullName, String username, String password, String role, String status) {
        String sql = "INSERT INTO users (full_name, username, password_hash, role, status) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, hash(password));
            ps.setString(4, role);
            ps.setString(5, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[DB error] " + e.getMessage());
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("full_name"), rs.getString("username"),
                rs.getString("role"), rs.getString("status"));
    }

    private String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + digest(salt, password);
    }

    private boolean verify(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return digest(salt, password).equals(parts[1]);
    }

    private String digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return Base64.getEncoder().encodeToString(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}