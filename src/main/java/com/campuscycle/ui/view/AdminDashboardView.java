package com.campuscycle.ui.view;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.StationDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.model.*;
import com.campuscycle.service.AuthService;
import com.campuscycle.service.ReportService;
import com.campuscycle.service.ThreadPoolManager;
import com.campuscycle.ui.NavigationManager;
import com.campuscycle.ui.view.components.StatCard;
import com.campuscycle.ui.view.components.WeatherWidget;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Administrative Control Console.
 * Demonstrates:
 * - Data Manipulation: Complete CRUD operations (Create, Read, Update, Delete) on Cycles and Users
 * - Concurrency: Thread Pools and JavaFX Task with ProgressBar for background CSV report export
 * - Layout Responsiveness: Bindings relative to window height/width
 * - Layout Panes: BorderPane, StackPane, GridPane, TabPane, VBox, HBox
 * - UI Controls: TableView, TableColumn, ComboBox, ProgressBar, ProgressIndicator, Dialogs
 */
public class AdminDashboardView {
    private final AuthService authService;
    private final CycleDao cycleDao;
    private final UserDao userDao;
    private final RentalDao rentalDao;
    private final StationDao stationDao;
    private final ReportService reportService;

    private final StackPane rootStack;
    private final BorderPane mainLayout;

    // Stat Cards
    private StatCard totalFleetCard;
    private StatCard availableCard;
    private StatCard activeRentalsCard;
    private StatCard revenueCard;

    // Tables
    private final TableView<Cycle> cycleTable;
    private final TableView<User> userTable;
    private final TableView<Rental> rentalTable;

    public AdminDashboardView() {
        this.authService = AuthService.getInstance();
        this.cycleDao = new CycleDao();
        this.userDao = new UserDao();
        this.rentalDao = new RentalDao();
        this.stationDao = new StationDao();
        this.reportService = new ReportService();

        this.rootStack = new StackPane();
        this.mainLayout = new BorderPane();
        this.cycleTable = new TableView<>();
        this.userTable = new TableView<>();
        this.rentalTable = new TableView<>();

        buildUI();
        refreshAllData();
    }

