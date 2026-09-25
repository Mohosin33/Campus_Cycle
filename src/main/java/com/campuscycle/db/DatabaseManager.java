package com.campuscycle.db;

import com.campuscycle.security.PasswordHasher;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Database Manager for SQLite.
 * Dockless campus cycle marketplace architecture.
 * Three-role schema: ADMIN, OWNER, RIDER.
 * Cycles feature free-floating campus locations without fixed dock stations.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "campuscycle.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_FILE;
    private static DatabaseManager instance;

    private DatabaseManager() { initDatabase(); }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
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
            LOGGER.info("Initializing CampusCycle dockless database schema…");

            // ── Users ────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    username       TEXT UNIQUE NOT NULL,
                    password_hash  TEXT NOT NULL,
                    password_salt  TEXT NOT NULL,
                    full_name      TEXT NOT NULL,
                    email          TEXT,
                    phone          TEXT,
                    role           TEXT NOT NULL,
                    specific_id    TEXT,
                    department     TEXT,
                    loyalty_points INTEGER DEFAULT 0,
                    wallet_balance REAL    DEFAULT 0.0,
                    is_active      INTEGER DEFAULT 1,
                    created_at     TEXT
                );
            """);

            // ── Cycles (Dockless: location string, owner_id FK) ──────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cycles (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    model              TEXT NOT NULL,
                    brand              TEXT NOT NULL,
                    type               TEXT NOT NULL,
                    hourly_rate        REAL NOT NULL,
                    status             TEXT NOT NULL,
                    location           TEXT NOT NULL DEFAULT 'Campus Central Plaza',
                    battery_percentage INTEGER DEFAULT -1,
                    total_rides        INTEGER DEFAULT 0,
                    last_maintained    TEXT,
                    owner_id           INTEGER,
                    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
                );
            """);

            // ── Rentals (Dockless: pickup_location and return_location) 
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rentals (
                    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id                 INTEGER NOT NULL,
                    cycle_id                INTEGER NOT NULL,
                    start_time              TEXT    NOT NULL,
                    end_time                TEXT,
                    duration_hours          INTEGER NOT NULL,
                    total_cost              REAL    NOT NULL,
                    overdue_fine            REAL    DEFAULT 0.0,
                    actual_duration_minutes INTEGER DEFAULT 0,
                    status                  TEXT    NOT NULL,
                    pickup_location         TEXT    DEFAULT 'Campus Central Plaza',
                    return_location         TEXT,
                    notes                   TEXT,
                    FOREIGN KEY (user_id)   REFERENCES users(id)  ON DELETE CASCADE,
                    FOREIGN KEY (cycle_id)  REFERENCES cycles(id) ON DELETE CASCADE
                );
            """);

            // ── Payments ─────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    rental_id        INTEGER NOT NULL,
                    amount           REAL    NOT NULL,
                    payment_method   TEXT    NOT NULL,
                    payment_status   TEXT    NOT NULL,
                    transaction_date TEXT    NOT NULL,
                    transaction_ref  TEXT    UNIQUE NOT NULL,
                    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE
                );
            """);

            // ── Wallet Transactions ──────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS wallet_transactions (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id        INTEGER NOT NULL,
                    amount         REAL    NOT NULL,
                    type           TEXT    NOT NULL,
                    balance_after  REAL    NOT NULL,
                    timestamp      TEXT    NOT NULL,
                    description    TEXT,
                    reference_code TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // ── Maintenance Tickets ──────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS maintenance_tickets (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    cycle_id            INTEGER NOT NULL,
                    reported_by_user_id INTEGER,
                    issue_category      TEXT    NOT NULL,
                    description         TEXT,
                    status              TEXT    NOT NULL,
                    reported_at         TEXT    NOT NULL,
                    resolved_at         TEXT,
                    technician_notes    TEXT,
                    repair_cost         REAL    DEFAULT 0.0,
                    FOREIGN KEY (cycle_id)            REFERENCES cycles(id) ON DELETE CASCADE,
                    FOREIGN KEY (reported_by_user_id) REFERENCES users(id)  ON DELETE SET NULL
                );
            """);

            LOGGER.info("Dockless database schema initialized successfully.");
            seedInitialData(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        try (Statement check = conn.createStatement();
             ResultSet rs = check.executeQuery("SELECT COUNT(*) FROM users;")) {
            if (rs.next() && rs.getInt(1) > 0) return; // Already seeded
        }

        LOGGER.info("Seeding initial data for dockless campus cycle system…");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ── Seed Users ───────────────────────────────────────────────
        String userSql = """
            INSERT INTO users (username, password_hash, password_salt, full_name, email, phone, role, specific_id, department, loyalty_points, wallet_balance, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        int adminId = 0, owner1Id = 0, owner2Id = 0, rider1Id = 0, rider2Id = 0;

        try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
            // 1. Admin
            String salt = PasswordHasher.generateSalt();
            setUserParams(ps, "admin", PasswordHasher.hashPassword("admin123", salt), salt,
                "Campus Transport Admin", "admin@campuscycle.edu", "+8801700000001",
                "ADMIN", "ADM-001", "Campus Logistics", 0, 500.0, 1, now);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) adminId = k.getInt(1); }

            // 2. Owner 1 (Karim Rahman)
            salt = PasswordHasher.generateSalt();
            setUserParams(ps, "owner1", PasswordHasher.hashPassword("owner123", salt), salt,
                "Karim Rahman", "karim@rahmanrentals.com", "+8801700000002",
                "OWNER", "OWN-001", "North Campus Zone", 0, 200.0, 1, now);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) owner1Id = k.getInt(1); }

            // 3. Owner 2 (Sadia Islam)
            salt = PasswordHasher.generateSalt();
            setUserParams(ps, "owner2", PasswordHasher.hashPassword("owner123", salt), salt,
                "Sadia Islam", "sadia@greenwheels.com", "+8801700000003",
                "OWNER", "OWN-002", "South Campus Zone", 0, 150.0, 1, now);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) owner2Id = k.getInt(1); }

            // 4. Rider 1 (Mohosin Khan)
            salt = PasswordHasher.generateSalt();
            setUserParams(ps, "rider1", PasswordHasher.hashPassword("rider123", salt), salt,
                "Mohosin Khan", "mohosin@student.edu", "+8801700000004",
                "RIDER", "RDR-001", "Computer Science", 45, 80.0, 1, now);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) rider1Id = k.getInt(1); }

            // 5. Rider 2 (Sara Ahmed)
            salt = PasswordHasher.generateSalt();
            setUserParams(ps, "rider2", PasswordHasher.hashPassword("rider123", salt), salt,
                "Sara Ahmed", "sara@student.edu", "+8801700000005",
                "RIDER", "RDR-002", "Business Administration", 20, 50.0, 1, now);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) rider2Id = k.getInt(1); }
        }

        // ── Seed Cycles with Campus Locations ─────────────────────────
        String cyc = "INSERT INTO cycles (model, brand, type, hourly_rate, status, location, battery_percentage, total_rides, last_maintained, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(cyc)) {
            // Owner 1 cycles
            insertCycle(ps, "Campus Glide 100",    "Trek",      "STANDARD", 15.0, "AVAILABLE", "Central Library", -1, 14, "2026-09-10", owner1Id);
            insertCycle(ps, "City Commuter X",     "Giant",     "STANDARD", 15.0, "AVAILABLE", "Science Complex", -1,  8, "2026-09-12", owner1Id);
            insertCycle(ps, "Veloce Outlaw 7S",    "Veloce",    "GEARED",   22.0, "AVAILABLE", "Engineering Block 4", -1, 26, "2026-09-15", owner1Id);
            insertCycle(ps, "VoltCampus E-1",      "RadPower",  "ELECTRIC", 35.0, "AVAILABLE", "Student Dormitories", 94, 32, "2026-09-20", owner1Id);
            insertCycle(ps, "VoltCampus E-2",      "RadPower",  "ELECTRIC", 35.0, "AVAILABLE", "Main Campus Gateway", 82, 28, "2026-09-22", owner1Id);
            // Owner 2 cycles
            insertCycle(ps, "Urban Swift 21S",     "Decathlon", "GEARED",   22.0, "AVAILABLE", "Cafeteria Plaza", -1, 19, "2026-09-18", owner2Id);
            insertCycle(ps, "TrailBlazer Pro",     "Cannondale","MOUNTAIN",  25.0, "AVAILABLE", "Sports Pavilion", -1, 11, "2026-09-14", owner2Id);
            insertCycle(ps, "RockRider 520",       "B'Twin",    "MOUNTAIN",  25.0, "MAINTENANCE","Repair Workshop",-1, 40, "2026-09-02", owner2Id);
            insertCycle(ps, "Campus Eco Cruiser",  "Hero",      "STANDARD", 15.0, "AVAILABLE", "Administrative Building", -1, 6, "2026-09-21", owner2Id);
            insertCycle(ps, "VoltGlide Ultra",     "Specialized","ELECTRIC", 38.0, "AVAILABLE", "Central Library", 98, 15, "2026-09-24", owner2Id);
        }

        // ── Seed Wallet Opening Deposits ─────────────────────────────
        String walletSql = "INSERT INTO wallet_transactions (user_id, amount, type, balance_after, timestamp, description, reference_code) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(walletSql)) {
            insertWallet(ps, rider1Id, 80.0, "DEP-R1-001", "Initial Rider wallet deposit", now);
            insertWallet(ps, rider2Id, 50.0, "DEP-R2-001", "Initial Rider wallet deposit", now);
        }

        // ── Seed One Maintenance Ticket (RockRider 520 = cycle #8) ──
        String mSql = "INSERT INTO maintenance_tickets (cycle_id, reported_by_user_id, issue_category, description, status, reported_at, technician_notes, repair_cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(mSql)) {
            ps.setInt(1, 8);
            ps.setInt(2, rider1Id);
            ps.setString(3, "FLAT_TIRE");
            ps.setString(4, "Rear tire punctured near Dormitory North slope");
            ps.setString(5, "OPEN");
            ps.setString(6, "2026-09-24 16:30:00");
            ps.setString(7, "Awaiting 27.5-inch inner tube replacement");
            ps.setDouble(8, 8.50);
            ps.executeUpdate();
        }

        LOGGER.info("Seeding completed for dockless campus cycle system.");
    }

    private void setUserParams(PreparedStatement ps, String username, String hash, String salt,
                               String name, String email, String phone, String role,
                               String specificId, String dept, int points, double wallet,
                               int active, String now) throws SQLException {
        ps.setString(1, username); ps.setString(2, hash); ps.setString(3, salt);
        ps.setString(4, name);     ps.setString(5, email); ps.setString(6, phone);
        ps.setString(7, role);     ps.setString(8, specificId); ps.setString(9, dept);
        ps.setInt(10, points);     ps.setDouble(11, wallet); ps.setInt(12, active);
        ps.setString(13, now);
    }

    private void insertCycle(PreparedStatement ps, String model, String brand, String type,
                             double rate, String status, String location, int battery,
                             int rides, String maint, int ownerId) throws SQLException {
        ps.setString(1, model); ps.setString(2, brand); ps.setString(3, type);
        ps.setDouble(4, rate);  ps.setString(5, status); ps.setString(6, location);
        ps.setInt(7, battery);  ps.setInt(8, rides);     ps.setString(9, maint);
        ps.setInt(10, ownerId);
        ps.executeUpdate();
    }

    private void insertWallet(PreparedStatement ps, int userId, double amount,
                              String ref, String desc, String ts) throws SQLException {
        ps.setInt(1, userId);   ps.setDouble(2, amount); ps.setString(3, "DEPOSIT");
        ps.setDouble(4, amount);ps.setString(5, ts);     ps.setString(6, desc);
        ps.setString(7, ref);
        ps.executeUpdate();
    }
}
