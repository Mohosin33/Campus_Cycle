package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.Rental;
import com.campuscycle.model.RentalStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Rental transactions in a dockless campus cycle system.
 */
public class RentalDao implements GenericDao<Rental, Integer> {
    private static final Logger LOGGER = Logger.getLogger(RentalDao.class.getName());
    private final DatabaseManager dbManager;

    public RentalDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Rental save(Rental rental) {
        String sql = """
            INSERT INTO rentals (user_id, cycle_id, start_time, end_time, duration_hours, total_cost, status, pickup_location, return_location, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, rental.getUserId());
            pstmt.setInt(2, rental.getCycleId());
            pstmt.setString(3, rental.getStartTime());
            if (rental.getEndTime() != null) pstmt.setString(4, rental.getEndTime());
            else pstmt.setNull(4, Types.VARCHAR);
            pstmt.setInt(5, rental.getDurationHours());
            pstmt.setDouble(6, rental.getTotalCost());
            pstmt.setString(7, rental.getStatus().name());
            pstmt.setString(8, rental.getPickupLocation());
            if (rental.getReturnLocation() != null) pstmt.setString(9, rental.getReturnLocation());
            else pstmt.setNull(9, Types.VARCHAR);
            pstmt.setString(10, rental.getNotes());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        rental.setId(rs.getInt(1));
                    }
                }
            }
            return rental;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating rental: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Optional<Rental> findById(Integer id) {
        String sql = """
            SELECT r.*, u.full_name as user_name, (c.brand || ' ' || c.model) as cycle_name
            FROM rentals r
            JOIN users u ON r.user_id = u.id
            JOIN cycles c ON r.cycle_id = c.id
            WHERE r.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rental ID: " + id, e);
        }
        return Optional.empty();
    }

    public Optional<Rental> findActiveRentalByUser(int userId) {
        String sql = """
            SELECT r.*, u.full_name as user_name, (c.brand || ' ' || c.model) as cycle_name
            FROM rentals r
            JOIN users u ON r.user_id = u.id
            JOIN cycles c ON r.cycle_id = c.id
            WHERE r.user_id = ? AND r.status = 'ACTIVE'
            ORDER BY r.id DESC LIMIT 1;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding active rental for user: " + userId, e);
        }
        return Optional.empty();
    }

    public List<Rental> findByUser(int userId) {
        List<Rental> list = new ArrayList<>();
        String sql = """
            SELECT r.*, u.full_name as user_name, (c.brand || ' ' || c.model) as cycle_name
            FROM rentals r
            JOIN users u ON r.user_id = u.id
            JOIN cycles c ON r.cycle_id = c.id
            WHERE r.user_id = ?
            ORDER BY r.id DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rentals for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Rental> findAll() {
        List<Rental> list = new ArrayList<>();
        String sql = """
            SELECT r.*, u.full_name as user_name, (c.brand || ' ' || c.model) as cycle_name
            FROM rentals r
            JOIN users u ON r.user_id = u.id
            JOIN cycles c ON r.cycle_id = c.id
            ORDER BY r.id DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRental(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all rentals", e);
        }
        return list;
    }

    @Override
    public boolean update(Rental rental) {
        String sql = """
            UPDATE rentals
            SET end_time = ?, duration_hours = ?, total_cost = ?, status = ?, return_location = ?, notes = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (rental.getEndTime() != null) pstmt.setString(1, rental.getEndTime());
            else pstmt.setNull(1, Types.VARCHAR);
            pstmt.setInt(2, rental.getDurationHours());
            pstmt.setDouble(3, rental.getTotalCost());
            pstmt.setString(4, rental.getStatus().name());
            pstmt.setString(5, rental.getReturnLocation());
            pstmt.setString(6, rental.getNotes());
            pstmt.setInt(7, rental.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating rental: " + rental.getId() + " - " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM rentals WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting rental: " + id, e);
            return false;
        }
    }

    private Rental mapResultSetToRental(ResultSet rs) throws SQLException {
        return new Rental(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("user_name"),
            rs.getInt("cycle_id"),
            rs.getString("cycle_name"),
            rs.getString("start_time"),
            rs.getString("end_time"),
            rs.getInt("duration_hours"),
            rs.getDouble("total_cost"),
            RentalStatus.valueOf(rs.getString("status")),
            rs.getString("pickup_location"),
            rs.getString("return_location"),
            rs.getString("notes")
        );
    }
}
