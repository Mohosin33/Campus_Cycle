package com.campuscycle.service;

import com.campuscycle.dao.UserDao;
import com.campuscycle.dao.WalletTransactionDao;
import com.campuscycle.model.User;
import com.campuscycle.model.WalletTransaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service managing Campus Pay digital wallet balances and double-entry financial ledger.
 */
public class WalletService {
    private static final Logger LOGGER = Logger.getLogger(WalletService.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserDao userDao;
    private final WalletTransactionDao transactionDao;

    public WalletService() {
        this.userDao = new UserDao();
        this.transactionDao = new WalletTransactionDao();
    }

    /**
     * Tops up user prepaid wallet balance.
     */
    public synchronized boolean topUpBalance(int userId, double amount, String description) {
        if (amount <= 0) return false;

        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        double newBalance = Math.round((user.getWalletBalance() + amount) * 100.0) / 100.0;
        user.setWalletBalance(newBalance);

        boolean updated = userDao.updateWalletBalance(userId, newBalance);
        if (!updated) return false;

        String ref = "TOP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String time = LocalDateTime.now().format(FORMATTER);

        WalletTransaction tx = new WalletTransaction(
            0, userId, amount, WalletTransaction.TransactionType.DEPOSIT,
            newBalance, time, description != null ? description : "Wallet Top-up", ref
        );
        transactionDao.save(tx);

        LOGGER.info(String.format("Wallet topped up for user #%d: +$%.2f. New Balance: $%.2f", userId, amount, newBalance));
        return true;
    }

    /**
     * Deducts fare or penalties from user prepaid balance.
     */
    public synchronized boolean deductBalance(int userId, double amount, WalletTransaction.TransactionType type, String description) {
        if (amount <= 0) return false;

        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        if (user.getWalletBalance() < amount) {
            return false; // Insufficient funds
        }

        double newBalance = Math.round((user.getWalletBalance() - amount) * 100.0) / 100.0;
        user.setWalletBalance(newBalance);

        boolean updated = userDao.updateWalletBalance(userId, newBalance);
        if (!updated) return false;

        String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String time = LocalDateTime.now().format(FORMATTER);

        WalletTransaction tx = new WalletTransaction(
            0, userId, -amount, type,
            newBalance, time, description, ref
        );
        transactionDao.save(tx);

        LOGGER.info(String.format("Wallet deduction for user #%d: -$%.2f. New Balance: $%.2f", userId, amount, newBalance));
        return true;
    }

    public double getBalance(int userId) {
        return userDao.findById(userId).map(User::getWalletBalance).orElse(0.0);
    }

    public List<WalletTransaction> getTransactionHistory(int userId) {
        return transactionDao.findByUserId(userId);
    }
}
