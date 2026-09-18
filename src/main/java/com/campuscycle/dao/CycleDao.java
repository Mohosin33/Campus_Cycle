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
 * Demonstrates SQL relational JOINs, filtering, and complete CRUD operations.
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
            INSERT INTO cycles (model, brand, type, hourly_rate, status, station_id, battery_percentage, total_rides, last_maintained)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, cycle.getModel());
            pstmt.setString(2, cycle.getBrand());
            pstmt.setString(3, cycle.getType().name());
            pstmt.setDouble(4, cycle.getHourlyRate());
            pstmt.setString(5, cycle.getStatus().name());
            pstmt.setInt(6, cycle.getStationId());
            pstmt.setInt(7, cycle.getBatteryPercentage());
            pstmt.setInt(8, cycle.getTotalRides());
            pstmt.setString(9, cycle.getLastMaintainedDate());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cycle.setId(rs.getInt(1));
                    }
                }
            }
            return cycle;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting cycle", e);
            return null;
        }
    }

    @Override
    public Optional<Cycle> findById(Integer id) {
        String sql = """
            SELECT c.*, s.name as station_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            WHERE c.id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCycle(rs));
                }
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
            SELECT c.*, s.name as station_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            ORDER BY c.id ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToCycle(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving cycles", e);
        }
        return list;
    }

    public List<Cycle> findAvailable() {
        List<Cycle> list = new ArrayList<>();
        String sql = """
            SELECT c.*, s.name as station_name
            FROM cycles c
            LEFT JOIN stations s ON c.station_id = s.id
            WHERE c.status = 'AVAILABLE'
            ORDER BY c.type ASC, c.model ASC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToCycle(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving available cycles", e);
        }
        return list;
    }

    @Override
    public boolean update(Cycle cycle) {
        String sql = """
            UPDATE cycles
            SET model = ?, brand = ?, type = ?, hourly_rate = ?, status = ?, station_id = ?, battery_percentage = ?, total_rides = ?, last_maintained = ?
            WHERE id = ?;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cycle.getModel());
            pstmt.setString(2, cycle.getBrand());
            pstmt.setString(3, cycle.getType().name());
            pstmt.setDouble(4, cycle.getHourlyRate());
            pstmt.setString(5, cycle.getStatus().name());
            pstmt.setInt(6, cycle.getStationId());
            pstmt.setInt(7, cycle.getBatteryPercentage());
            pstmt.setInt(8, cycle.getTotalRides());
            pstmt.setString(9, cycle.getLastMaintainedDate());
            pstmt.setInt(10, cycle.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating cycle ID: " + cycle.getId(), e);
            return false;
        }
    }

    public boolean updateStatus(int cycleId, CycleStatus status, int newStationId) {
        String sql = "UPDATE cycles SET status = ?, station_id = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, newStationId);
            pstmt.setInt(3, cycleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating status for cycle ID: " + cycleId, e);
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
        int id = rs.getInt("id");
        String model = rs.getString("model");
        String brand = rs.getString("brand");
        CycleType type = CycleType.valueOf(rs.getString("type"));
        double rate = rs.getDouble("hourly_rate");
        CycleStatus status = CycleStatus.valueOf(rs.getString("status"));
        int stationId = rs.getInt("station_id");
        String stationName = rs.getString("station_name");
        if (stationName == null) {
            stationName = "Station #" + stationId;
        }
        int battery = rs.getInt("battery_percentage");
        int rides = rs.getInt("total_rides");
        String maint = rs.getString("last_maintained");

        return new Cycle(id, model, brand, type, rate, status, stationId, stationName, battery, rides, maint);
    }
}
