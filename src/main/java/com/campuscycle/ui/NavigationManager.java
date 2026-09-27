package com.campuscycle.ui;

import com.campuscycle.service.AuthService;
import com.campuscycle.ui.view.AdminDashboardView;
import com.campuscycle.ui.view.LoginView;
import com.campuscycle.ui.view.OwnerDashboardView;
import com.campuscycle.ui.view.RegisterView;
import com.campuscycle.ui.view.StudentDashboardView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Navigation Manager controlling Scene transitions and window state.
 */
public class NavigationManager {
    private static NavigationManager instance;
    private Stage primaryStage;

    private NavigationManager() {}

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void showLoginView() {
        LoginView view = new LoginView();
        switchScene(view.getView(), "CampusCycle - University Rental Portal", 900, 680);
    }

    public void showRegisterView() {
        RegisterView view = new RegisterView();
        switchScene(view.getView(), "CampusCycle - Create an Account", 900, 650);
    }

    public void showStudentDashboard() {
        StudentDashboardView view = new StudentDashboardView();
        switchScene(view.getView(), "CampusCycle - Rider Portal", 1100, 750);
    }

    public void showOwnerDashboard() {
        OwnerDashboardView view = new OwnerDashboardView();
        switchScene(view.getView(), "CampusCycle - Owner Portal", 1150, 780);
    }

    public void showAdminDashboard() {
        AdminDashboardView view = new AdminDashboardView();
        switchScene(view.getView(), "CampusCycle - Campus Administration", 1200, 800);
    }

    private void switchScene(Parent root, String title, double width, double height) {
        if (primaryStage == null) return;

        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) {
            Scene scene = new Scene(root, width, height);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } else {
            currentScene.setRoot(root);
            applyStyles(currentScene);
            if (primaryStage.getWidth() < width) primaryStage.setWidth(width);
            if (primaryStage.getHeight() < height) primaryStage.setHeight(height);
        }

        primaryStage.setTitle(title);
        primaryStage.show();
    }

    private void applyStyles(Scene scene) {
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            String cssPath = cssResource.toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        }
    }
}
