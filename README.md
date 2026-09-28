# CampusCycle

CampusCycle is a JavaFX desktop application for managing campus bicycle rentals. It includes role-based dashboards, bicycle listings, rentals, wallet and payment records, maintenance tickets, and live weather information.

## Features

- Admin, owner, and rider user roles
- Browse and manage bicycle listings
- Create, view, update, and delete bicycle records
- Rental, wallet, payment, and maintenance-ticket management
- SQLite database with foreign-key relationships
- Background thread pool for tasks such as weather fetching and report generation
- Live weather fetched from Open-Meteo, with fallback data when the network is unavailable
- Password hashing and verification

## Technologies

- Java 17
- JavaFX 21
- SQLite with SQLite JDBC
- `org.json` for parsing weather API responses
- Maven

## Requirements

- JDK 17 or later
- Maven
- Internet connection for live weather data

## Run the Application

Run one of these commands from the project root:

**Windows Command Prompt:**

```bat
run.bat
