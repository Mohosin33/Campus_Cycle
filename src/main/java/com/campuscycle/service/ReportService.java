package com.campuscycle.service;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.PaymentDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.model.Cycle;
import com.campuscycle.model.CycleStatus;
import com.campuscycle.model.Payment;
import com.campuscycle.model.Rental;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service demonstrating Multi-Threading, Concurrency, and Thread Pools.
 * Computes analytics and handles asynchronous data export with JavaFX progress tracking.
 */
public class ReportService {
    private final CycleDao cycleDao;
    private final RentalDao rentalDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;

    public ReportService() {
        this.cycleDao = new CycleDao();
        this.rentalDao = new RentalDao();
        this.paymentDao = new PaymentDao();
        this.userDao = new UserDao();
    }

    /**
     * Creates a background Task that runs on the ThreadPool to generate and export
     * an administrative CSV audit log while updating a JavaFX ProgressBar.
     */
    public Task<File> createExportAuditReportTask(File destinationFile) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Starting fleet audit export...");
                updateProgress(0, 100);

                Thread.sleep(300); // Simulate background pipeline steps
                updateMessage("Querying cycle records from SQLite...");
                List<Cycle> cycles = cycleDao.findAll();
                updateProgress(25, 100);

                Thread.sleep(400);
                updateMessage("Retrieving rental logs and payment ledgers...");
                List<Rental> rentals = rentalDao.findAll();
                List<Payment> payments = paymentDao.findAll();
                updateProgress(50, 100);

                Thread.sleep(300);
                updateMessage("Writing CSV data to disk...");
                try (PrintWriter writer = new PrintWriter(new FileWriter(destinationFile))) {
                    writer.println("# CAMPUSCYCLE SYSTEM AUDIT EXPORT");
                    writer.println("# Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    writer.println();
                    writer.println("--- CYCLES INVENTORY ---");
                    writer.println("ID,Model,Brand,Type,HourlyRate,Status,Station,BatteryPercent,TotalRides");
                    for (Cycle c : cycles) {
                        writer.printf("%d,%s,%s,%s,%.2f,%s,%s,%d,%d%n",
                            c.getId(), c.getModel(), c.getBrand(), c.getType().name(),
                            c.getHourlyRate(), c.getStatus().name(), c.getStationName(),
                            c.getBatteryPercentage(), c.getTotalRides());
                    }

                    writer.println();
                    writer.println("--- RENTAL TRANSACTIONS ---");
                    writer.println("RentalID,UserID,UserName,CycleID,CycleName,StartTime,EndTime,Hours,TotalCost,Status");
                    for (Rental r : rentals) {
                        writer.printf("%d,%d,%s,%d,%s,%s,%s,%d,%.2f,%s%n",
                            r.getId(), r.getUserId(), r.getUserName(), r.getCycleId(),
                            r.getCycleName(), r.getStartTime(), r.getEndTime(),
                            r.getDurationHours(), r.getTotalCost(), r.getStatus().name());
                    }

                    writer.println();
                    writer.println("--- PAYMENT RECORDS ---");
                    writer.println("PaymentID,RentalID,Amount,Method,Status,Date,TransactionRef");
                    for (Payment p : payments) {
                        writer.printf("%d,%d,%.2f,%s,%s,%s,%s%n",
                            p.getId(), p.getRentalId(), p.getAmount(),
                            p.getMethod().name(), p.getStatus().name(),
                            p.getTransactionDate(), p.getTransactionReference());
                    }
                }
                updateProgress(85, 100);

                Thread.sleep(200);
                updateProgress(100, 100);
                updateMessage("Export complete: " + destinationFile.getName());

                return destinationFile;
            }
        };
    }

    public DashboardStats getQuickStats() {
        List<Cycle> cycles = cycleDao.findAll();
        List<Rental> rentals = rentalDao.findAll();
        List<Payment> payments = paymentDao.findAll();

        int totalCycles = cycles.size();
        int availableCycles = (int) cycles.stream().filter(c -> c.getStatus() == CycleStatus.AVAILABLE).count();
        int activeRentals = (int) rentals.stream().filter(Rental::isActive).count();
        double totalRevenue = payments.stream().mapToDouble(Payment::getAmount).sum();

        return new DashboardStats(totalCycles, availableCycles, activeRentals, totalRevenue);
    }

    public record DashboardStats(int totalCycles, int availableCycles, int activeRentals, double totalRevenue) {}
}
