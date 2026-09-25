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

        Label perkLabel = new Label("✨ Riders receive $20.00 Welcome Campus Pay Credit on sign-up");
        perkLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #16a34a; -fx-font-weight: 600;");

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

        TextField idField = new TextField();
        idField.setPromptText("Rider ID (e.g. RDR-101)");

        TextField deptField = new TextField();
        deptField.setPromptText("Department / Area of Operations");

        grid.add(new Label("Full Name:"),    0, 0); grid.add(nameField,  1, 0);
        grid.add(new Label("Username:"),     0, 1); grid.add(userField,  1, 1);
        grid.add(new Label("Password:"),     0, 2); grid.add(passField,  1, 2);
        grid.add(new Label("Campus Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Phone:"),        0, 4); grid.add(phoneField, 1, 4);
        grid.add(new Label("Role:"),         0, 5); grid.add(roleCombo,  1, 5);

        Label specificLabel = new Label("Rider ID:");
        grid.add(specificLabel, 0, 6);
        grid.add(idField, 1, 6);

        grid.add(new Label("Department:"), 0, 7); grid.add(deptField, 1, 7);

        roleCombo.setOnAction(e -> {
            if (roleCombo.getValue() == UserRole.OWNER) {
                specificLabel.setText("Owner ID:");
                idField.setPromptText("e.g. OWN-201 or NID");
                perkLabel.setText("🏪 As an Owner you can list your cycles and earn rental income");
                perkLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c3aed; -fx-font-weight: 600;");
            } else {
                specificLabel.setText("Rider ID:");
                idField.setPromptText("Rider ID (e.g. RDR-101)");
                perkLabel.setText("✨ Riders receive $20.00 Welcome Campus Pay Credit on sign-up");
                perkLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #16a34a; -fx-font-weight: 600;");
            }
        });

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setPrefHeight(40);
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        submitBtn.setOnAction(e -> {
            String name   = nameField.getText().trim();
            String user   = userField.getText().trim();
            String pass   = passField.getText().trim();
            String email  = emailField.getText().trim();
            String phone  = phoneField.getText().trim();
            String specId = idField.getText().trim();
            String dept   = deptField.getText().trim();

            if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Name, username, and password are required.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                return;
            }

            if (!InputValidator.isValidUsername(user)) {
                statusLabel.setText("Username must be 3-24 characters (letters, numbers, underscore only).");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                return;
            }

            if (!InputValidator.isStrongPassword(pass)) {
                statusLabel.setText("Password must be at least 6 characters long.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                return;
            }

            if (!email.isEmpty() && !InputValidator.isValidEmail(email)) {
                statusLabel.setText("Please enter a valid email address.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                return;
            }

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            double welcomeCredit = roleCombo.getValue() == UserRole.RIDER ? 20.0 : 0.0;

            User newUser;
            if (roleCombo.getValue() == UserRole.OWNER) {
                newUser = new Owner(0, user, "", "", name, email, phone, specId, dept, welcomeCredit, true, now);
            } else {
                newUser = new Rider(0, user, "", "", name, email, phone, specId, dept, 0, welcomeCredit, true, now);
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

        card.getChildren().addAll(title, perkLabel, grid, statusLabel, submitBtn, backBtn);

        StackPane centerContainer = new StackPane(card);
        centerContainer.setPadding(new Insets(24));
        centerContainer.setAlignment(Pos.CENTER);
        layout.setCenter(centerContainer);

        root.getChildren().add(layout);
    }

    public Parent getView() { return root; }
}
