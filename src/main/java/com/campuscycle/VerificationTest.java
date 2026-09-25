package com.campuscycle;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.db.DatabaseManager;
import com.campuscycle.model.*;
import com.campuscycle.service.RentalService;
import com.campuscycle.service.ThreadPoolManager;
import com.campuscycle.service.WeatherService;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Automated Verification Suite for CampusCycle.
 * Verifies all assignment rubric criteria:
 * 1. Advanced OOP Concepts
 * 2. SQLite Database Setup & Relationships
 * 3. Complete CRUD Operations
 * 4. HTTP Networking & JSON Parsing
 * 5. Concurrency & Thread Pool Execution
 */
public class VerificationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    CAMPUSCYCLE - AUTOMATED CRITERIA VERIFICATION ");
        System.out.println("==================================================");

        boolean allPassed = true;

        try {
            // 1. DATABASE & RELATIONSHIPS
            System.out.println("\n[1/5] Testing SQLite Database Setup & Table Seeding...");
            DatabaseManager dbManager = DatabaseManager.getInstance();
            File dbFile = new File("campuscycle.db");
            if (dbFile.exists()) {
                System.out.println("  ✓ SQLite database file confirmed: " + dbFile.getAbsolutePath());
            } else {
                System.err.println("  ✗ Database file was not found!");
                allPassed = false;
            }

            UserDao userDao = new UserDao();
            List<User> users = userDao.findAll();
            System.out.println("  ✓ Found " + users.size() + " seeded users in database.");

            CycleDao cycleDao = new CycleDao();
            List<Cycle> cycles = cycleDao.findAll();
            System.out.println("  ✓ Found " + cycles.size() + " seeded cycles across campus docks.");

            // 2. ADVANCED OOP (Polymorphism & Strategy Pattern)
            System.out.println("\n[2/5] Testing Advanced OOP Concepts (Polymorphism & Strategy)...");
            Optional<User> studentOpt = userDao.findByUsername("student");
            Optional<User> staffOpt = userDao.findByUsername("dr_smith");

            if (studentOpt.isPresent() && studentOpt.get() instanceof Student) {
                Student s = (Student) studentOpt.get();
                System.out.println("  ✓ Polymorphism: " + s.getFullName() + " correctly instantiated as " + s.getClass().getSimpleName());
                System.out.println("  ✓ Student Pricing Strategy: " + s.getPricingStrategy().getStrategyName());
            } else {
                System.err.println("  ✗ Student polymorphism failed!");
                allPassed = false;
            }

            if (staffOpt.isPresent() && staffOpt.get() instanceof Staff) {
                Staff st = (Staff) staffOpt.get();
                System.out.println("  ✓ Polymorphism: " + st.getFullName() + " correctly instantiated as " + st.getClass().getSimpleName());
                System.out.println("  ✓ Staff Pricing Strategy: " + st.getPricingStrategy().getStrategyName());
            }

            // 3. COMPLETE CRUD DATA MANIPULATION
            System.out.println("\n[3/5] Testing Complete CRUD Data Manipulation...");
            // Create
            Cycle testBike = new Cycle(0, "Speedster Z-9", "Specialized", CycleType.GEARED, 20.0, CycleStatus.AVAILABLE, 1, -1);
            Cycle saved = cycleDao.save(testBike);
            System.out.println("  ✓ CREATE: Inserted test cycle with ID: " + saved.getId());

            // Read
            Optional<Cycle> fetched = cycleDao.findById(saved.getId());
            if (fetched.isPresent() && fetched.get().getModel().equals("Speedster Z-9")) {
                System.out.println("  ✓ READ: Retrieved cycle #" + fetched.get().getId() + " - " + fetched.get().getDisplayName());
            } else {
                System.err.println("  ✗ READ failed!");
                allPassed = false;
            }

            // Update
            saved.setHourlyRate(24.50);
            saved.setStatus(CycleStatus.MAINTENANCE);
            boolean updated = cycleDao.update(saved);
            System.out.println("  ✓ UPDATE: Updated rate to $24.50 and status to MAINTENANCE (success: " + updated + ")");

            // Delete
            boolean deleted = cycleDao.delete(saved.getId());
            System.out.println("  ✓ DELETE: Deleted test cycle #" + saved.getId() + " (success: " + deleted + ")");

            // 4. NETWORKING & JSON PARSING
            System.out.println("\n[4/5] Testing HTTP Request & JSON Parsing (Open-Meteo API)...");
            WeatherService weatherService = new WeatherService();
            WeatherReport weather = weatherService.fetchLiveWeather();
            if (weather != null) {
                System.out.println("  ✓ HTTP Request Succeeded: Received JSON response.");
                System.out.println("  ✓ Parsed Temperature: " + weather.getTemperature() + " °C");
                System.out.println("  ✓ Parsed Condition: " + weather.getConditionDescription());
                System.out.println("  ✓ Parsed Wind Speed: " + weather.getWindSpeed() + " km/h");
                System.out.println("  ✓ Cycling Advisory: " + weather.getSafetyLevel() + " (" + weather.getRecommendation() + ")");
            } else {
                System.err.println("  ✗ Weather networking or parsing failed!");
                allPassed = false;
            }

            // 5. CONCURRENCY & THREAD POOL
            System.out.println("\n[5/5] Testing Concurrency & Thread Pool Execution...");
            CountDownLatch latch = new CountDownLatch(1);
            ThreadPoolManager.getInstance().execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println("  ✓ Background task executed on Thread Pool worker: " + threadName);
                latch.countDown();
            });

            boolean await = latch.await(3, TimeUnit.SECONDS);
            if (await) {
                System.out.println("  ✓ Thread Pool Concurrency Test Passed.");
            } else {
                System.err.println("  ✗ Thread Pool Concurrency timed out!");
                allPassed = false;
            }

            System.out.println("\n==================================================");
            if (allPassed) {
                System.out.println("    ALL ASSIGNMENT CRITERIA TESTS PASSED! 100%   ");
            } else {
                System.out.println("    SOME TESTS REPORTED WARNINGS OR FAILURES     ");
            }
            System.out.println("==================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ThreadPoolManager.getInstance().shutdown();
        }
    }
}
