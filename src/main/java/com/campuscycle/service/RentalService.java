package com.campuscycle.service;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.PaymentDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Enterprise Service managing cycle rental operations, return procedures, and wallet deductions.
 */
public class RentalService {
    private static final Logger LOGGER = Logger.getLogger(RentalService.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RentalDao rentalDao;
    private final CycleDao cycleDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;
    private final WalletService walletService;
    private final MaintenanceService maintenanceService;

    public RentalService() {
        this.rentalDao = new RentalDao();
        this.cycleDao = new CycleDao();
        this.paymentDao = new PaymentDao();
        this.userDao = new UserDao();
        this.walletService = new WalletService();
        this.maintenanceService = new MaintenanceService();
    }

    /**
     * Rents a cycle for a user using their polymorphic pricing strategy and wallet balance.
     */
    public Rental rentCycle(User user, Cycle cycle, int durationHours, PaymentMethod paymentMethod, String notes) throws Exception {
        if (user == null || cycle == null) {
            throw new IllegalArgumentException("User and Cycle must not be null.");
        }

        // Active rental check
        Optional<Rental> activeRental = rentalDao.findActiveRentalByUser(user.getId());
        if (activeRental.isPresent()) {
            throw new IllegalStateException("You already have an active ride (" + activeRental.get().getCycleName() + "). Please return it before booking another.");
        }

        // Cycle availability check
        if (!cycle.isAvailable()) {
            throw new IllegalStateException("Selected cycle is currently unavailable.");
        }

        // Calculate dynamic cost based on user's OOP PricingStrategy
        double totalCost = cycle.calculateCost(durationHours, user.getPricingStrategy());

        // Check wallet balance if paying with Campus Card / Wallet
        if (paymentMethod == PaymentMethod.CAMPUS_CARD) {
            double currentBalance = walletService.getBalance(user.getId());
            if (currentBalance < totalCost) {
                throw new IllegalStateException(String.format("Insufficient Campus Pay wallet balance ($%.2f). Required: $%.2f. Please top up your wallet first.",
                    currentBalance, totalCost));
            }
            walletService.deductBalance(user.getId(), totalCost, WalletTransaction.TransactionType.RENTAL_CHARGE,
                "Prepaid Rental for " + cycle.getDisplayName());
        }

        String startTime = LocalDateTime.now().format(TIME_FORMATTER);

        Rental rental = new Rental(
            0,
            user.getId(),
            user.getFullName(),
            cycle.getId(),
            cycle.getDisplayName(),
            startTime,
            null,
            durationHours,
            totalCost,
            RentalStatus.ACTIVE,
            cycle.getLocation(),
            null,
            notes != null ? notes : "Campus trip"
        );

        Rental savedRental = rentalDao.save(rental);
        if (savedRental == null) {
            throw new RuntimeException("Failed to record rental booking in database.");
        }

        // Mark cycle as RENTED
        cycle.rentOut(user, durationHours);
        cycleDao.updateLocationAndStatus(cycle.getId(), CycleStatus.RENTED, cycle.getLocation());

        // Log payment record
        String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(
            0,
            savedRental.getId(),
            totalCost,
            paymentMethod,
            PaymentStatus.PAID,
            startTime,
            txnRef
        );
        paymentDao.save(payment);

        LOGGER.info(String.format("Rental #%d created for %s on %s. Total: $%.2f",
            savedRental.getId(), user.getUsername(), cycle.getDisplayName(), totalCost));

        return savedRental;
    }

    /**
     * Returns an active rental with dockless return location, overtime detection, and damage ticketing.
     */
    public ReturnReceipt returnCycle(Rental rental, String returnLocation, String returnNotes,
                                     boolean reportDamage, MaintenanceTicket.IssueCategory damageCategory, String damageDetails) {
        if (rental == null || !rental.isActive()) {
            return null;
        }

        LocalDateTime startDt = LocalDateTime.parse(rental.getStartTime(), TIME_FORMATTER);
        LocalDateTime now = LocalDateTime.now();
        String returnTime = now.format(TIME_FORMATTER);

        long actualMinutes = Math.max(1, Duration.between(startDt, now).toMinutes());
        long scheduledMinutes = rental.getDurationHours() * 60L;

        double overdueFine = 0.0;
        if (actualMinutes > scheduledMinutes) {
            long extraMinutes = actualMinutes - scheduledMinutes;
            overdueFine = Math.ceil(extraMinutes / 15.0) * 1.50; // $1.50 per 15 min overdue
            overdueFine = Math.round(overdueFine * 100.0) / 100.0;

            // Deduct overdue fine from wallet if available
            walletService.deductBalance(rental.getUserId(), overdueFine,
                WalletTransaction.TransactionType.OVERDUE_FINE,
                "Overtime fine (" + extraMinutes + " min overdue on rental #" + rental.getId() + ")");
        }

        String finalLocation = (returnLocation != null && !returnLocation.trim().isEmpty())
            ? returnLocation.trim() : "Campus Core";

        rental.setEndTime(returnTime);
        rental.setStatus(RentalStatus.COMPLETED);
        rental.setReturnLocation(finalLocation);
        rental.setTotalCost(rental.getTotalCost() + overdueFine);
        rental.setNotes((rental.getNotes() != null ? rental.getNotes() + " | " : "") + "Returned at " + finalLocation + (returnNotes != null && !returnNotes.isEmpty() ? ": " + returnNotes : ""));

        boolean rentalUpdated = rentalDao.update(rental);
        if (!rentalUpdated) {
            return null;
        }

        // Damage reporting workflow
        if (reportDamage && damageCategory != null) {
            maintenanceService.reportIssue(rental.getCycleId(), rental.getUserId(), damageCategory, damageDetails);
            cycleDao.updateLocationAndStatus(rental.getCycleId(), CycleStatus.MAINTENANCE, finalLocation);
        } else {
            // Restore cycle to AVAILABLE at the return location
            cycleDao.updateLocationAndStatus(rental.getCycleId(), CycleStatus.AVAILABLE, finalLocation);
        }

        // Award rider loyalty points
        Optional<User> userOpt = userDao.findById(rental.getUserId());
        if (userOpt.isPresent() && userOpt.get() instanceof Rider rider) {
            rider.setLoyaltyPoints(rider.getLoyaltyPoints() + 10);
            userDao.update(rider);
        }

        LOGGER.info(String.format("Rental #%d closed. Actual ride: %d min. Overtime fine: $%.2f",
            rental.getId(), actualMinutes, overdueFine));

        return new ReturnReceipt(rental.getId(), rental.getCycleName(), actualMinutes, rental.getTotalCost(), overdueFine, 10);
    }

    public Optional<Rental> getActiveRentalForUser(int userId) {
        return rentalDao.findActiveRentalByUser(userId);
    }

    public List<Rental> getUserRentals(int userId) {
        return rentalDao.findByUser(userId);
    }

    public List<Rental> getAllRentals() {
        return rentalDao.findAll();
    }

    public record ReturnReceipt(int rentalId, String cycleName, long actualMinutes, double totalCharged, double overdueFine, int loyaltyPointsEarned) {}
}
