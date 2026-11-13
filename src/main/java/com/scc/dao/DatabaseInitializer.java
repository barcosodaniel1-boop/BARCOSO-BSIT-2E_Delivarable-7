package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    /** Creates every required table (if missing) and seeds a default admin. */
    public static void initialize() {
        String users = """
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name     TEXT NOT NULL,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role          TEXT NOT NULL CHECK (role IN ('CUSTOMER','BAKER','ADMIN')),
                    status        TEXT NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','APPROVED','REJECTED')),
                    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""";

        String cakes = """
                CREATE TABLE IF NOT EXISTS cakes (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT NOT NULL,
                    description TEXT,
                    price       REAL NOT NULL CHECK (price >= 0)
                )""";

        String orders = """
                CREATE TABLE IF NOT EXISTS orders (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    status      TEXT NOT NULL DEFAULT 'PLACED',
                    created_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES users(id)
                )""";

        String orderItems = """
                CREATE TABLE IF NOT EXISTS order_items (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER NOT NULL,
                    cake_id  INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                    FOREIGN KEY (cake_id)  REFERENCES cakes(id)
                )""";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(users);
            st.execute(cakes);
            st.execute(orders);
            st.execute(orderItems);
        } catch (SQLException e) {
            throw new IllegalStateException("Database initialization failed: " + e.getMessage(), e);
        }

        new UserDao().ensureDefaultAdmin();
        System.out.println("[DB] Database initialized (foreign keys ON).");
    }
}