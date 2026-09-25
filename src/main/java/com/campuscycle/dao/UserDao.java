package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Production Data Access Object for User entities.
 * Supports cryptographic salt/hash, wallet balance management, and account activation flags.
 */
public class UserDao implements GenericDao<User, Integer> {
    private static final Logger LOGGER = Logger.getLogger(UserDao.class.getName());
    private final DatabaseManager dbManager;

    public UserDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public User save(User user) {
        String sql = """
            INSERT INTO users (username, password_hash, password_salt, full_name, email, phone, role, specific_id, department, loyalty_points, wallet_balance, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPasswordSalt());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getRole().name());
            pstmt.setString(8, user.getRoleSpecificId());
            pstmt.setString(9, user.getDepartment());
            int points = (user instanceof Student) ? ((Student) user).getLoyaltyPoints() : 0;
            pstmt.setInt(10, points);
            pstmt.setDouble(11, user.getWalletBalance());
            pstmt.setInt(12, user.isActive() ? 1 : 0);
            pstmt.setString(13, user.getCreatedAt());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                    }
                }
            }
            return user;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving user: " + user.getUsername(), e);
            return null;
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all users", e);
        }
        return list;
    }

    @Override
    public boolean update(User user) {
        String sql = """
            UPDATE users
            SET full_name = ?, email = ?, phone = ?, specific_id = ?, department = ?, loyalty_points = ?, wallet_balance = ?, is_active = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPhone());
            pstmt.setString(4, user.getRoleSpecificId());
            pstmt.setString(5, user.getDepartment());
            int points = (user instanceof Student) ? ((Student) user).getLoyaltyPoints() : 0;
            pstmt.setInt(6, points);
            pstmt.setDouble(7, user.getWalletBalance());
            pstmt.setInt(8, user.isActive() ? 1 : 0);
            pstmt.setInt(9, user.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user ID: " + user.getId(), e);
            return false;
        }
    }

    public boolean updateWalletBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET wallet_balance = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating wallet balance for user: " + userId, e);
            return false;
        }
    }

    public boolean updateActiveStatus(int userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, isActive ? 1 : 0);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating active status for user: " + userId, e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user ID: " + id, e);
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passHash = rs.getString("password_hash");
        String passSalt = rs.getString("password_salt");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String roleStr = rs.getString("role");
        String specificId = rs.getString("specific_id");
        String dept = rs.getString("department");
        int loyaltyPoints = rs.getInt("loyalty_points");
        double wallet = rs.getDouble("wallet_balance");
        boolean active = rs.getInt("is_active") == 1;
        String createdAt = rs.getString("created_at");

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            role = UserRole.STUDENT;
        }

        switch (role) {
            case ADMIN:
                return new Admin(id, username, passHash, passSalt, name, email, phone, specificId, dept, wallet, active, createdAt);
            case STAFF:
                return new Staff(id, username, passHash, passSalt, name, email, phone, specificId, dept, wallet, active, createdAt);
            case STUDENT:
            default:
                return new Student(id, username, passHash, passSalt, name, email, phone, specificId, dept, loyaltyPoints, wallet, active, createdAt);
        }
    }
}
