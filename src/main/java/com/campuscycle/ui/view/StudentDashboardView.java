package com.campuscycle.ui.view;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.StationDao;
import com.campuscycle.model.*;
import com.campuscycle.service.AuthService;
import com.campuscycle.service.RentalService;
import com.campuscycle.ui.NavigationManager;
import com.campuscycle.ui.view.components.CycleCard;
import com.campuscycle.ui.view.components.WeatherWidget;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Main Portal for Students & Staff members.
 * Demonstrates:
 * - Layout Panes: BorderPane, StackPane, FlowPane, ScrollPane, GridPane, VBox, HBox, TabPane
 * - UI Controls: TableView, ComboBox, TextField, TextArea, Button, Badge
 * - Layout Responsiveness: Fluid property bindings relative to window height and width
 * - Concurrency: Live weather widget, background data refresh
 */
public class StudentDashboardView {
    private final AuthService authService;
    private final RentalService rentalService;
    private final CycleDao cycleDao;
    private final StationDao stationDao;

    private final StackPane rootStack;
    private final BorderPane mainLayout;
    private final FlowPane cyclesContainer;
    private final TableView<Rental> historyTable;

    // Active Rental Card elements
    private VBox activeRentalCard;
    private Label activeStatusLabel;
    private Label activeBikeNameLabel;
    private Label activeTimeLabel;
    private Label activeCostLabel;
    private ComboBox<Station> returnStationCombo;
    private Button returnBtn;

    // Filter controls
    private ComboBox<String> typeFilterCombo;
    private TextField searchField;

    public StudentDashboardView() {
        this.authService = AuthService.getInstance();
        this.rentalService = new RentalService();
        this.cycleDao = new CycleDao();
        this.stationDao = new StationDao();

        this.rootStack = new StackPane();
        this.mainLayout = new BorderPane();
        this.cyclesContainer = new FlowPane();
        this.historyTable = new TableView<>();

        buildUI();
        refreshAllData();
    }

