package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.WalletTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Wallet financial ledger records.
 */
public class WalletTransactionDao implements GenericDao<WalletTransaction, Integer> {
    private static final Logger LOGGER = Logger.getLogger(WalletTransactionDao.class.getName());
    private final DatabaseManager dbManager;

    public WalletTransactionDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public WalletTransaction save(WalletTransaction tx) {
        String sql = """
            INSERT INTO wallet_transactions (user_id, amount, type, balance_after, timestamp, description, reference_code)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, tx.getUserId());
            pstmt.setDouble(2, tx.getAmount());
            pstmt.setString(3, tx.getType().name());
            pstmt.setDouble(4, tx.getBalanceAfter());
            pstmt.setString(5, tx.getTimestamp());
            pstmt.setString(6, tx.getDescription());
            pstmt.setString(7, tx.getReferenceCode());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tx.setId(rs.getInt(1));
                    }
                }
            }
            return tx;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting wallet transaction", e);
            return null;
        }
    }

    @Override
    public Optional<WalletTransaction> findById(Integer id) {
        String sql = "SELECT * FROM wallet_transactions WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding wallet transaction by ID: " + id, e);
        }
        return Optional.empty();
    }

    public List<WalletTransaction> findByUserId(int userId) {
        List<WalletTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY id DESC;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying user wallet transactions", e);
        }
        return list;
    }

    @Override
    public List<WalletTransaction> findAll() {
        List<WalletTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions ORDER BY id DESC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all wallet transactions", e);
        }
        return list;
    }

    @Override
    public boolean update(WalletTransaction entity) {
        return false; // Transactions are append-only audit ledgers
    }

    @Override
    public boolean delete(Integer id) {
        return false; // Immutable audit log
    }

    private WalletTransaction mapResultSet(ResultSet rs) throws SQLException {
        return new WalletTransaction(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getDouble("amount"),
            WalletTransaction.TransactionType.valueOf(rs.getString("type")),
            rs.getDouble("balance_after"),
            rs.getString("timestamp"),
            rs.getString("description"),
            rs.getString("reference_code")
        );
    }
}
