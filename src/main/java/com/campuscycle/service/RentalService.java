package com.campuscycle.service;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.PaymentDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service managing cycle rental operations, return procedures, and billing.
 */
public class RentalService {
    private static final Logger LOGGER = Logger.getLogger(RentalService.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RentalDao rentalDao;
    private final CycleDao cycleDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;

    public RentalService() {
        this.rentalDao = new RentalDao();
        this.cycleDao = new CycleDao();
        this.paymentDao = new PaymentDao();
        this.userDao = new UserDao();
    }

    /**
     * Rents a cycle for a user using their polymorphic pricing strategy.
     */
    public Rental rentCycle(User user, Cycle cycle, int durationHours, PaymentMethod paymentMethod, String notes) throws Exception {
        if (user == null || cycle == null) {
            throw new IllegalArgumentException("User and Cycle must not be null.");
        }

        // Check if user already has an active rental
        Optional<Rental> activeRental = rentalDao.findActiveRentalByUser(user.getId());
        if (activeRental.isPresent()) {
            throw new IllegalStateException("You already have an active cycle rental (" + activeRental.get().getCycleName() + "). Please return it before booking another.");
        }

        // Check cycle availability
        if (!cycle.isAvailable()) {
            throw new IllegalStateException("Cycle is currently not available for rent.");
        }

        // Calculate dynamic cost based on user's OOP PricingStrategy
        double totalCost = cycle.calculateCost(durationHours, user.getPricingStrategy());

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
            cycle.getStationId(),
            0,
            notes != null ? notes : "Campus trip"
        );

        Rental savedRental = rentalDao.save(rental);
        if (savedRental == null) {
            throw new RuntimeException("Failed to create rental record in database.");
        }

        // Update cycle status in database
        cycle.rentOut(user, durationHours);
        cycleDao.updateStatus(cycle.getId(), CycleStatus.RENTED, cycle.getStationId());

        // Create Payment record
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

        LOGGER.info(String.format("Rental #%d created successfully for user %s on cycle #%d. Total: $%.2f",
            savedRental.getId(), user.getUsername(), cycle.getId(), totalCost));

        return savedRental;
    }

    /**
     * Returns an active rental to a chosen dock station.
     */
    public boolean returnCycle(Rental rental, int returnStationId, String returnNotes) {
        if (rental == null || !rental.isActive()) {
            return false;
        }

        String returnTime = LocalDateTime.now().format(TIME_FORMATTER);
        rental.setEndTime(returnTime);
        rental.setStatus(RentalStatus.COMPLETED);
        rental.setEndStationId(returnStationId);
        rental.setNotes((rental.getNotes() != null ? rental.getNotes() + " | " : "") + "Returned: " + returnNotes);

        boolean rentalUpdated = rentalDao.update(rental);
        if (!rentalUpdated) {
            return false;
        }

        // Update cycle to AVAILABLE at new station
        cycleDao.updateStatus(rental.getCycleId(), CycleStatus.AVAILABLE, returnStationId);

        // Award student loyalty points
        Optional<User> userOpt = userDao.findById(rental.getUserId());
        if (userOpt.isPresent() && userOpt.get() instanceof Student) {
            Student student = (Student) userOpt.get();
            student.setLoyaltyPoints(student.getLoyaltyPoints() + 10);
            userDao.update(student);
        }

        LOGGER.info("Rental #" + rental.getId() + " returned successfully to station #" + returnStationId);
        return true;
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
}
