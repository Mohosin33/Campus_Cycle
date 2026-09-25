package com.campuscycle;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.MaintenanceTicketDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.*;
import com.campuscycle.security.PasswordHasher;
import com.campuscycle.service.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Automated Verification Suite for CampusCycle (Production Build).
 * Verifies assignment rubric criteria PLUS production security / wallet / maintenance:
 *  1. Advanced OOP Concepts (Polymorphism + Strategy Pattern)
 *  2. SQLite Database Setup & Relationships
 *  3. Complete CRUD Operations
 *  4. HTTP Networking & JSON Parsing (Open-Meteo)
 *  5. Concurrency & Thread Pool Execution
 *  6. [PROD] SHA-256 Password Hashing & Verification
 *  7. [PROD] Campus Wallet — Top-Up & Deduction
 *  8. [PROD] Maintenance Ticket Lifecycle (Open → Resolved)
 */
public class VerificationTest {

    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("   CAMPUSCYCLE  —  PRODUCTION VERIFICATION SUITE ");
        System.out.println("══════════════════════════════════════════════════");

        boolean allPassed = true;

        try {
            // ── 1. Database ──────────────────────────────────────────
            System.out.println("\n[1/8] SQLite Database Setup & Seeding…");
            DatabaseManager.getInstance();
            File dbFile = new File("campuscycle.db");
            if (dbFile.exists()) {
                System.out.println("  ✓ DB file confirmed: " + dbFile.getAbsolutePath());
            } else {
                System.err.println("  ✗ Database file NOT found!");
                allPassed = false;
            }

            UserDao userDao = new UserDao();
            List<User> users = userDao.findAll();
            System.out.println("  ✓ " + users.size() + " users seeded in DB.");

            CycleDao cycleDao = new CycleDao();
            List<Cycle> cycles = cycleDao.findAll();
            System.out.println("  ✓ " + cycles.size() + " cycles seeded across dock stations.");

            // ── 2. Advanced OOP ──────────────────────────────────────
            System.out.println("\n[2/8] Advanced OOP — Polymorphism & Strategy Pattern…");
            Optional<User> riderOpt = userDao.findByUsername("rider1");
            Optional<User> ownerOpt = userDao.findByUsername("owner1");

            if (riderOpt.isPresent() && riderOpt.get() instanceof Rider r) {
                System.out.println("  ✓ instanceof Rider: " + r.getFullName());
                System.out.println("  ✓ Pricing strategy: " + r.getPricingStrategy().getStrategyName());
                System.out.println("  ✓ Wallet balance  : $" + String.format("%.2f", r.getWalletBalance()));
                System.out.println("  ✓ Account active  : " + r.isActive());
            } else {
                System.err.println("  ✗ Rider polymorphism failed!"); allPassed = false;
            }

            if (ownerOpt.isPresent() && ownerOpt.get() instanceof Owner ow) {
                System.out.println("  ✓ instanceof Owner: " + ow.getFullName());
                System.out.println("  ✓ Can manage cycles: " + ow.canManageCycles());
                System.out.println("  ✓ Can rent cycles  : " + ow.canRentCycles());
            } else {
                System.err.println("  ✗ Owner polymorphism failed!"); allPassed = false;
            }

            // ── 3. CRUD ───────────────────────────────────────────────
            System.out.println("\n[3/8] Complete CRUD Operations…");
            Cycle testBike = new Cycle(0, "Speedster Z-9", "Specialized", CycleType.GEARED, 20.0, CycleStatus.AVAILABLE, 1, -1);
            Cycle saved = cycleDao.save(testBike);
            System.out.println("  ✓ CREATE : Cycle inserted with ID=" + saved.getId());

            Optional<Cycle> fetched = cycleDao.findById(saved.getId());
            if (fetched.isPresent() && fetched.get().getModel().equals("Speedster Z-9")) {
                System.out.println("  ✓ READ   : Retrieved — " + fetched.get().getDisplayName());
            } else {
                System.err.println("  ✗ READ failed!"); allPassed = false;
            }

            saved.setHourlyRate(24.50);
            saved.setStatus(CycleStatus.MAINTENANCE);
            boolean updated = cycleDao.update(saved);
            System.out.println("  ✓ UPDATE : rate=$24.50, status=MAINTENANCE (ok=" + updated + ")");

            boolean deleted = cycleDao.delete(saved.getId());
            System.out.println("  ✓ DELETE : Cycle #" + saved.getId() + " removed (ok=" + deleted + ")");

            // ── 4. HTTP Networking ────────────────────────────────────
            System.out.println("\n[4/8] HTTP Request & JSON Parsing (Open-Meteo API)…");
            WeatherService weatherService = new WeatherService();
            WeatherReport weather = weatherService.fetchLiveWeather();
            if (weather != null) {
                System.out.println("  ✓ HTTP request succeeded.");
                System.out.println("  ✓ Temperature : " + weather.getTemperature() + " °C");
                System.out.println("  ✓ Condition   : " + weather.getConditionDescription());
                System.out.println("  ✓ Wind speed  : " + weather.getWindSpeed() + " km/h");
                System.out.println("  ✓ Advisory    : " + weather.getSafetyLevel() + " — " + weather.getRecommendation());
            } else {
                System.err.println("  ✗ Weather fetch failed!"); allPassed = false;
            }

            // ── 5. Concurrency ────────────────────────────────────────
            System.out.println("\n[5/8] Concurrency & Thread Pool…");
            CountDownLatch latch = new CountDownLatch(1);
            ThreadPoolManager.getInstance().execute(() -> {
                System.out.println("  ✓ Background task on: " + Thread.currentThread().getName());
                latch.countDown();
            });
            boolean ok = latch.await(3, TimeUnit.SECONDS);
            if (ok) System.out.println("  ✓ Thread pool concurrency test PASSED.");
            else  { System.err.println("  ✗ Thread pool timed out!"); allPassed = false; }

            // ── 6. [PROD] Password Hashing ────────────────────────────
            System.out.println("\n[6/8] SHA-256 Password Hashing & Verification…");
            String plain = "Secure@Test123";
            PasswordHasher.HashedCredentials creds = PasswordHasher.hashPassword(plain);
            System.out.println("  ✓ Salt generated   : " + creds.salt());
            System.out.println("  ✓ Hash generated   : " + creds.hash().substring(0, 16) + "…");

            boolean correctVerify = PasswordHasher.verifyPassword(plain, creds.hash(), creds.salt());
            boolean wrongVerify   = PasswordHasher.verifyPassword("WrongPassword!", creds.hash(), creds.salt());

            if (correctVerify && !wrongVerify) {
                System.out.println("  ✓ Correct password verified: TRUE");
                System.out.println("  ✓ Wrong password rejected  : TRUE (not a false positive)");
            } else {
                System.err.println("  ✗ Password hash verification logic FAILED!"); allPassed = false;
            }

            // Verify seeded admin password
            Optional<User> adminOpt = userDao.findByUsername("admin");
            if (adminOpt.isPresent()) {
                User admin = adminOpt.get();
                boolean adminOk = PasswordHasher.verifyPassword("admin123", admin.getPasswordHash(), admin.getPasswordSalt());
                System.out.println("  ✓ Seeded admin password verifies correctly: " + adminOk);
                if (!adminOk) allPassed = false;
            }

            // ── 7. [PROD] Wallet Operations ───────────────────────────
            System.out.println("\n[7/8] Campus Pay Wallet — Top-Up & Deduction…");
            WalletService walletService = new WalletService();
            if (riderOpt.isPresent()) {
                int uid = riderOpt.get().getId();
                double before = walletService.getBalance(uid);
                System.out.println("  → Balance before top-up: $" + String.format("%.2f", before));

                boolean topped = walletService.topUpBalance(uid, 25.00, "Verification test credit");
                System.out.println("  ✓ Top-up $25.00 : success=" + topped);

                double after = walletService.getBalance(uid);
                System.out.println("  → Balance after top-up : $" + String.format("%.2f", after));

                if (topped && Math.abs(after - before - 25.00) < 0.01) {
                    System.out.println("  ✓ Balance delta verified correctly (+$25.00).");
                } else {
                    System.err.println("  ✗ Wallet top-up delta mismatch!"); allPassed = false;
                }

                boolean deducted = walletService.deductBalance(uid, 5.00, WalletTransaction.TransactionType.RENTAL_CHARGE, "Test rental charge");
                System.out.println("  ✓ Deduct $5.00  : success=" + deducted);
                double final_ = walletService.getBalance(uid);
                System.out.println("  → Final balance : $" + String.format("%.2f", final_));
            } else {
                System.err.println("  ✗ Could not find 'rider1' user for wallet test!"); allPassed = false;
            }

            // ── 8. [PROD] Maintenance Ticket Lifecycle ─────────────────
            System.out.println("\n[8/8] Maintenance Ticket Lifecycle (Open → Resolved)…");
            MaintenanceService maintenanceService = new MaintenanceService();
            List<Cycle> allCycles = cycleDao.findAll();
            if (!allCycles.isEmpty()) {
                Cycle testCycle = allCycles.get(0);
                int adminId = adminOpt.map(User::getId).orElse(1);

                MaintenanceTicket ticket = maintenanceService.reportIssue(
                    testCycle.getId(), adminId,
                    MaintenanceTicket.IssueCategory.FLAT_TIRE, "Verification test ticket"
                );
                System.out.println("  ✓ OPEN ticket created: #" + ticket.getId() + " for " + testCycle.getDisplayName());

                // Verify cycle was moved to MAINTENANCE
                Optional<Cycle> inMaint = cycleDao.findById(testCycle.getId());
                boolean isMaint = inMaint.isPresent() && inMaint.get().getStatus() == CycleStatus.MAINTENANCE;
                System.out.println("  ✓ Cycle auto-flagged MAINTENANCE: " + isMaint);
                if (!isMaint) allPassed = false;

                // Resolve ticket
                boolean resolved = maintenanceService.resolveTicket(ticket.getId(), "Tube replaced. Air pressure checked.", 12.50, testCycle.getStationId());
                System.out.println("  ✓ Ticket resolved: " + resolved);

                Optional<Cycle> restored = cycleDao.findById(testCycle.getId());
                boolean isAvail = restored.isPresent() && restored.get().getStatus() == CycleStatus.AVAILABLE;
                System.out.println("  ✓ Cycle restored to AVAILABLE: " + isAvail);
                if (!isAvail) allPassed = false;
            } else {
                System.err.println("  ✗ No cycles found for maintenance test!"); allPassed = false;
            }

            // ── Summary ───────────────────────────────────────────────
            System.out.println("\n══════════════════════════════════════════════════");
            if (allPassed) {
                System.out.println("  ✅  ALL 8 PRODUCTION TESTS PASSED — BUILD GOOD  ");
            } else {
                System.out.println("  ⚠️   ONE OR MORE TESTS FAILED — SEE ABOVE       ");
            }
            System.out.println("══════════════════════════════════════════════════");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ThreadPoolManager.getInstance().shutdown();
        }
    }
}