    private void buildUI() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            Platform.runLater(() -> NavigationManager.getInstance().showLoginView());
            return;
        }

        // ================= TOP HEADER =================
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("🚲 CampusCycle • Operations Command");
        brandTitle.getStyleClass().add("brand-title");

        Label brandSub = new Label("Administrator: " + currentUser.getFullName() + " (" + currentUser.getEmail() + ")");
        brandSub.getStyleClass().add("brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        HBox rightHeaderBox = new HBox(14);
        rightHeaderBox.setAlignment(Pos.CENTER_RIGHT);

        WeatherWidget weatherWidget = new WeatherWidget();

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            NavigationManager.getInstance().showLoginView();
        });

        rightHeaderBox.getChildren().addAll(weatherWidget, logoutBtn);
        header.setLeft(brandBox);
        header.setRight(rightHeaderBox);
        mainLayout.setTop(header);

        // ================= CENTER CONTENT =================
        VBox centerBox = new VBox(16);
        centerBox.setPadding(new Insets(16, 20, 16, 20));

        // --- KPI Stat Cards Bar ---
        HBox statsBar = new HBox(16);
        statsBar.setAlignment(Pos.CENTER_LEFT);

        totalFleetCard = new StatCard("Total Cycles", "0", "#2563eb");
        availableCard = new StatCard("Available for Rent", "0", "#10b981");
        activeRentalsCard = new StatCard("Active Rides Now", "0", "#f59e0b");
        revenueCard = new StatCard("Total Revenue", "$0.00", "#7c3aed");

        // Layout Responsiveness: Bind stat card widths proportionally to window width
        totalFleetCard.prefWidthProperty().bind(mainLayout.widthProperty().divide(4).subtract(24));
        availableCard.prefWidthProperty().bind(mainLayout.widthProperty().divide(4).subtract(24));
        activeRentalsCard.prefWidthProperty().bind(mainLayout.widthProperty().divide(4).subtract(24));
        revenueCard.prefWidthProperty().bind(mainLayout.widthProperty().divide(4).subtract(24));

        statsBar.getChildren().addAll(totalFleetCard, availableCard, activeRentalsCard, revenueCard);
        centerBox.getChildren().add(statsBar);

        // --- TabPane for Operations ---
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.prefHeightProperty().bind(mainLayout.heightProperty().multiply(0.68));

        Tab cyclesTab = new Tab("🚲 Fleet Management (CRUD)", buildCyclesCrudTab());
        Tab usersTab = new Tab("👥 User Accounts (CRUD)", buildUsersCrudTab());
        Tab rentalsTab = new Tab("📋 Campus Rental Ledger", buildRentalsTab());
        Tab concurrencyTab = new Tab("⚡ Concurrency & Audit Export", buildConcurrencyExportTab());

        tabPane.getTabs().addAll(cyclesTab, usersTab, rentalsTab, concurrencyTab);
        centerBox.getChildren().add(tabPane);

        mainLayout.setCenter(centerBox);
        rootStack.getChildren().add(mainLayout);
    }

    // ================= CRUD: CYCLES =================
    private Parent buildCyclesCrudTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        Button addBtn = new Button("➕ Add New Cycle");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openAddCycleModal());

        Button editBtn = new Button("✏️ Edit Selected");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Cycle selected = cycleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openEditCycleModal(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a cycle to edit.");
            }
        });

        Button deleteBtn = new Button("🗑️ Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Cycle selected = cycleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete cycle " + selected.getModel() + " [#" + selected.getId() + "]?",
                    ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        cycleDao.delete(selected.getId());
                        refreshAllData();
                    }
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a cycle to delete.");
            }
        });

        TextField search = new TextField();
        search.setPromptText("Search cycles...");
        search.textProperty().addListener((obs, oldV, newV) -> filterCycles(newV));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshAllData());

        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, spacer, search, refreshBtn);
        pane.setTop(toolbar);

        // Columns
        TableColumn<Cycle, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.06));

        TableColumn<Cycle, String> colModel = new TableColumn<>("Model & Brand");
        colModel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrand() + " " + c.getValue().getModel()));
        colModel.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.24));

        TableColumn<Cycle, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLabel()));
        colType.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.16));

        TableColumn<Cycle, String> colRate = new TableColumn<>("Rate ($/hr)");
        colRate.setCellValueFactory(c -> new SimpleStringProperty(String.format("$%.2f", c.getValue().getHourlyRate())));
        colRate.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.10));

        TableColumn<Cycle, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.12));

        TableColumn<Cycle, String> colStation = new TableColumn<>("Dock Station");
        colStation.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        colStation.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.18));

        TableColumn<Cycle, Integer> colRides = new TableColumn<>("Total Rides");
        colRides.setCellValueFactory(new PropertyValueFactory<>("totalRides"));
        colRides.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.10));

        cycleTable.getColumns().addAll(colId, colModel, colType, colRate, colStatus, colStation, colRides);
        cycleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pane.setCenter(cycleTable);
        return pane;
    }

    private void filterCycles(String query) {
        if (query == null || query.trim().isEmpty()) {
            cycleTable.setItems(FXCollections.observableArrayList(cycleDao.findAll()));
            return;
        }
        String q = query.toLowerCase().trim();
        List<Cycle> filtered = cycleDao.findAll().stream()
            .filter(c -> c.getModel().toLowerCase().contains(q) ||
                         c.getBrand().toLowerCase().contains(q) ||
                         c.getStationName().toLowerCase().contains(q) ||
                         c.getType().name().toLowerCase().contains(q))
            .toList();
        cycleTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void openAddCycleModal() {
        showCycleFormModal("Add New Cycle to Campus Fleet", null);
    }

    private void openEditCycleModal(Cycle cycle) {
        showCycleFormModal("Edit Cycle Details [#" + cycle.getId() + "]", cycle);
    }

    private void showCycleFormModal(String title, Cycle existing) {
        VBox modal = new VBox(14);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(20));
        modal.setMaxWidth(420);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 16, 0, 0, 4);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField modelField = new TextField(existing != null ? existing.getModel() : "");
        TextField brandField = new TextField(existing != null ? existing.getBrand() : "");
        ComboBox<CycleType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(CycleType.values()));
        typeCombo.setValue(existing != null ? existing.getType() : CycleType.STANDARD);

        TextField rateField = new TextField(existing != null ? String.valueOf(existing.getHourlyRate()) : "15.0");
        ComboBox<CycleStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(CycleStatus.values()));
        statusCombo.setValue(existing != null ? existing.getStatus() : CycleStatus.AVAILABLE);

        List<Station> stations = stationDao.findAll();
        ComboBox<Station> stationCombo = new ComboBox<>(FXCollections.observableArrayList(stations));
        if (!stations.isEmpty()) {
            if (existing != null) {
                stations.stream().filter(s -> s.getId() == existing.getStationId()).findFirst().ifPresent(stationCombo::setValue);
            } else {
                stationCombo.setValue(stations.get(0));
            }
        }

        TextField batteryField = new TextField(existing != null ? String.valueOf(existing.getBatteryPercentage()) : "-1");
        batteryField.setPromptText("-1 if standard, 0-100 for E-Bike");

        grid.add(new Label("Brand:"), 0, 0);
        grid.add(brandField, 1, 0);
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Hourly Rate ($):"), 0, 3);
        grid.add(rateField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);
        grid.add(new Label("Dock Station:"), 0, 5);
        grid.add(stationCombo, 1, 5);
        grid.add(new Label("Battery (%):"), 0, 6);
        grid.add(batteryField, 1, 6);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");

        Button save = new Button("Save Cycle");
        save.getStyleClass().add("btn-primary");

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);");
        overlay.getChildren().add(modal);

        cancel.setOnAction(e -> rootStack.getChildren().remove(overlay));

        save.setOnAction(e -> {
            try {
                String model = modelField.getText().trim();
                String brand = brandField.getText().trim();
                double rate = Double.parseDouble(rateField.getText().trim());
                int battery = Integer.parseInt(batteryField.getText().trim());
                Station st = stationCombo.getValue();
                int stId = st != null ? st.getId() : 1;

                if (model.isEmpty() || brand.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Model and Brand cannot be empty.");
                    return;
                }

                if (existing == null) {
                    Cycle newCycle = new Cycle(0, model, brand, typeCombo.getValue(), rate, statusCombo.getValue(), stId, battery);
                    cycleDao.save(newCycle);
                } else {
                    existing.setModel(model);
                    existing.setBrand(brand);
                    existing.setType(typeCombo.getValue());
                    existing.setHourlyRate(rate);
                    existing.setStatus(statusCombo.getValue());
                    existing.setStationId(stId);
                    existing.setBatteryPercentage(battery);
                    cycleDao.update(existing);
                }

                rootStack.getChildren().remove(overlay);
                refreshAllData();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numeric values for Rate and Battery.");
            }
        });

        btnBar.getChildren().addAll(cancel, save);
        modal.getChildren().addAll(titleLabel, grid, btnBar);
        rootStack.getChildren().add(overlay);
    }

    // ================= CRUD: USERS =================
    private Parent buildUsersCrudTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        Button deleteUserBtn = new Button("🗑️ Delete Selected User");
        deleteUserBtn.getStyleClass().add("btn-danger");
        deleteUserBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getId() == authService.getCurrentUser().getId()) {
                    showAlert(Alert.AlertType.WARNING, "Forbidden", "You cannot delete your own admin account.");
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete user account " + selected.getFullName() + " (" + selected.getUsername() + ")?",
                    ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        userDao.delete(selected.getId());
                        refreshAllData();
                    }
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete.");
            }
        });

        toolbar.getChildren().add(deleteUserBtn);
        pane.setTop(toolbar);

        TableColumn<User, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(userTable.widthProperty().multiply(0.06));

        TableColumn<User, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colName.prefWidthProperty().bind(userTable.widthProperty().multiply(0.20));

        TableColumn<User, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUser.prefWidthProperty().bind(userTable.widthProperty().multiply(0.14));

        TableColumn<User, String> colRole = new TableColumn<>("Role (Polymorphic)");
        colRole.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getRole().getDisplayName()));
        colRole.prefWidthProperty().bind(userTable.widthProperty().multiply(0.16));

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.prefWidthProperty().bind(userTable.widthProperty().multiply(0.22));

        TableColumn<User, String> colDept = new TableColumn<>("Department / ID");
        colDept.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getDepartment() + " (" + u.getValue().getRoleSpecificId() + ")"));
        colDept.prefWidthProperty().bind(userTable.widthProperty().multiply(0.22));

        userTable.getColumns().addAll(colId, colName, colUser, colRole, colEmail, colDept);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pane.setCenter(userTable);
        return pane;
    }

    // ================= RENTALS AUDIT LEDGER =================
    private Parent buildRentalsTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        TableColumn<Rental, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.06));

        TableColumn<Rental, String> colUser = new TableColumn<>("User");
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colUser.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.18));

        TableColumn<Rental, String> colCycle = new TableColumn<>("Rented Cycle");
        colCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        colCycle.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.22));

        TableColumn<Rental, String> colStart = new TableColumn<>("Start Time");
        colStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colStart.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.18));

        TableColumn<Rental, String> colEnd = new TableColumn<>("End Time");
        colEnd.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getEndTime() != null ? r.getValue().getEndTime() : "Active Now"));
        colEnd.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.18));

        TableColumn<Rental, String> colCost = new TableColumn<>("Revenue ($)");
        colCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("$%.2f", r.getValue().getTotalCost())));
        colCost.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.08));

        TableColumn<Rental, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getStatus().name()));
        colStatus.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.10));

        rentalTable.getColumns().addAll(colId, colUser, colCycle, colStart, colEnd, colCost, colStatus);
        rentalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pane.setCenter(rentalTable);
        return pane;
    }

    // ================= CONCURRENCY & THREAD POOL DEMO =================
    private Parent buildConcurrencyExportTab() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(24));
        container.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Multi-Threaded System Report & CSV Audit Generator");
        title.getStyleClass().add("card-title");

        Label desc = new Label(
            "This module satisfies the Concurrency & Thread Pool assignment requirement. " +
            "It launches a JavaFX Task on a managed background Thread Pool (ExecutorService) to compile database records " +
            "and write an audit file to disk without blocking or freezing the JavaFX Application Thread."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox exportCard = new VBox(14);
        exportCard.getStyleClass().add("card");
        exportCard.setMaxWidth(650);

        Label cardHeader = new Label("Export Complete Database Ledger (CSV)");
        cardHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(22);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(24, 24);
        spinner.setVisible(false);

        Label progressStatusLabel = new Label("Status: Ready to export");
        progressStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        HBox statusBox = new HBox(10, spinner, progressStatusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Button startExportBtn = new Button("🚀 Start Background Export Task (Thread Pool)");
        startExportBtn.getStyleClass().add("btn-primary");

        startExportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save CampusCycle Audit CSV");
            chooser.setInitialFileName("campuscycle_system_audit.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = chooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());

            if (file != null) {
                startExportBtn.setDisable(true);
                spinner.setVisible(true);

                // Create background task
                Task<File> exportTask = reportService.createExportAuditReportTask(file);

                // Bind progress and status message directly to JavaFX UI
                progressBar.progressProperty().bind(exportTask.progressProperty());
                progressStatusLabel.textProperty().bind(exportTask.messageProperty());

                exportTask.setOnSucceeded(t -> {
                    spinner.setVisible(false);
                    startExportBtn.setDisable(false);
                    progressBar.progressProperty().unbind();
                    progressStatusLabel.textProperty().unbind();
                    progressStatusLabel.setText("✅ Export Completed: " + file.getAbsolutePath());
                    showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Audit report has been written successfully to:\n" + file.getAbsolutePath());
                });

                exportTask.setOnFailed(t -> {
                    spinner.setVisible(false);
                    startExportBtn.setDisable(false);
                    progressBar.progressProperty().unbind();
                    progressStatusLabel.textProperty().unbind();
                    progressStatusLabel.setText("❌ Export failed: " + exportTask.getException().getMessage());
                });

                // Submit to ThreadPoolManager ExecutorService
                ThreadPoolManager.getInstance().execute(exportTask);
            }
        });

        exportCard.getChildren().addAll(cardHeader, desc, startExportBtn, progressBar, statusBox);
        container.getChildren().addAll(title, exportCard);
        return container;
    }

    public void refreshAllData() {
        // Refresh Stats
        ReportService.DashboardStats stats = reportService.getQuickStats();
        totalFleetCard.setValue(String.valueOf(stats.totalCycles()));
        availableCard.setValue(String.valueOf(stats.availableCycles()));
        activeRentalsCard.setValue(String.valueOf(stats.activeRentals()));
        revenueCard.setValue(String.format("$%.2f", stats.totalRevenue()));

        // Refresh Tables
        cycleTable.setItems(FXCollections.observableArrayList(cycleDao.findAll()));
        userTable.setItems(FXCollections.observableArrayList(userDao.findAll()));
        rentalTable.setItems(FXCollections.observableArrayList(rentalDao.findAll()));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Parent getView() {
        return rootStack;
    }
}
