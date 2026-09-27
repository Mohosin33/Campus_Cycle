package com.campuscycle;

import com.campuscycle.db.DatabaseManager;
import com.campuscycle.service.ThreadPoolManager;
import com.campuscycle.ui.NavigationManager;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

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

        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b); -fx-padding: 30;");

        Label welcomeLabel = new Label("Welcome to the Campus Cycle");
        welcomeLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: white; -fx-alignment: center;");
        welcomeLabel.setWrapText(true);
        splash.getChildren().add(welcomeLabel);
        splash.setAlignment(Pos.CENTER);

        primaryStage.setTitle("CampusCycle");
        primaryStage.setScene(new Scene(splash, 900, 680));
        primaryStage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.6));
        pause.setOnFinished(event -> nav.showLoginView());
        pause.play();
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
