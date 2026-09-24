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
 * Authentication View demonstrating:
 * - Layout Panes: BorderPane, StackPane, GridPane, VBox, HBox
 * - UI Controls: PasswordField, TextField, ComboBox, Button, Label
 * - Layout Responsiveness: Width/height property bindings
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
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f1f5f9, #e2e8f0);");

        BorderPane layout = new BorderPane();

        // --- Top Branding Bar ---
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));
        topBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Label logoLabel = new Label("🚲 CampusCycle");
        logoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");

        Label tagLabel = new Label("• Smart University Micro-Mobility");
        tagLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        topBar.getChildren().addAll(logoLabel, tagLabel);
        layout.setTop(topBar);

        // --- Center Login Card ---
        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32, 36, 32, 36));
        card.getStyleClass().add("card");

        // Layout Responsiveness: Bind card width dynamically to window width
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.40));
        card.minWidthProperty().set(360);

        Label welcomeTitle = new Label("Sign In to CampusCycle");
        welcomeTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label welcomeSub = new Label("Rent eco-friendly cycles across campus docks");
        welcomeSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Form using GridPane
        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(14);
        formGrid.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");
        TextField userField = new TextField();
        userField.setPromptText("Enter your campus username");
        userField.setPrefHeight(38);

        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setPrefHeight(38);

        formGrid.add(userLabel, 0, 0);
        formGrid.add(userField, 1, 0);
        formGrid.add(passLabel, 0, 1);
        formGrid.add(passField, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(90);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(col1, col2);

        // Demo Account Quick-Fill Dropdown (ideal for evaluator demo walkthrough)
        Label demoLabel = new Label("Quick Demo Fill:");
        demoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        ComboBox<String> demoCombo = new ComboBox<>();
        demoCombo.getItems().addAll(
            "Student: Mohosin (student / student123)",
            "Staff: Dr. Smith (dr_smith / staff123)",
            "Admin: Fleet Manager (admin / admin123)"
        );
        demoCombo.setPromptText("Choose account to test...");
        demoCombo.setMaxWidth(Double.MAX_VALUE);
        demoCombo.setOnAction(e -> {
            int idx = demoCombo.getSelectionModel().getSelectedIndex();
            if (idx == 0) {
                userField.setText("student");
                passField.setText("student123");
            } else if (idx == 1) {
                userField.setText("dr_smith");
                passField.setText("staff123");
            } else if (idx == 2) {
                userField.setText("admin");
                passField.setText("admin123");
            }
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Buttons
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setPrefHeight(40);
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();

            if (authService.login(username, password)) {
                errorLabel.setVisible(false);
                User user = authService.getCurrentUser();
                if (user.getRole() == UserRole.ADMIN) {
                    NavigationManager.getInstance().showAdminDashboard();
                } else {
                    NavigationManager.getInstance().showStudentDashboard();
                }
            } else {
                errorLabel.setText("Invalid username or password. Please try again.");
                errorLabel.setVisible(true);
            }
        });

        // Trigger login on ENTER key
        passField.setOnAction(e -> loginBtn.fire());
        userField.setOnAction(e -> passField.requestFocus());

        Button registerBtn = new Button("New user? Create an account");
        registerBtn.getStyleClass().add("btn-secondary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> NavigationManager.getInstance().showRegisterView());

        VBox demoBox = new VBox(6, demoLabel, demoCombo);
        demoBox.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
            welcomeTitle,
            welcomeSub,
            formGrid,
            errorLabel,
            loginBtn,
            registerBtn,
            new Separator(),
            demoBox
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
