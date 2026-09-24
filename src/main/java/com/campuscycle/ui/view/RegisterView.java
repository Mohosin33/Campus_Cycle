package com.campuscycle.ui.view;

import com.campuscycle.model.Staff;
import com.campuscycle.model.Student;
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
 * Registration View for new students and staff members.
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
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f1f5f9, #e2e8f0);");

        BorderPane layout = new BorderPane();

        // Top Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));
        topBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        Label logoLabel = new Label("🚲 CampusCycle - User Registration");
        logoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        topBar.getChildren().add(logoLabel);
        layout.setTop(topBar);

        // Center Card
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.getStyleClass().add("card");
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.48));
        card.minWidthProperty().set(380);

        Label title = new Label("Create Campus Account");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField nameField = new TextField();
        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(UserRole.STUDENT, UserRole.STAFF);
        roleCombo.setValue(UserRole.STUDENT);

        TextField idField = new TextField();
        idField.setPromptText("e.g. 2023-1-60-101");
        TextField deptField = new TextField();
        deptField.setPromptText("e.g. Computer Science");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Username:"), 0, 1);
        grid.add(userField, 1, 1);

        grid.add(new Label("Password:"), 0, 2);
        grid.add(passField, 1, 2);

        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);

        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);

        grid.add(new Label("Account Role:"), 0, 5);
        grid.add(roleCombo, 1, 5);

        Label specificLabel = new Label("Student ID:");
        grid.add(specificLabel, 0, 6);
        grid.add(idField, 1, 6);

        grid.add(new Label("Department:"), 0, 7);
        grid.add(deptField, 1, 7);

        roleCombo.setOnAction(e -> {
            if (roleCombo.getValue() == UserRole.STAFF) {
                specificLabel.setText("Employee ID:");
                idField.setPromptText("e.g. EMP-402");
            } else {
                specificLabel.setText("Student ID:");
                idField.setPromptText("e.g. 2023-1-60-101");
            }
        });

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-weight: bold;");
        statusLabel.setVisible(false);

        Button submitBtn = new Button("Register Account");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String user = userField.getText().trim();
            String pass = passField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String specId = idField.getText().trim();
            String dept = deptField.getText().trim();

            if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Please fill in all required fields.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
                return;
            }

            User newUser;
            if (roleCombo.getValue() == UserRole.STAFF) {
                newUser = new Staff(0, user, pass, name, email, phone, specId, dept);
            } else {
                newUser = new Student(0, user, pass, name, email, phone, specId, dept, 15);
            }

            if (authService.register(newUser)) {
                statusLabel.setText("Registration successful! Redirecting to login...");
                statusLabel.setStyle("-fx-text-fill: #10b981;");
                statusLabel.setVisible(true);
                NavigationManager.getInstance().showLoginView();
            } else {
                statusLabel.setText("Registration failed: Username already exists.");
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setVisible(true);
            }
        });

        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> NavigationManager.getInstance().showLoginView());

        card.getChildren().addAll(title, grid, statusLabel, submitBtn, backBtn);

        ScrollPane scroll = new ScrollPane(new StackPane(card));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        layout.setCenter(scroll);

        root.getChildren().add(layout);
    }

    public Parent getView() {
        return root;
    }
}
