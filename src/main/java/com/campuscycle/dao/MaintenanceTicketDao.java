package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.MaintenanceTicket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for cycle maintenance tickets and rider damage reports.
 */
public class MaintenanceTicketDao implements GenericDao<MaintenanceTicket, Integer> {
    private static final Logger LOGGER = Logger.getLogger(MaintenanceTicketDao.class.getName());
    private final DatabaseManager dbManager;

    public MaintenanceTicketDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public MaintenanceTicket save(MaintenanceTicket t) {
        String sql = """
            INSERT INTO maintenance_tickets (cycle_id, reported_by_user_id, issue_category, description, status, reported_at, resolved_at, technician_notes, repair_cost)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, t.getCycleId());
            pstmt.setInt(2, t.getReportedByUserId());
            pstmt.setString(3, t.getIssueCategory().name());
            pstmt.setString(4, t.getDescription());
            pstmt.setString(5, t.getStatus().name());
            pstmt.setString(6, t.getReportedAt());
            pstmt.setString(7, t.getResolvedAt());
            pstmt.setString(8, t.getTechnicianNotes());
            pstmt.setDouble(9, t.getRepairCost());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        t.setId(rs.getInt(1));
                    }
                }
            }
            return t;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting maintenance ticket", e);
            return null;
        }
    }

    @Override
    public Optional<MaintenanceTicket> findById(Integer id) {
        String sql = """
            SELECT m.*, (c.brand || ' ' || c.model) as cycle_name, u.full_name as user_name
            FROM maintenance_tickets m
            JOIN cycles c ON m.cycle_id = c.id
            LEFT JOIN users u ON m.reported_by_user_id = u.id
            WHERE m.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding maintenance ticket: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<MaintenanceTicket> findAll() {
        List<MaintenanceTicket> list = new ArrayList<>();
        String sql = """
            SELECT m.*, (c.brand || ' ' || c.model) as cycle_name, u.full_name as user_name
            FROM maintenance_tickets m
            JOIN cycles c ON m.cycle_id = c.id
            LEFT JOIN users u ON m.reported_by_user_id = u.id
            ORDER BY m.id DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all maintenance tickets", e);
        }
        return list;
    }

    public List<MaintenanceTicket> findOpenTickets() {
        List<MaintenanceTicket> list = new ArrayList<>();
        String sql = """
            SELECT m.*, (c.brand || ' ' || c.model) as cycle_name, u.full_name as user_name
            FROM maintenance_tickets m
            JOIN cycles c ON m.cycle_id = c.id
            LEFT JOIN users u ON m.reported_by_user_id = u.id
            WHERE m.status != 'RESOLVED'
            ORDER BY m.id DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying open maintenance tickets", e);
        }
        return list;
    }

    @Override
    public boolean update(MaintenanceTicket t) {
        String sql = """
            UPDATE maintenance_tickets
            SET status = ?, resolved_at = ?, technician_notes = ?, repair_cost = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, t.getStatus().name());
            pstmt.setString(2, t.getResolvedAt());
            pstmt.setString(3, t.getTechnicianNotes());
            pstmt.setDouble(4, t.getRepairCost());
            pstmt.setInt(5, t.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating maintenance ticket: " + t.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM maintenance_tickets WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting maintenance ticket: " + id, e);
            return false;
        }
    }

    private MaintenanceTicket mapResultSet(ResultSet rs) throws SQLException {
        return new MaintenanceTicket(
            rs.getInt("id"),
            rs.getInt("cycle_id"),
            rs.getString("cycle_name"),
            rs.getInt("reported_by_user_id"),
            rs.getString("user_name"),
            MaintenanceTicket.IssueCategory.valueOf(rs.getString("issue_category")),
            rs.getString("description"),
            MaintenanceTicket.TicketStatus.valueOf(rs.getString("status")),
            rs.getString("reported_at"),
            rs.getString("resolved_at"),
            rs.getString("technician_notes"),
            rs.getDouble("repair_cost")
        );
    }
}
