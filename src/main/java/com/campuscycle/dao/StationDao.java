package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.Station;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Station management.
 */
public class StationDao implements GenericDao<Station, Integer> {
    private static final Logger LOGGER = Logger.getLogger(StationDao.class.getName());
    private final DatabaseManager dbManager;

    public StationDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Station save(Station station) {
        String sql = "INSERT INTO stations (name, location, capacity) VALUES (?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, station.getName());
            pstmt.setString(2, station.getLocation());
            pstmt.setInt(3, station.getCapacity());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        station.setId(rs.getInt(1));
                    }
                }
            }
            return station;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving station", e);
            return null;
        }
    }

    @Override
    public Optional<Station> findById(Integer id) {
        String sql = "SELECT * FROM stations WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Station(rs.getInt("id"), rs.getString("name"), rs.getString("location"), rs.getInt("capacity")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding station ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Station> findAll() {
        List<Station> list = new ArrayList<>();
        String sql = "SELECT * FROM stations ORDER BY name ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Station(rs.getInt("id"), rs.getString("name"), rs.getString("location"), rs.getInt("capacity")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving stations", e);
        }
        return list;
    }

    @Override
    public boolean update(Station station) {
        String sql = "UPDATE stations SET name = ?, location = ?, capacity = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, station.getName());
            pstmt.setString(2, station.getLocation());
            pstmt.setInt(3, station.getCapacity());
            pstmt.setInt(4, station.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating station ID: " + station.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM stations WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting station ID: " + id, e);
            return false;
        }
    }
}
