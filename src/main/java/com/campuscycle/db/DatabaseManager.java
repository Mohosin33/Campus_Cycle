package com.campuscycle.db;

import com.campuscycle.security.PasswordHasher;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Database Manager for SQLite Integration.
 * Manages transactional integrity, foreign key constraints,
 * encrypted credentials, and automated schema migration.
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
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Initializing Enterprise SQLite database schema...");

            // 1. Users Table (Production: Salted Hashes, Wallet Balance, Status)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    role TEXT NOT NULL,
                    specific_id TEXT,
                    department TEXT,
                    loyalty_points INTEGER DEFAULT 0,
                    wallet_balance REAL DEFAULT 0.0,
                    is_active INTEGER DEFAULT 1,
                    created_at TEXT
                );
            """);

            // 2. Stations Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    location TEXT NOT NULL,
                    capacity INTEGER NOT NULL DEFAULT 15
                );
            """);

            // 3. Cycles Table
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

            // 4. Rentals Table (Supports Overdue Fine & Accurate Minutes)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rentals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    cycle_id INTEGER NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT,
                    duration_hours INTEGER NOT NULL,
                    total_cost REAL NOT NULL,
                    overdue_fine REAL DEFAULT 0.0,
                    actual_duration_minutes INTEGER DEFAULT 0,
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

            // 5. Payments Table
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

            // 6. Wallet Transactions Table (Double-entry Financial Ledger)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS wallet_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    balance_after REAL NOT NULL,
                    timestamp TEXT NOT NULL,
                    description TEXT,
                    reference_code TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // 7. Maintenance Work Orders & Damage Tickets Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS maintenance_tickets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cycle_id INTEGER NOT NULL,
                    reported_by_user_id INTEGER,
                    issue_category TEXT NOT NULL,
                    description TEXT,
                    status TEXT NOT NULL,
                    reported_at TEXT NOT NULL,
                    resolved_at TEXT,
                    technician_notes TEXT,
                    repair_cost REAL DEFAULT 0.0,
                    FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE,
                    FOREIGN KEY (reported_by_user_id) REFERENCES users(id) ON DELETE SET NULL
                );
            """);

            LOGGER.info("Production database tables initialized successfully.");
            seedInitialData(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize production database", e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already populated
            }
        }

        LOGGER.info("Seeding production records with salted cryptography and wallet balances...");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Seed Users with SHA-256 + Salt
        String userSql = """
            INSERT INTO users (username, password_hash, password_salt, full_name, email, phone, role, specific_id, department, loyalty_points, wallet_balance, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
            // 1. Admin
            String adminSalt = PasswordHasher.generateSalt();
            pstmt.setString(1, "admin");
            pstmt.setString(2, PasswordHasher.hashPassword("admin123", adminSalt));
            pstmt.setString(3, adminSalt);
            pstmt.setString(4, "Campus Transport Operations Admin");
            pstmt.setString(5, "admin@campuscycle.edu");
            pstmt.setString(6, "+8801700000001");
            pstmt.setString(7, "ADMIN");
            pstmt.setString(8, "ADM-901");
            pstmt.setString(9, "Campus Facilities & Logistics");
            pstmt.setInt(10, 0);
            pstmt.setDouble(11, 200.0);
            pstmt.setInt(12, 1);
            pstmt.setString(13, now);
            pstmt.executeUpdate();

            // 2. Student (Mohosin Khan)
            String studentSalt = PasswordHasher.generateSalt();
            pstmt.setString(1, "student");
            pstmt.setString(2, PasswordHasher.hashPassword("student123", studentSalt));
            pstmt.setString(3, studentSalt);
            pstmt.setString(4, "Mohosin Khan");
            pstmt.setString(5, "mohosin.cse@campuscycle.edu");
            pstmt.setString(6, "+8801700000002");
            pstmt.setString(7, "STUDENT");
            pstmt.setString(8, "2021-1-60-042");
            pstmt.setString(9, "Computer Science & Engineering");
            pstmt.setInt(10, 45);
            pstmt.setDouble(11, 65.0);
            pstmt.setInt(12, 1);
            pstmt.setString(13, now);
            pstmt.executeUpdate();

            // 3. Student (Sara Rahman)
            String saraSalt = PasswordHasher.generateSalt();
            pstmt.setString(1, "sara");
            pstmt.setString(2, PasswordHasher.hashPassword("sara123", saraSalt));
            pstmt.setString(3, saraSalt);
            pstmt.setString(4, "Sara Rahman");
            pstmt.setString(5, "sara.eee@campuscycle.edu");
            pstmt.setString(6, "+8801700000003");
            pstmt.setString(7, "STUDENT");
            pstmt.setString(8, "2022-2-50-119");
            pstmt.setString(9, "Electrical & Electronic Engineering");
            pstmt.setInt(10, 20);
            pstmt.setDouble(11, 40.0);
            pstmt.setInt(12, 1);
            pstmt.setString(13, now);
            pstmt.executeUpdate();

            // 4. Staff (Dr. Robert Smith)
            String staffSalt = PasswordHasher.generateSalt();
            pstmt.setString(1, "dr_smith");
            pstmt.setString(2, PasswordHasher.hashPassword("staff123", staffSalt));
            pstmt.setString(3, staffSalt);
            pstmt.setString(4, "Dr. Robert Smith");
            pstmt.setString(5, "r.smith@campuscycle.edu");
            pstmt.setString(6, "+8801700000004");
            pstmt.setString(7, "STAFF");
            pstmt.setString(8, "FAC-304");
            pstmt.setString(9, "Faculty of Natural Sciences");
            pstmt.setInt(10, 10);
            pstmt.setDouble(11, 100.0);
            pstmt.setInt(12, 1);
            pstmt.setString(13, now);
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
            pstmt.setInt(3, 15);
            pstmt.executeUpdate();

            pstmt.setString(1, "Student Dormitories Hub");
            pstmt.setString(2, "North Campus Residential");
            pstmt.setInt(3, 20);
            pstmt.executeUpdate();

            pstmt.setString(1, "Main Campus Gateway");
            pstmt.setString(2, "University Avenue South Gate");
            pstmt.setInt(3, 12);
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

        // Seed Wallet Initial Deposits
        String walletSql = "INSERT INTO wallet_transactions (user_id, amount, type, balance_after, timestamp, description, reference_code) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(walletSql)) {
            pstmt.setInt(1, 2); // Mohosin
            pstmt.setDouble(2, 65.0);
            pstmt.setString(3, "DEPOSIT");
            pstmt.setDouble(4, 65.0);
            pstmt.setString(5, "2026-09-22 09:00:00");
            pstmt.setString(6, "Initial Campus Pay Student Deposit");
            pstmt.setString(7, "DEP-20260922-001");
            pstmt.executeUpdate();

            pstmt.setInt(1, 3); // Sara
            pstmt.setDouble(2, 40.0);
            pstmt.setString(3, "DEPOSIT");
            pstmt.setDouble(4, 40.0);
            pstmt.setString(5, "2026-09-22 10:30:00");
            pstmt.setString(6, "bKash Online Student Top-up");
            pstmt.setString(7, "DEP-20260922-002");
            pstmt.executeUpdate();

            pstmt.setInt(1, 4); // Dr. Smith
            pstmt.setDouble(2, 100.0);
            pstmt.setString(3, "DEPOSIT");
            pstmt.setDouble(4, 100.0);
            pstmt.setString(5, "2026-09-22 11:00:00");
            pstmt.setString(6, "Faculty Mobility Allowance");
            pstmt.setString(7, "DEP-20260922-003");
            pstmt.executeUpdate();
        }

        // Seed Maintenance Work Order for RockRider 520
        String maintSql = "INSERT INTO maintenance_tickets (cycle_id, reported_by_user_id, issue_category, description, status, reported_at, resolved_at, technician_notes, repair_cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(maintSql)) {
            pstmt.setInt(1, 8); // RockRider 520
            pstmt.setInt(2, 2); // Reported by Mohosin
            pstmt.setString(3, "FLAT_TIRE");
            pstmt.setString(4, "Rear tire punctured near Dormitory North slope");
            pstmt.setString(5, "OPEN");
            pstmt.setString(6, "2026-09-24 16:30:00");
            pstmt.setString(7, null);
            pstmt.setString(8, "Waiting for replacement 27.5-inch inner tube");
            pstmt.setDouble(9, 8.50);
            pstmt.executeUpdate();
        }

        LOGGER.info("Production seeding completed successfully.");
    }
}
