package com.campuscycle.db;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Database Manager for SQLite Integration.
 * Handles database connection lifecycle, schema migrations, and initial seeding.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "campuscycle.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static DatabaseManager instance;

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            // Enable SQLite Foreign Key constraints
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Initializing SQLite database tables...");

            // 1. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    role TEXT NOT NULL,
                    specific_id TEXT,
                    department TEXT,
                    loyalty_points INTEGER DEFAULT 0
                );
            """);

            // 2. Stations Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    location TEXT NOT NULL,
                    capacity INTEGER NOT NULL DEFAULT 10
                );
            """);

            // 3. Cycles Table with Foreign Key to Stations
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cycles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    type TEXT NOT NULL,
                    hourly_rate REAL NOT NULL,
                    status TEXT NOT NULL,
                    station_id INTEGER,
                    battery_percentage INTEGER DEFAULT -1,
                    total_rides INTEGER DEFAULT 0,
                    last_maintained TEXT,
                    FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE SET NULL
                );
            """);

            // 4. Rentals Table with Foreign Keys to Users and Cycles
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rentals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    cycle_id INTEGER NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT,
                    duration_hours INTEGER NOT NULL,
                    total_cost REAL NOT NULL,
                    status TEXT NOT NULL,
                    start_station_id INTEGER,
                    end_station_id INTEGER,
                    notes TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE,
                    FOREIGN KEY (start_station_id) REFERENCES stations(id),
                    FOREIGN KEY (end_station_id) REFERENCES stations(id)
                );
            """);

            // 5. Payments Table with Foreign Key to Rentals
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rental_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    payment_method TEXT NOT NULL,
                    payment_status TEXT NOT NULL,
                    transaction_date TEXT NOT NULL,
                    transaction_ref TEXT UNIQUE NOT NULL,
                    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE
                );
            """);

            LOGGER.info("Database tables initialized successfully.");
            seedInitialData(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        // Check if users already seeded
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        LOGGER.info("Seeding initial data for CampusCycle...");

        // Seed Users
        String userSql = "INSERT INTO users (username, password, full_name, email, phone, role, specific_id, department, loyalty_points) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
            // Admin
            pstmt.setString(1, "admin");
            pstmt.setString(2, "admin123");
            pstmt.setString(3, "Campus Transport Admin");
            pstmt.setString(4, "admin@campuscycle.edu");
            pstmt.setString(5, "+8801700000001");
            pstmt.setString(6, "ADMIN");
            pstmt.setString(7, "ADM-901");
            pstmt.setString(8, "Campus Facilities");
            pstmt.setInt(9, 0);
            pstmt.executeUpdate();

            // Student 1
            pstmt.setString(1, "student");
            pstmt.setString(2, "student123");
            pstmt.setString(3, "Mohosin Khan");
            pstmt.setString(4, "mohosin.cse@campuscycle.edu");
            pstmt.setString(5, "+8801700000002");
            pstmt.setString(6, "STUDENT");
            pstmt.setString(7, "2021-1-60-042");
            pstmt.setString(8, "Computer Science & Engineering");
            pstmt.setInt(9, 45);
            pstmt.executeUpdate();

            // Student 2
            pstmt.setString(1, "sara");
            pstmt.setString(2, "sara123");
            pstmt.setString(3, "Sara Rahman");
            pstmt.setString(4, "sara.eee@campuscycle.edu");
            pstmt.setString(5, "+8801700000003");
            pstmt.setString(6, "STUDENT");
            pstmt.setString(7, "2022-2-50-119");
            pstmt.setString(8, "Electrical Engineering");
            pstmt.setInt(9, 20);
            pstmt.executeUpdate();

            // Staff
            pstmt.setString(1, "dr_smith");
            pstmt.setString(2, "staff123");
            pstmt.setString(3, "Dr. Robert Smith");
            pstmt.setString(4, "r.smith@campuscycle.edu");
            pstmt.setString(5, "+8801700000004");
            pstmt.setString(6, "STAFF");
            pstmt.setString(7, "FAC-304");
            pstmt.setString(8, "Faculty of Natural Sciences");
            pstmt.setInt(9, 10);
            pstmt.executeUpdate();
        }

        // Seed Stations
        String stationSql = "INSERT INTO stations (name, location, capacity) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(stationSql)) {
            pstmt.setString(1, "Central Library Dock");
            pstmt.setString(2, "Building A, East Entrance");
            pstmt.setInt(3, 15);
            pstmt.executeUpdate();

            pstmt.setString(1, "Science & Engineering Complex");
            pstmt.setString(2, "Academic Zone Block 4");
            pstmt.setInt(3, 12);
            pstmt.executeUpdate();

            pstmt.setString(1, "Student Dormitories Hub");
            pstmt.setString(2, "North Campus Residential");
            pstmt.setInt(3, 20);
            pstmt.executeUpdate();

            pstmt.setString(1, "Main Campus Gateway");
            pstmt.setString(2, "University Avenue South Gate");
            pstmt.setInt(3, 10);
            pstmt.executeUpdate();
        }

        // Seed Cycles
        String cycleSql = "INSERT INTO cycles (model, brand, type, hourly_rate, status, station_id, battery_percentage, total_rides, last_maintained) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(cycleSql)) {
            Object[][] initialBikes = {
                {"Campus Glide 100", "Trek", "STANDARD", 15.0, "AVAILABLE", 1, -1, 14, "2026-09-10"},
                {"City Commuter X", "Giant", "STANDARD", 15.0, "AVAILABLE", 1, -1, 8, "2026-09-12"},
                {"Veloce Outlaw 7-Speed", "Veloce", "GEARED", 22.0, "AVAILABLE", 2, -1, 26, "2026-09-15"},
                {"Urban Swift 21S", "Decathlon", "GEARED", 22.0, "AVAILABLE", 2, -1, 19, "2026-09-18"},
                {"VoltCampus E-1", "RadPower", "ELECTRIC", 35.0, "AVAILABLE", 3, 94, 32, "2026-09-20"},
                {"VoltCampus E-2", "RadPower", "ELECTRIC", 35.0, "AVAILABLE", 3, 82, 28, "2026-09-22"},
                {"TrailBlazer Pro", "Cannondale", "MOUNTAIN", 25.0, "AVAILABLE", 4, -1, 11, "2026-09-14"},
                {"RockRider 520", "B'Twin", "MOUNTAIN", 25.0, "MAINTENANCE", 4, -1, 40, "2026-09-02"},
                {"Campus Eco Cruiser", "Hero", "STANDARD", 15.0, "AVAILABLE", 2, -1, 6, "2026-09-21"},
                {"VoltGlide Ultra", "Specialized", "ELECTRIC", 38.0, "AVAILABLE", 1, 98, 15, "2026-09-24"}
            };

            for (Object[] b : initialBikes) {
                pstmt.setString(1, (String) b[0]);
                pstmt.setString(2, (String) b[1]);
                pstmt.setString(3, (String) b[2]);
                pstmt.setDouble(4, (Double) b[3]);
                pstmt.setString(5, (String) b[4]);
                pstmt.setInt(6, (Integer) b[5]);
                pstmt.setInt(7, (Integer) b[6]);
                pstmt.setInt(8, (Integer) b[7]);
                pstmt.setString(9, (String) b[8]);
                pstmt.executeUpdate();
            }
        }

        // Seed Sample Rentals
        String rentalSql = "INSERT INTO rentals (user_id, cycle_id, start_time, end_time, duration_hours, total_cost, status, start_station_id, end_station_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(rentalSql)) {
            pstmt.setInt(1, 2); // Mohosin
            pstmt.setInt(2, 3); // Veloce Outlaw
            pstmt.setString(3, "2026-09-23 10:15:00");
            pstmt.setString(4, "2026-09-23 12:15:00");
            pstmt.setInt(5, 2);
            pstmt.setDouble(6, 33.0); // discounted rate
            pstmt.setString(7, "COMPLETED");
            pstmt.setInt(8, 2);
            pstmt.setInt(9, 1);
            pstmt.setString(10, "Returned on time, excellent condition.");
            pstmt.executeUpdate();

            pstmt.setInt(1, 3); // Sara
            pstmt.setInt(2, 5); // VoltCampus E-1
            pstmt.setString(3, "2026-09-24 14:00:00");
            pstmt.setString(4, "2026-09-24 15:30:00");
            pstmt.setInt(5, 2);
            pstmt.setDouble(6, 52.5);
            pstmt.setString(7, "COMPLETED");
            pstmt.setInt(8, 3);
            pstmt.setInt(9, 3);
            pstmt.setString(10, "Battery returned at 94%.");
            pstmt.executeUpdate();
        }

        // Seed Sample Payments
        String paymentSql = "INSERT INTO payments (rental_id, amount, payment_method, payment_status, transaction_date, transaction_ref) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(paymentSql)) {
            pstmt.setInt(1, 1);
            pstmt.setDouble(2, 33.0);
            pstmt.setString(3, "CAMPUS_CARD");
            pstmt.setString(4, "PAID");
            pstmt.setString(5, "2026-09-23 12:16:00");
            pstmt.setString(6, "TXN-CC-20260923-001");
            pstmt.executeUpdate();

            pstmt.setInt(1, 2);
            pstmt.setDouble(2, 52.5);
            pstmt.setString(3, "BKASH");
            pstmt.setString(4, "PAID");
            pstmt.setString(5, "2026-09-24 15:31:00");
            pstmt.setString(6, "TXN-BK-20260924-042");
            pstmt.executeUpdate();
        }

        LOGGER.info("Seeding completed successfully.");
    }
}
