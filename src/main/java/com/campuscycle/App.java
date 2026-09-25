package com.campuscycle;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.service.ThreadPoolManager;
import com.campuscycle.ui.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * CampusCycle - Desktop Application Entry Point.
 * JavaFX LifeCycle management.
 */
public class App extends Application {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void init() throws Exception {
        LOGGER.info("Starting CampusCycle Application initialization...");
        // Initialize SQLite Database and seed tables
        DatabaseManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("Launching CampusCycle JavaFX UI Stage...");
        NavigationManager nav = NavigationManager.getInstance();
        nav.setPrimaryStage(primaryStage);
        nav.showLoginView();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Shutting down CampusCycle thread pools and resources...");
        ThreadPoolManager.getInstance().shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
