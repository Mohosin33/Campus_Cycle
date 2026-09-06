# 🚲 CampusCycle - University Campus Cycle Rental System

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java&logoColor=white)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-3.45-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

**CampusCycle** is a modern, responsive JavaFX-based desktop application designed for renting bicycles and e-bikes within a university campus. Students and staff can browse available cycles across campus docking stations, book a cycle with automated role-based discount pricing, view active ride durations, lock & return cycles, and track rental receipts. An administrative operations console provides fleet managers with full CRUD capabilities over cycles and user accounts, system rental audit logs, and multi-threaded background CSV reporting.

---

## 📋 Comprehensive Assignment Criteria Coverage

This project covers all requirements outlined from **Lab 1 to the final assignment**:

| Assignment Criterion | Implementation in CampusCycle | Key Files / Classes |
| :--- | :--- | :--- |
| **1. Version Control** | Initialized Git repository with regular, progressive commits dating back to idea submission (September 6th). | Git commit history, `.gitignore`, `README.md` |
| **2. Advanced OOP Concepts** | • **Abstract Classes**: `User` (abstract base) with polymorphic behavior.<br>• **Subclasses**: `Student`, `Staff`, `Admin`.<br>• **Interfaces**: `Rentable`, `Identifiable`, `PricingStrategy`, `GenericDao<T, ID>`.<br>• **Design Patterns**: Strategy Pattern (dynamic discounts), DAO Pattern, Singleton (`DatabaseManager`, `ThreadPoolManager`, `AuthService`). | `com.campuscycle.model.User`<br>`com.campuscycle.model.Student`<br>`com.campuscycle.model.Staff`<br>`com.campuscycle.model.Admin`<br>`com.campuscycle.service.oop.*` |
| **3. JavaFX UI Design** | Wide variety of layout panes (`BorderPane`, `StackPane`, `GridPane`, `VBox`, `HBox`, `ScrollPane`, `FlowPane`, `TabPane`) and rich controls (`PasswordField`, `TableView`, `TableColumn`, `ComboBox`, `ProgressBar`, `ProgressIndicator`, `Alert`, `Spinner`, `TextArea`, badges). | `com.campuscycle.ui.view.*`<br>`src/main/resources/styles.css` |
| **4. Layout Responsiveness** | Property constraints relative to window width and height: `prefWidthProperty().bind(...)`, `columnResizePolicy`, dynamic FlowPane card reflow on window resize. | `LoginView.java`<br>`StudentDashboardView.java`<br>`AdminDashboardView.java` |
| **5. Concurrency** | Dedicated background `ExecutorService` Thread Pool (`ThreadPoolManager`) executing JavaFX `Task<T>`. Asynchronous weather fetching and multi-step CSV audit report generator with live `ProgressBar` and `ProgressIndicator` updates without freezing the JavaFX application thread. | `com.campuscycle.service.ThreadPoolManager`<br>`com.campuscycle.service.ReportService`<br>`AdminDashboardView.java` |
| **6. Database Integration** | Relational SQLite database (`campuscycle.db`) with `PRAGMA foreign_keys = ON;`, automated schema generation, and relational foreign keys (`users` ↔ `rentals` ↔ `cycles` ↔ `stations`). | `com.campuscycle.db.DatabaseManager` |
| **7. Data Manipulation (CRUD)** | Complete Create, Read, Update, and Delete operations for Cycles and Users, plus rental booking and returns. | `com.campuscycle.dao.*`<br>`AdminDashboardView.java` |
| **8. Networking & Data Parsing** | HTTP GET request using `java.net.http.HttpClient` to the live Open-Meteo public REST API (`https://api.open-meteo.com/v1/forecast?...`). Parses JSON response using `org.json.JSONObject` to display live temperature, wind speed, condition, and rider safety advisory. | `com.campuscycle.service.WeatherService`<br>`WeatherWidget.java` |

---

## 🏛️ System Architecture & Database Schema

### Database Schema (SQLite)
```sql
users (id, username UNIQUE, password, full_name, email, phone, role, specific_id, department, loyalty_points)
stations (id, name, location, capacity)
cycles (id, model, brand, type, hourly_rate, status, station_id FK, battery_percentage, total_rides, last_maintained)
rentals (id, user_id FK, cycle_id FK, start_time, end_time, duration_hours, total_cost, status, start_station_id FK, end_station_id FK, notes)
payments (id, rental_id FK, amount, payment_method, payment_status, transaction_date, transaction_ref UNIQUE)
```

---

## 🚀 How to Run the Application

### Option A: One-Click Launch (Windows)
Double-click `run.bat` or run:
```powershell
.\run.ps1
```

### Option B: Maven Command
```powershell
$env:JAVA_HOME = "E:\Downloads\IntelliJ IDEA 2026.2.1\jbr"
& "E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" clean javafx:run
```

### Option C: Run Automated Verification Suite (Terminal)
To test all criteria (Database, OOP, CRUD, HTTP JSON, Concurrency) automatically:
```powershell
& "E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" exec:java "-Dexec.mainClass=com.campuscycle.VerificationTest"
```

---

## 🔑 Pre-Seeded Demo Accounts

You can instantly test the system using the **Quick Demo Fill** dropdown on the login screen or with these credentials:

| Role | Username | Password | Full Name | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | `student` | `student123` | Mohosin Khan | 25% discount, loyalty points tracking |
| **Student 2** | `sara` | `sara123` | Sara Rahman | Student account with ride history |
| **Staff** | `dr_smith` | `staff123` | Dr. Robert Smith | 15% staff discount rate |
| **Admin** | `admin` | `admin123` | Operations Admin | Full CRUD, Fleet control, Concurrency export |

---

## 📹 Video Demonstration Walkthrough Guide

Ensure you present these exact highlights during your project video recording:

1. **Version Control**: Open terminal or Git GUI to show commits dating from September 6th (`git log --oneline`).
2. **Login & UI Design**:
   - Point out the `BorderPane`, `StackPane`, `GridPane`, and `PasswordField`.
   - Resize the window horizontally and vertically to showcase **Layout Responsiveness** (login card bounds adjust).
3. **Student / Staff Portal**:
   - Demonstrate the **Live Weather Widget** (top right) fetching and parsing JSON over HTTP.
   - Show OOP **Strategy Pattern**: Select a cycle; observe the price calculation showing 25% student discount or 15% staff discount.
   - Reserve a cycle (Create rental & payment in SQLite).
   - Go to "My Active Ride" and perform a cycle return to a chosen dock station.
   - Switch to "Rental History" `TableView` to show the persisted transaction.
4. **Admin Dashboard**:
   - Log in as `admin` / `admin123`.
   - Point out the responsive KPI Stat Cards.
   - Perform **CRUD Operations** on Cycles:
     - **Create**: Click "Add New Cycle" and insert a new bicycle.
     - **Read**: Search and filter by brand/model in the `TableView`.
     - **Update**: Select a cycle, click "Edit Selected", modify hourly rate or status to `MAINTENANCE`.
     - **Delete**: Click "Delete Selected" and confirm deletion.
   - Go to "Concurrency & Audit Export" tab:
     - Click "Start Background Export Task".
     - Show the `ProgressBar` and `ProgressIndicator` advancing on a background `ExecutorService` thread without freezing the UI!
