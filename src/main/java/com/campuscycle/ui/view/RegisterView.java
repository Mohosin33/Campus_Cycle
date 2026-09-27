package com.campuscycle.ui.view;

import com.campuscycle.model.Owner;
import com.campuscycle.model.Rider;
import com.campuscycle.model.User;
import com.campuscycle.model.UserRole;
import com.campuscycle.security.InputValidator;
import com.campuscycle.service.AuthService;
import com.campuscycle.ui.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Production Registration View with strict validation and cryptographic setup.
 * Allows new users to register as Rider or Owner.
 */
public class RegisterView {
    private final AuthService authService;
    private final StackPane root;

    public RegisterView() {
        this.authService = AuthService.getInstance();
        this.root = new StackPane();
        buildUI();
    }

    private void buildUI() {
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        BorderPane layout = new BorderPane();

        // Top Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 28, 16, 28));
        topBar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        Label logoLabel = new Label("🚲 CampusCycle • Account Registration");
        logoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        topBar.getChildren().add(logoLabel);
        layout.setTop(topBar);

        // Center Card
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 36, 30, 36));
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 20, 0, 0, 6);");
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.46));
        card.minWidthProperty().set(400);

        Label title = new Label("Create Your Campus Mobility Account");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("Your official full name");

        TextField userField = new TextField();
        userField.setPromptText("Alphanumeric username (e.g. jdoe)");

        PasswordField passField = new PasswordField();
        passField.setPromptText("At least 6 characters");

        TextField emailField = new TextField();
        emailField.setPromptText("e.g. name@campuscycle.edu");

        TextField phoneField = new TextField();
        phoneField.setPromptText("+880 1700-000000");

        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(UserRole.RIDER, UserRole.OWNER);
        roleCombo.setValue(UserRole.RIDER);

        grid.add(new Label("Full Name:"),    0, 0); grid.add(nameField,  1, 0);
        grid.add(new Label("Username:"),     0, 1); grid.add(userField,  1, 1);
        grid.add(new Label("Password:"),     0, 2); grid.add(passField,  1, 2);
        grid.add(new Label("Campus Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Phone:"),        0, 4); grid.add(phoneField, 1, 4);
        grid.add(new Label("Role:"),         0, 5); grid.add(roleCombo,  1, 5);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setPrefHeight(40);
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String user = userField.getText().trim();
            String pass = passField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String dept = "";

            TextField[] requiredFields = {nameField, userField, passField, emailField, phoneField};
            String defaultStyle = "-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10 8 10;";
            String errorStyle = "-fx-background-color: #fff1f2; -fx-border-color: #ef4444; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10 8 10;";

            boolean hasError = false;
            for (TextField field : requiredFields) {
                if (field.getText() == null || field.getText().trim().isEmpty()) {
                    field.setStyle(errorStyle);
                    hasError = true;
                } else {
                    field.setStyle(defaultStyle);
                }
            }

            if (hasError) {
                statusLabel.setText("Please fill in all required fields before creating your account.");
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                statusLabel.setVisible(true);
                return;
            }

            if (!InputValidator.isValidUsername(user)) {
                statusLabel.setText("Username must be 3-24 characters (letters, numbers, underscore only).");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                userField.setStyle(errorStyle);
                return;
            }

            if (!InputValidator.isStrongPassword(pass)) {
                statusLabel.setText("Password must be at least 6 characters long.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                passField.setStyle(errorStyle);
                return;
            }

            if (!InputValidator.isValidEmail(email)) {
                statusLabel.setText("Please enter a valid email address.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                emailField.setStyle(errorStyle);
                return;
            }

            if (!InputValidator.isValidPhone(phone)) {
                statusLabel.setText("Please enter a valid phone number.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                phoneField.setStyle(errorStyle);
                return;
            }

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            double welcomeCredit = roleCombo.getValue() == UserRole.RIDER ? 20.0 : 0.0;

            User newUser;
            if (roleCombo.getValue() == UserRole.OWNER) {
                newUser = new Owner(0, user, "", "", name, email, phone, "", dept, welcomeCredit, true, now);
            } else {
                newUser = new Rider(0, user, "", "", name, email, phone, "", dept, 0, welcomeCredit, true, now);
            }

            if (authService.register(newUser, pass)) {
                statusLabel.setText("Registration successful! Redirecting to sign-in...");
                statusLabel.setStyle("-fx-text-fill: #10b981;");
                statusLabel.setVisible(true);
                NavigationManager.getInstance().showLoginView();
            } else {
                statusLabel.setText("Username already exists. Please choose a different username.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
            }
        });

        Button backBtn = new Button("Back to Sign In");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setPrefHeight(38);
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> NavigationManager.getInstance().showLoginView());

        card.getChildren().addAll(title, grid, statusLabel, submitBtn, backBtn);

        StackPane centerContainer = new StackPane(card);
        centerContainer.setPadding(new Insets(24));
        centerContainer.setAlignment(Pos.CENTER);
        layout.setCenter(centerContainer);

        root.getChildren().add(layout);
    }

    public Parent getView() { return root; }
}
