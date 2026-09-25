package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.Cycle;
import com.campuscycle.model.CycleStatus;
import com.campuscycle.model.CycleType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Cycle fleet management.
 * Supports owner-linked cycles and full CRUD operations.
 */
public class CycleDao implements GenericDao<Cycle, Integer> {
    private static final Logger LOGGER = Logger.getLogger(CycleDao.class.getName());
    private final DatabaseManager dbManager;

    public CycleDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Cycle save(Cycle cycle) {
        String sql = """
            INSERT INTO cycles (model, brand, type, hourly_rate, status, station_id, battery_percentage, total_rides, last_maintained, owner_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, cycle.getModel());
            pstmt.setString(2, cycle.getBrand());
            pstmt.setString(3, cycle.getType().name());
            pstmt.setDouble(4, cycle.getHourlyRate());
            pstmt.setString(5, cycle.getStatus().name());
            if (cycle.getStationId() > 0) pstmt.setInt(6, cycle.getStationId());
            else pstmt.setNull(6, Types.INTEGER);
            pstmt.setInt(7, cycle.getBatteryPercentage());
            pstmt.setInt(8, cycle.getTotalRides());
            pstmt.setString(9, cycle.getLastMaintainedDate());
            if (cycle.getOwnerId() > 0) pstmt.setInt(10, cycle.getOwnerId());
            else pstmt.setNull(10, Types.INTEGER);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) cycle.setId(rs.getInt(1));
                }
            }
            return cycle;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting cycle: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Optional<Cycle> findById(Integer id) {
        String sql = """
            SELECT c.*, s.name as station_name, u.full_name as owner_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            LEFT JOIN users u ON c.owner_id = u.id
            WHERE c.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToCycle(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding cycle ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cycle> findAll() {
        List<Cycle> list = new ArrayList<>();
        String sql = """
            SELECT c.*, s.name as station_name, u.full_name as owner_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            LEFT JOIN users u ON c.owner_id = u.id
            ORDER BY c.id ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToCycle(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving cycles", e);
        }
        return list;
    }

    /** Returns only cycles belonging to a specific owner */
    public List<Cycle> findByOwner(int ownerId) {
        List<Cycle> list = new ArrayList<>();
        String sql = """
            SELECT c.*, s.name as station_name, u.full_name as owner_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            LEFT JOIN users u ON c.owner_id = u.id
            WHERE c.owner_id = ?
            ORDER BY c.id ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToCycle(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding cycles for owner: " + ownerId, e);
        }
        return list;
    }

    public List<Cycle> findAvailable() {
        List<Cycle> list = new ArrayList<>();
        String sql = """
            SELECT c.*, s.name as station_name, u.full_name as owner_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            LEFT JOIN users u ON c.owner_id = u.id
            WHERE c.status = 'AVAILABLE'
            ORDER BY c.type ASC, c.model ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToCycle(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving available cycles", e);
        }
        return list;
    }

    @Override
    public boolean update(Cycle cycle) {
        String sql = """
            UPDATE cycles
            SET model = ?, brand = ?, type = ?, hourly_rate = ?, status = ?, station_id = ?, battery_percentage = ?, total_rides = ?, last_maintained = ?, owner_id = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cycle.getModel());
            pstmt.setString(2, cycle.getBrand());
            pstmt.setString(3, cycle.getType().name());
            pstmt.setDouble(4, cycle.getHourlyRate());
            pstmt.setString(5, cycle.getStatus().name());
            if (cycle.getStationId() > 0) pstmt.setInt(6, cycle.getStationId());
            else pstmt.setNull(6, Types.INTEGER);
            pstmt.setInt(7, cycle.getBatteryPercentage());
            pstmt.setInt(8, cycle.getTotalRides());
            pstmt.setString(9, cycle.getLastMaintainedDate());
            if (cycle.getOwnerId() > 0) pstmt.setInt(10, cycle.getOwnerId());
            else pstmt.setNull(10, Types.INTEGER);
            pstmt.setInt(11, cycle.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating cycle ID: " + cycle.getId(), e);
            return false;
        }
    }

    public boolean updateStatus(int cycleId, CycleStatus status, int newStationId) {
        String sql;
        if (newStationId > 0) {
            sql = "UPDATE cycles SET status = ?, station_id = ? WHERE id = ?;";
        } else {
            sql = "UPDATE cycles SET status = ? WHERE id = ?;";
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            if (newStationId > 0) {
                pstmt.setInt(2, newStationId);
                pstmt.setInt(3, cycleId);
            } else {
                pstmt.setInt(2, cycleId);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating status for cycle: " + cycleId, e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM cycles WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting cycle ID: " + id, e);
            return false;
        }
    }

    private Cycle mapResultSetToCycle(ResultSet rs) throws SQLException {
        int id              = rs.getInt("id");
        String model        = rs.getString("model");
        String brand        = rs.getString("brand");
        CycleType type      = CycleType.valueOf(rs.getString("type"));
        double rate         = rs.getDouble("hourly_rate");
        CycleStatus status  = CycleStatus.valueOf(rs.getString("status"));
        int stationId       = rs.getInt("station_id");
        String stationName  = rs.getString("station_name");
        if (stationName == null) stationName = stationId > 0 ? "Station #" + stationId : "Unassigned";
        int battery         = rs.getInt("battery_percentage");
        int rides           = rs.getInt("total_rides");
        String maint        = rs.getString("last_maintained");
        int ownerId         = rs.getInt("owner_id");
        String ownerName    = rs.getString("owner_name");
        if (ownerName == null) ownerName = "";

        return new Cycle(id, model, brand, type, rate, status, stationId, stationName,
                         battery, rides, maint, ownerId, ownerName);
    }
}
