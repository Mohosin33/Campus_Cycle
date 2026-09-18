package com.campuscycle.dao;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.Payment;
import com.campuscycle.model.PaymentMethod;
import com.campuscycle.model.PaymentStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Payment processing and transaction records.
 */
public class PaymentDao implements GenericDao<Payment, Integer> {
    private static final Logger LOGGER = Logger.getLogger(PaymentDao.class.getName());
    private final DatabaseManager dbManager;

    public PaymentDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Payment save(Payment payment) {
        String sql = """
            INSERT INTO payments (rental_id, amount, payment_method, payment_status, transaction_date, transaction_ref)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, payment.getRentalId());
            pstmt.setDouble(2, payment.getAmount());
            pstmt.setString(3, payment.getMethod().name());
            pstmt.setString(4, payment.getStatus().name());
            pstmt.setString(5, payment.getTransactionDate());
            pstmt.setString(6, payment.getTransactionReference());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setId(rs.getInt(1));
                    }
                }
            }
            return payment;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving payment", e);
            return null;
        }
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        String sql = "SELECT * FROM payments WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding payment: " + id, e);
        }
        return Optional.empty();
    }

    public List<Payment> findByRentalId(int rentalId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE rental_id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rentalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding payment by rental ID: " + rentalId, e);
        }
        return list;
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments ORDER BY id DESC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all payments", e);
        }
        return list;
    }

    @Override
    public boolean update(Payment payment) {
        String sql = "UPDATE payments SET amount = ?, payment_method = ?, payment_status = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, payment.getAmount());
            pstmt.setString(2, payment.getMethod().name());
            pstmt.setString(3, payment.getStatus().name());
            pstmt.setInt(4, payment.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating payment ID: " + payment.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM payments WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting payment ID: " + id, e);
            return false;
        }
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        return new Payment(
            rs.getInt("id"),
            rs.getInt("rental_id"),
            rs.getDouble("amount"),
            PaymentMethod.valueOf(rs.getString("payment_method")),
            PaymentStatus.valueOf(rs.getString("payment_status")),
            rs.getString("transaction_date"),
            rs.getString("transaction_ref")
        );
    }
}