    private void buildUI() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(() -> NavigationManager.getInstance().showLoginView());
            return;
        }

        // ================= TOP BAR =================
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("🚲 CampusCycle");
        brandTitle.getStyleClass().add("brand-title");

        String subtitleText = currentUser.getFullName() + " • " + currentUser.getRole().getDisplayName();
        if (currentUser instanceof Student) {
            subtitleText += " (ID: " + ((Student) currentUser).getStudentId() + " • " + ((Student) currentUser).getLoyaltyPoints() + " Pts)";
        }
        Label brandSub = new Label(subtitleText);
        brandSub.getStyleClass().add("brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        // Center / Right: Live Weather Widget (Networking + JSON) + Logout Button
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

        // ================= CENTER CONTENT (TAB PANE) =================
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Available Cycles (Browse & Rent)
        Tab cyclesTab = new Tab("🚴 Available Cycles", buildCyclesTabContent());

        // Tab 2: My Active Rental & Return
        Tab activeTab = new Tab("⚡ My Active Ride", buildActiveRentalTabContent());

        // Tab 3: Rental History Table
        Tab historyTab = new Tab("📜 Rental History", buildHistoryTabContent());

        tabPane.getTabs().addAll(cyclesTab, activeTab, historyTab);
        mainLayout.setCenter(tabPane);

        rootStack.getChildren().add(mainLayout);
    }

    private Parent buildCyclesTabContent() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(16, 20, 16, 20));

        // --- Filter Toolbar ---
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(8, 0, 14, 0));

        Label filterLabel = new Label("Filter by Type:");
        filterLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");

        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.getItems().addAll("All Categories", "Standard City", "Multi-Speed Geared", "E-Bike (Electric)", "All-Terrain Mountain");
        typeFilterCombo.setValue("All Categories");
        typeFilterCombo.setOnAction(e -> applyCycleFilter());

        searchField = new TextField();
        searchField.setPromptText("Search model or dock station...");
        // Layout Responsiveness: Bind searchField width relative to window
        searchField.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.25));
        searchField.textProperty().addListener((obs, oldV, newV) -> applyCycleFilter());

        Button refreshBtn = new Button("Refresh Fleet");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshAllData());

        // Discount banner
        User user = authService.getCurrentUser();
        String discountText = user.getPricingStrategy().getStrategyName();
        Label discountBadge = new Label("🎁 " + discountText);
        discountBadge.getStyleClass().add("badge-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(filterLabel, typeFilterCombo, searchField, refreshBtn, spacer, discountBadge);
        content.setTop(filterBar);

        // --- Grid of Cycle Cards using FlowPane ---
        cyclesContainer.setHgap(16);
        cyclesContainer.setVgap(16);
        cyclesContainer.setPadding(new Insets(10));
        // Layout Responsiveness: Dynamic flow wrapping with window width
        cyclesContainer.prefWidthProperty().bind(mainLayout.widthProperty().subtract(60));

        ScrollPane scrollPane = new ScrollPane(cyclesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        content.setCenter(scrollPane);
        return content;
    }

    private Parent buildActiveRentalTabContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setAlignment(Pos.TOP_CENTER);

        activeRentalCard = new VBox(16);
        activeRentalCard.getStyleClass().add("card");
        activeRentalCard.setAlignment(Pos.CENTER);
        activeRentalCard.setPadding(new Insets(28));
        // Layout Responsiveness
        activeRentalCard.maxWidthProperty().bind(mainLayout.widthProperty().multiply(0.60));
        activeRentalCard.minWidthProperty().set(400);

        Label cardTitle = new Label("Current Active Booking");
        cardTitle.getStyleClass().add("card-title");

        activeStatusLabel = new Label("Checking active rentals...");
        activeStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        activeBikeNameLabel = new Label("No Active Ride");
        activeBikeNameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        activeTimeLabel = new Label();
        activeTimeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        activeCostLabel = new Label();
        activeCostLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        // Return dock selector
        HBox returnBox = new HBox(12);
        returnBox.setAlignment(Pos.CENTER);
        Label dockLabel = new Label("Select Return Station:");
        dockLabel.setStyle("-fx-font-weight: 600;");

        returnStationCombo = new ComboBox<>();
        returnStationCombo.setPromptText("Choose docking dock...");
        returnStationCombo.setPrefWidth(220);

        returnBox.getChildren().addAll(dockLabel, returnStationCombo);

        TextField notesField = new TextField();
        notesField.setPromptText("Optional return condition notes (e.g. Clean, tires checked)");
        notesField.setMaxWidth(350);

        returnBtn = new Button("Lock & Complete Return");
        returnBtn.getStyleClass().add("btn-success");
        returnBtn.setPrefHeight(38);
        returnBtn.setMaxWidth(280);

        returnBtn.setOnAction(e -> {
            User user = authService.getCurrentUser();
            Optional<Rental> activeOpt = rentalService.getActiveRentalForUser(user.getId());
            if (activeOpt.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Active Rental", "You do not have any active cycle to return.");
                return;
            }

            Station targetStation = returnStationCombo.getValue();
            if (targetStation == null) {
                showAlert(Alert.AlertType.WARNING, "Station Required", "Please select a docking station where you are locking the cycle.");
                return;
            }

            boolean ok = rentalService.returnCycle(activeOpt.get(), targetStation.getId(), notesField.getText());
            if (ok) {
                showAlert(Alert.AlertType.INFORMATION, "Cycle Returned Successfully",
                    "Thank you for riding with CampusCycle! Your ride has been recorded and 10 campus loyalty points were awarded.");
                refreshAllData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Return Error", "Failed to update return status in database.");
            }
        });

        activeRentalCard.getChildren().addAll(
            cardTitle, activeBikeNameLabel, activeStatusLabel, activeTimeLabel,
            activeCostLabel, new Separator(), returnBox, notesField, returnBtn
        );

        container.getChildren().add(activeRentalCard);
        return container;
    }

    private Parent buildHistoryTabContent() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(16, 20, 16, 20));

        Label title = new Label("Your Campus Rental History");
        title.getStyleClass().add("card-title");

        // TableView configuration
        TableColumn<Rental, Integer> colId = new TableColumn<>("Rental #");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.10));

        TableColumn<Rental, String> colCycle = new TableColumn<>("Cycle Details");
        colCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        colCycle.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.25));

        TableColumn<Rental, String> colStart = new TableColumn<>("Start Time");
        colStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colStart.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.20));

        TableColumn<Rental, String> colEnd = new TableColumn<>("End Time");
        colEnd.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getEndTime() != null ? r.getValue().getEndTime() : "In Progress"));
        colEnd.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.20));

        TableColumn<Rental, Integer> colHours = new TableColumn<>("Hours");
        colHours.setCellValueFactory(new PropertyValueFactory<>("durationHours"));
        colHours.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.08));

        TableColumn<Rental, String> colCost = new TableColumn<>("Total Paid");
        colCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("$%.2f", r.getValue().getTotalCost())));
        colCost.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.10));

        TableColumn<Rental, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getStatus().name()));
        colStatus.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.12));

        historyTable.getColumns().addAll(colId, colCycle, colStart, colEnd, colHours, colCost, colStatus);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Layout Responsiveness
        historyTable.prefHeightProperty().bind(mainLayout.heightProperty().multiply(0.70));

        container.getChildren().addAll(title, historyTable);
        return container;
    }

    public void refreshAllData() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        // Populate return stations
        List<Station> stations = stationDao.findAll();
        returnStationCombo.setItems(FXCollections.observableArrayList(stations));
        if (!stations.isEmpty()) returnStationCombo.setValue(stations.get(0));

        // Populate Available Cycles
        applyCycleFilter();

        // Populate Active Rental
        Optional<Rental> activeOpt = rentalService.getActiveRentalForUser(user.getId());
        if (activeOpt.isPresent()) {
            Rental active = activeOpt.get();
            activeBikeNameLabel.setText(active.getCycleName());
            activeStatusLabel.setText("Currently Rented • Ride in Progress");
            activeTimeLabel.setText("Started: " + active.getStartTime() + " (" + active.getDurationHours() + " hr booking)");
            activeCostLabel.setText(String.format("Total Fee: $%.2f (Prepaid)", active.getTotalCost()));
            returnBtn.setDisable(false);
            returnStationCombo.setDisable(false);
        } else {
            activeBikeNameLabel.setText("No Active Ride");
            activeStatusLabel.setText("You do not currently have any rented cycles.");
            activeTimeLabel.setText("Browse 'Available Cycles' tab to reserve an eco-friendly campus bike.");
            activeCostLabel.setText("");
            returnBtn.setDisable(true);
            returnStationCombo.setDisable(true);
        }

        // Populate History Table
        List<Rental> history = rentalService.getUserRentals(user.getId());
        historyTable.setItems(FXCollections.observableArrayList(history));
    }

    private void applyCycleFilter() {
        User user = authService.getCurrentUser();
        List<Cycle> cycles = cycleDao.findAll();
        String selectedCategory = typeFilterCombo != null ? typeFilterCombo.getValue() : "All Categories";
        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        cyclesContainer.getChildren().clear();

        for (Cycle c : cycles) {
            boolean matchesCategory = selectedCategory.equals("All Categories") || c.getType().getLabel().equals(selectedCategory);
            boolean matchesSearch = searchText.isEmpty() ||
                c.getModel().toLowerCase().contains(searchText) ||
                c.getBrand().toLowerCase().contains(searchText) ||
                c.getStationName().toLowerCase().contains(searchText);

            if (matchesCategory && matchesSearch) {
                CycleCard card = new CycleCard(c, user, this::openBookingModal);
                cyclesContainer.getChildren().add(card);
            }
        }
    }

    /**
     * Modal dialog overlay for reserving a cycle.
     * Demonstrates StackPane modal layering and dynamic pricing calculation.
     */
    private void openBookingModal(Cycle cycle) {
        User user = authService.getCurrentUser();

        // Check if user already has active ride
        if (rentalService.getActiveRentalForUser(user.getId()).isPresent()) {
            showAlert(Alert.AlertType.WARNING, "Booking Limit", "You already have an active ride. Please return your current cycle first.");
            return;
        }

        VBox modalCard = new VBox(16);
        modalCard.getStyleClass().add("card");
        modalCard.setPadding(new Insets(24));
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setMaxWidth(440);
        modalCard.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 16, 0, 0, 4);");

        Label modalTitle = new Label("Reserve " + cycle.getBrand() + " " + cycle.getModel());
        modalTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);

        Spinner<Integer> hoursSpinner = new Spinner<>(1, 12, 1);
        hoursSpinner.setEditable(false);

        ComboBox<PaymentMethod> payCombo = new ComboBox<>();
        payCombo.getItems().addAll(PaymentMethod.CAMPUS_CARD, PaymentMethod.BKASH, PaymentMethod.CREDIT_CARD, PaymentMethod.CASH);
        payCombo.setValue(PaymentMethod.CAMPUS_CARD);

        Label baseRateLabel = new Label(String.format("$%.2f / hour", cycle.getHourlyRate()));
        Label appliedDiscountLabel = new Label(user.getPricingStrategy().getStrategyName());
        appliedDiscountLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        Label finalPriceLabel = new Label();
        finalPriceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");

        // Dynamic price update listener
        Runnable updateCost = () -> {
            int hrs = hoursSpinner.getValue();
            double cost = cycle.calculateCost(hrs, user.getPricingStrategy());
            finalPriceLabel.setText(String.format("$%.2f", cost));
        };
        hoursSpinner.valueProperty().addListener((obs, oldV, newV) -> updateCost.run());
        updateCost.run();

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Trip destination or notes (e.g. Science Library to Main Gate)");
        notesArea.setPrefRowCount(2);

        grid.add(new Label("Dock Location:"), 0, 0);
        grid.add(new Label(cycle.getStationName()), 1, 0);

        grid.add(new Label("Duration (Hours):"), 0, 1);
        grid.add(hoursSpinner, 1, 1);

        grid.add(new Label("Pricing Tier:"), 0, 2);
        grid.add(appliedDiscountLabel, 1, 2);

        grid.add(new Label("Payment Method:"), 0, 3);
        grid.add(payCombo, 1, 3);

        grid.add(new Label("Total Estimated Cost:"), 0, 4);
        grid.add(finalPriceLabel, 1, 4);

        grid.add(new Label("Trip Note:"), 0, 5);
        grid.add(notesArea, 1, 5);

        // Buttons
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button confirmBtn = new Button("Confirm & Rent Now");
        confirmBtn.getStyleClass().add("btn-primary");

        // Dim background overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);");
        overlay.getChildren().add(modalCard);

        cancelBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));

        confirmBtn.setOnAction(e -> {
            try {
                int hours = hoursSpinner.getValue();
                rentalService.rentCycle(user, cycle, hours, payCombo.getValue(), notesArea.getText());
                rootStack.getChildren().remove(overlay);
                showAlert(Alert.AlertType.INFORMATION, "Booking Confirmed",
                    "Cycle " + cycle.getModel() + " is now rented for " + hours + " hour(s). Happy cycling!");
                refreshAllData();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Rental Failed", ex.getMessage());
            }
        });

        buttonBar.getChildren().addAll(cancelBtn, confirmBtn);
        modalCard.getChildren().addAll(modalTitle, grid, buttonBar);

        rootStack.getChildren().add(overlay);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Parent getView() {
        return rootStack;
    }
}
