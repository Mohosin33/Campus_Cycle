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
        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(34, 38, 34, 38));
        card.getStyleClass().add("card");
        card.setStyle("-fx-background-color: rgba(255,255,255,0.98); -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.22), 22, 0, 0, 10); -fx-border-color: rgba(148,163,184,0.4); -fx-border-width: 1; -fx-border-radius: 18;");

        card.maxWidthProperty().bind(root.widthProperty().multiply(0.38));
        card.minWidthProperty().set(380);

        Label welcomeTitle = new Label("Welcome to Campus Cycle");
        welcomeTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label welcomeSub = new Label("Sign in to rent, return, and manage your campus rides");
        welcomeSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 0 0 8 0;");

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
                switch (user.getRole()) {
                    case ADMIN -> NavigationManager.getInstance().showAdminDashboard();
                    case OWNER -> NavigationManager.getInstance().showOwnerDashboard();
                    case RIDER -> NavigationManager.getInstance().showStudentDashboard();
                }
            } else {
                errorLabel.setText("Invalid credentials or account is suspended.");
                errorLabel.setVisible(true);
            }
        });

        passField.setOnAction(e -> loginBtn.fire());
        userField.setOnAction(e -> passField.requestFocus());

        Button registerBtn = new Button("New in the Campus Cycle? Create an account");
        registerBtn.getStyleClass().add("btn-secondary");
        registerBtn.setPrefHeight(38);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> NavigationManager.getInstance().showRegisterView());

        Label securityBadge = new Label("🔒 Secure campus access with verified rider and owner accounts");
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
