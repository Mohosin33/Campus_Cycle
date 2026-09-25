package com.campuscycle.ui.view;

import com.campuscycle.model.User;
import com.campuscycle.model.UserRole;
import com.campuscycle.service.AuthService;
import com.campuscycle.ui.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Production Authentication View.
 * Enforces SHA-256 salted password verification, input validation, and account state checks.
 */
public class LoginView {
    private final AuthService authService;
    private final StackPane root;

    public LoginView() {
        this.authService = AuthService.getInstance();
        this.root = new StackPane();
        buildUI();
    }

    private void buildUI() {
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        BorderPane layout = new BorderPane();

        // --- Top Bar ---
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 28, 16, 28));
        topBar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        Label logoLabel = new Label("🚲 CampusCycle");
        logoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Label tagLabel = new Label("• University Enterprise Micro-Mobility Platform");
        tagLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        topBar.getChildren().addAll(logoLabel, tagLabel);
        layout.setTop(topBar);

        // --- Center Card ---
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 20, 0, 0, 6);");

        // Layout Responsiveness
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.38));
        card.minWidthProperty().set(370);

        Label welcomeTitle = new Label("Welcome to CampusCycle");
        welcomeTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label welcomeSub = new Label("Sign in with your university credentials to rent cycles");
        welcomeSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Form using GridPane
        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(16);
        formGrid.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");
        TextField userField = new TextField();
        userField.setPromptText("Campus username");
        userField.setPrefHeight(40);

        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setPrefHeight(40);

        formGrid.add(userLabel, 0, 0);
        formGrid.add(userField, 1, 0);
        formGrid.add(passLabel, 0, 1);
        formGrid.add(passField, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(90);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(col1, col2);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // Sign In Button
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setPrefHeight(42);
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();

            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                errorLabel.setText("Please enter both username and password.");
                errorLabel.setVisible(true);
                return;
            }

            if (authService.login(username, password)) {
                errorLabel.setVisible(false);
                User user = authService.getCurrentUser();
                if (user.getRole() == UserRole.ADMIN) {
                    NavigationManager.getInstance().showAdminDashboard();
                } else {
                    NavigationManager.getInstance().showStudentDashboard();
                }
            } else {
                errorLabel.setText("Invalid credentials or account is suspended.");
                errorLabel.setVisible(true);
            }
        });

        passField.setOnAction(e -> loginBtn.fire());
        userField.setOnAction(e -> passField.requestFocus());

        Button registerBtn = new Button("New Rider? Create Campus Account");
        registerBtn.getStyleClass().add("btn-secondary");
        registerBtn.setPrefHeight(38);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> NavigationManager.getInstance().showRegisterView());

        // Campus Security Badge
        Label securityBadge = new Label("🔒 256-Bit SHA Encrypted Authentication & Secure Docking");
        securityBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(
            welcomeTitle,
            welcomeSub,
            formGrid,
            errorLabel,
            loginBtn,
            registerBtn,
            new Separator(),
            securityBadge
        );

        StackPane centerContainer = new StackPane(card);
        centerContainer.setPadding(new Insets(24));
        centerContainer.setAlignment(Pos.CENTER);
        layout.setCenter(centerContainer);

        root.getChildren().add(layout);
    }

    public Parent getView() {
        return root;
    }
}
