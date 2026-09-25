package com.campuscycle.ui.view;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.StationDao;
import com.campuscycle.model.*;
import com.campuscycle.service.AuthService;
import com.campuscycle.service.RentalService;
import com.campuscycle.service.WalletService;
import com.campuscycle.ui.NavigationManager;
import com.campuscycle.ui.view.components.CycleCard;
import com.campuscycle.ui.view.components.WeatherWidget;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Enterprise Rider Mobility Portal for Students & Faculty.
 * Features prepaid wallet balance, live ride timer, damage reporting, and audit history.
 */
public class StudentDashboardView {
    private final AuthService authService;
    private final RentalService rentalService;
    private final WalletService walletService;
    private final CycleDao cycleDao;
    private final StationDao stationDao;

    private final StackPane rootStack;
    private final BorderPane mainLayout;
    private final FlowPane cyclesContainer;
    private final TableView<Rental> historyTable;
    private final TableView<WalletTransaction> walletTable;

    // Header Wallet & Info Labels
    private Label walletBalanceLabel;
    private Label loyaltyPointsLabel;

    // Active Ride Elements
    private VBox activeRentalCard;
    private Label activeStatusLabel;
    private Label activeBikeNameLabel;
    private Label activeTimeLabel;
    private Label activeDurationTimerLabel;
    private Label activeCostLabel;
    private ComboBox<Station> returnStationCombo;
    private Button returnBtn;

    // Return Damage Reporting Checkbox & Subform
    private CheckBox reportDamageCheck;
    private VBox damageReportBox;
    private ComboBox<MaintenanceTicket.IssueCategory> damageCategoryCombo;
    private TextField damageDetailsField;

    // Filters
    private ComboBox<String> typeFilterCombo;
    private ComboBox<String> stationFilterCombo;
    private TextField searchField;

    // Timer for active ride elapsed time
    private Timeline rideTimer;

    public StudentDashboardView() {
        this.authService = AuthService.getInstance();
        this.rentalService = new RentalService();
        this.walletService = new WalletService();
        this.cycleDao = new CycleDao();
        this.stationDao = new StationDao();

        this.rootStack = new StackPane();
        this.mainLayout = new BorderPane();
        this.cyclesContainer = new FlowPane();
        this.historyTable = new TableView<>();
        this.walletTable = new TableView<>();

        buildUI();
        refreshAllData();
        setupRideTimer();
    }

    private void buildUI() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(() -> NavigationManager.getInstance().showLoginView());
            return;
        }

        // ================= TOP HEADER =================
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("🚲 CampusCycle");
        brandTitle.getStyleClass().add("brand-title");

        String subtitleText = currentUser.getFullName() + " • " + currentUser.getRole().getDisplayName();
        if (currentUser instanceof Student) {
            subtitleText += " (" + ((Student) currentUser).getStudentId() + ")";
        }
        Label brandSub = new Label(subtitleText);
        brandSub.getStyleClass().add("brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        // Center / Right Controls: Wallet Pill, Weather, Sign Out
        HBox rightHeaderBox = new HBox(12);
        rightHeaderBox.setAlignment(Pos.CENTER_RIGHT);

        // Campus Pay Wallet Box
        HBox walletPill = new HBox(8);
        walletPill.setAlignment(Pos.CENTER);
        walletPill.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #86efac; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 4 12 4 12;");

        walletBalanceLabel = new Label("💳 Campus Pay: $0.00");
        walletBalanceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #166534; -fx-font-size: 13px;");

        Button topUpBtn = new Button("+ Top Up");
        topUpBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        topUpBtn.setOnAction(e -> openTopUpModal());

        walletPill.getChildren().addAll(walletBalanceLabel, topUpBtn);

        // Loyalty points
        loyaltyPointsLabel = new Label("⭐ 0 Pts");
        loyaltyPointsLabel.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 5 10 5 10; -fx-background-radius: 12; -fx-font-size: 12px;");

        WeatherWidget weatherWidget = new WeatherWidget();

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> {
            if (rideTimer != null) rideTimer.stop();
            authService.logout();
            NavigationManager.getInstance().showLoginView();
        });

        rightHeaderBox.getChildren().addAll(walletPill, loyaltyPointsLabel, weatherWidget, logoutBtn);

        header.setLeft(brandBox);
        header.setRight(rightHeaderBox);
        mainLayout.setTop(header);

        // ================= CENTER CONTENT (TAB PANE) =================
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab cyclesTab = new Tab("🚴 Available Cycles", buildCyclesTabContent());
        Tab activeTab = new Tab("⚡ My Active Ride", buildActiveRentalTabContent());
        Tab historyTab = new Tab("📜 Ride History & Receipts", buildHistoryTabContent());
        Tab walletTab = new Tab("💳 Campus Pay Ledger", buildWalletTabContent());

        tabPane.getTabs().addAll(cyclesTab, activeTab, historyTab, walletTab);
        mainLayout.setCenter(tabPane);

        rootStack.getChildren().add(mainLayout);
    }

    private Parent buildCyclesTabContent() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(16, 20, 16, 20));

        // Filter Toolbar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 0, 14, 0));

        Label typeLbl = new Label("Type:");
        typeLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");
        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.getItems().addAll("All Types", "Standard City", "Multi-Speed Geared", "E-Bike (Electric)", "All-Terrain Mountain");
        typeFilterCombo.setValue("All Types");
        typeFilterCombo.setOnAction(e -> applyCycleFilter());

        Label dockLbl = new Label("Dock:");
        dockLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");
        stationFilterCombo = new ComboBox<>();
        stationFilterCombo.getItems().add("All Stations");
        stationDao.findAll().forEach(s -> stationFilterCombo.getItems().add(s.getName()));
        stationFilterCombo.setValue("All Stations");
        stationFilterCombo.setOnAction(e -> applyCycleFilter());

        searchField = new TextField();
        searchField.setPromptText("Search model or brand...");
        searchField.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.20));
        searchField.textProperty().addListener((obs, oldV, newV) -> applyCycleFilter());

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshAllData());

        User user = authService.getCurrentUser();
        Label tierBadge = new Label("🏷️ Applied: " + user.getPricingStrategy().getStrategyName());
        tierBadge.getStyleClass().add("badge-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(typeLbl, typeFilterCombo, dockLbl, stationFilterCombo, searchField, refreshBtn, spacer, tierBadge);
        content.setTop(filterBar);

        // Grid of Cycle Cards using FlowPane
        cyclesContainer.setHgap(16);
        cyclesContainer.setVgap(16);
        cyclesContainer.setPadding(new Insets(10));
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
        activeRentalCard.maxWidthProperty().bind(mainLayout.widthProperty().multiply(0.60));
        activeRentalCard.minWidthProperty().set(440);

        Label cardTitle = new Label("Active Rental Session");
        cardTitle.getStyleClass().add("card-title");

        activeBikeNameLabel = new Label("No Active Ride");
        activeBikeNameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        activeStatusLabel = new Label("You do not currently have any checked-out cycles.");
        activeStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        activeDurationTimerLabel = new Label();
        activeDurationTimerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-background-color: #ecfdf5; -fx-padding: 6 16 6 16; -fx-background-radius: 20;");
        activeDurationTimerLabel.setVisible(false);

        activeTimeLabel = new Label();
        activeTimeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        activeCostLabel = new Label();
        activeCostLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        // Dock Station Return Selector
        HBox returnBox = new HBox(12);
        returnBox.setAlignment(Pos.CENTER);
        Label dockLabel = new Label("Lock at Return Dock:");
        dockLabel.setStyle("-fx-font-weight: 600;");

        returnStationCombo = new ComboBox<>();
        returnStationCombo.setPromptText("Choose docking station...");
        returnStationCombo.setPrefWidth(240);

        returnBox.getChildren().addAll(dockLabel, returnStationCombo);

        TextField notesField = new TextField();
        notesField.setPromptText("Optional return notes");
        notesField.setMaxWidth(380);

        // Issue / Damage Reporting Subform
        reportDamageCheck = new CheckBox("Report damage or mechanical issue on this cycle");
        reportDamageCheck.setStyle("-fx-font-weight: 600; -fx-text-fill: #b91c1c;");

        damageReportBox = new VBox(8);
        damageReportBox.setVisible(false);
        damageReportBox.setManaged(false);
        damageReportBox.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");
        damageReportBox.setMaxWidth(400);

        damageCategoryCombo = new ComboBox<>(FXCollections.observableArrayList(MaintenanceTicket.IssueCategory.values()));
        damageCategoryCombo.setValue(MaintenanceTicket.IssueCategory.FLAT_TIRE);
        damageCategoryCombo.setMaxWidth(Double.MAX_VALUE);

        damageDetailsField = new TextField();
        damageDetailsField.setPromptText("Describe the defect (e.g. rear brake loose)");

        damageReportBox.getChildren().addAll(new Label("Select Issue Category:"), damageCategoryCombo, damageDetailsField);

        reportDamageCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            damageReportBox.setVisible(newV);
            damageReportBox.setManaged(newV);
        });

        returnBtn = new Button("🔒 Dock & Complete Return");
        returnBtn.getStyleClass().add("btn-success");
        returnBtn.setPrefHeight(40);
        returnBtn.setMaxWidth(300);

        returnBtn.setOnAction(e -> {
            User user = authService.getCurrentUser();
            Optional<Rental> activeOpt = rentalService.getActiveRentalForUser(user.getId());
            if (activeOpt.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Active Rental", "You do not have any active rental to return.");
                return;
            }

            Station targetStation = returnStationCombo.getValue();
            if (targetStation == null) {
                showAlert(Alert.AlertType.WARNING, "Dock Station Required", "Please select the docking station where you have locked the cycle.");
                return;
            }

            boolean reportDamage = reportDamageCheck.isSelected();
            MaintenanceTicket.IssueCategory cat = reportDamage ? damageCategoryCombo.getValue() : null;
            String details = reportDamage ? damageDetailsField.getText() : null;

            RentalService.ReturnReceipt receipt = rentalService.returnCycle(
                activeOpt.get(), targetStation.getId(), notesField.getText(), reportDamage, cat, details
            );

            if (receipt != null) {
                String msg = String.format("Ride Duration: %d min\nTotal Charged: $%.2f",
                    receipt.actualMinutes(), receipt.totalCharged());
                if (receipt.overdueFine() > 0) {
                    msg += String.format("\n⚠️ Overdue Fine Applied: $%.2f", receipt.overdueFine());
                }
                msg += "\n⭐ Campus Loyalty Earned: +" + receipt.loyaltyPointsEarned() + " Points";
                if (reportDamage) {
                    msg += "\n🛠️ Maintenance ticket submitted. Technicians notified.";
                }

                showAlert(Alert.AlertType.INFORMATION, "Cycle Docked Successfully", msg);
                reportDamageCheck.setSelected(false);
                refreshAllData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Return Error", "Could not complete dock transaction.");
            }
        });

        activeRentalCard.getChildren().addAll(
            cardTitle, activeBikeNameLabel, activeStatusLabel, activeDurationTimerLabel,
            activeTimeLabel, activeCostLabel, new Separator(), returnBox, notesField,
            reportDamageCheck, damageReportBox, returnBtn
        );

        container.getChildren().add(activeRentalCard);
        return container;
    }

    private Parent buildHistoryTabContent() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(16, 20, 16, 20));

        Label title = new Label("Your Campus Mobility Journey & Receipts");
        title.getStyleClass().add("card-title");

        TableColumn<Rental, Integer> colId = new TableColumn<>("Rental #");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.08));

        TableColumn<Rental, String> colCycle = new TableColumn<>("Cycle Details");
        colCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        colCycle.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.26));

        TableColumn<Rental, String> colStart = new TableColumn<>("Start Time");
        colStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colStart.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.18));

        TableColumn<Rental, String> colEnd = new TableColumn<>("Return Time");
        colEnd.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getEndTime() != null ? r.getValue().getEndTime() : "In Progress"));
        colEnd.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.18));

        TableColumn<Rental, Integer> colHours = new TableColumn<>("Booked Hrs");
        colHours.setCellValueFactory(new PropertyValueFactory<>("durationHours"));
        colHours.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.10));

        TableColumn<Rental, String> colCost = new TableColumn<>("Total Paid");
        colCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("$%.2f", r.getValue().getTotalCost())));
        colCost.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.10));

        TableColumn<Rental, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getStatus().name()));
        colStatus.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.10));

        historyTable.getColumns().addAll(colId, colCycle, colStart, colEnd, colHours, colCost, colStatus);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.prefHeightProperty().bind(mainLayout.heightProperty().multiply(0.70));

        container.getChildren().addAll(title, historyTable);
        return container;
    }

    private Parent buildWalletTabContent() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(16, 20, 16, 20));

        BorderPane top = new BorderPane();
        Label title = new Label("Campus Pay Prepaid Ledger");
        title.getStyleClass().add("card-title");

        Button topUpBtn = new Button("➕ Top Up Balance");
        topUpBtn.getStyleClass().add("btn-primary");
        topUpBtn.setOnAction(e -> openTopUpModal());

        top.setLeft(title);
        top.setRight(topUpBtn);

        TableColumn<WalletTransaction, Integer> colId = new TableColumn<>("Txn #");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.08));

        TableColumn<WalletTransaction, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getType().getLabel()));
        colType.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.20));

        TableColumn<WalletTransaction, String> colAmt = new TableColumn<>("Amount ($)");
        colAmt.setCellValueFactory(t -> new SimpleStringProperty(
            (t.getValue().getAmount() >= 0 ? "+$" : "-$") + String.format("%.2f", Math.abs(t.getValue().getAmount()))
        ));
        colAmt.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.12));

        TableColumn<WalletTransaction, String> colBal = new TableColumn<>("Balance After");
        colBal.setCellValueFactory(t -> new SimpleStringProperty(String.format("$%.2f", t.getValue().getBalanceAfter())));
        colBal.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.14));

        TableColumn<WalletTransaction, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colTime.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.18));

        TableColumn<WalletTransaction, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.prefWidthProperty().bind(walletTable.widthProperty().multiply(0.28));

        walletTable.getColumns().addAll(colId, colType, colAmt, colBal, colTime, colDesc);
        walletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        walletTable.prefHeightProperty().bind(mainLayout.heightProperty().multiply(0.70));

        container.getChildren().addAll(top, walletTable);
        return container;
    }

    private void setupRideTimer() {
        rideTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            User user = authService.getCurrentUser();
            if (user == null) return;
            Optional<Rental> activeOpt = rentalService.getActiveRentalForUser(user.getId());
            if (activeOpt.isPresent()) {
                Rental r = activeOpt.get();
                try {
                    LocalDateTime start = LocalDateTime.parse(r.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    java.time.Duration diff = java.time.Duration.between(start, LocalDateTime.now());
                    long mins = diff.toMinutes();
                    long secs = diff.minusMinutes(mins).getSeconds();
                    activeDurationTimerLabel.setText(String.format("⏱️ Elapsed Ride Time: %02d min %02d sec", mins, secs));
                    activeDurationTimerLabel.setVisible(true);
                } catch (Exception ex) {
                    // Ignore parsing error
                }
            } else {
                activeDurationTimerLabel.setVisible(false);
            }
        }));
        rideTimer.setCycleCount(Timeline.INDEFINITE);
        rideTimer.play();
    }

    public void refreshAllData() {
        authService.refreshCurrentUser();
        User user = authService.getCurrentUser();
        if (user == null) return;

        // Wallet and points
        double balance = walletService.getBalance(user.getId());
        walletBalanceLabel.setText(String.format("💳 Campus Pay: $%.2f", balance));

        if (user instanceof Student) {
            loyaltyPointsLabel.setText("⭐ " + ((Student) user).getLoyaltyPoints() + " Pts");
            loyaltyPointsLabel.setVisible(true);
        } else {
            loyaltyPointsLabel.setVisible(false);
        }

        // Return stations
        List<Station> stations = stationDao.findAll();
        returnStationCombo.setItems(FXCollections.observableArrayList(stations));
        if (!stations.isEmpty()) returnStationCombo.setValue(stations.get(0));

        // Available cycles
        applyCycleFilter();

        // Active Rental card
        Optional<Rental> activeOpt = rentalService.getActiveRentalForUser(user.getId());
        if (activeOpt.isPresent()) {
            Rental active = activeOpt.get();
            activeBikeNameLabel.setText(active.getCycleName());
            activeStatusLabel.setText("Currently Rented • Live Ride Active");
            activeTimeLabel.setText("Started: " + active.getStartTime() + " (" + active.getDurationHours() + " hr booking)");
            activeCostLabel.setText(String.format("Fare Paid: $%.2f", active.getTotalCost()));
            returnBtn.setDisable(false);
            returnStationCombo.setDisable(false);
            reportDamageCheck.setDisable(false);
        } else {
            activeBikeNameLabel.setText("No Active Ride");
            activeStatusLabel.setText("You do not currently have any rented cycles.");
            activeDurationTimerLabel.setVisible(false);
            activeTimeLabel.setText("Browse 'Available Cycles' tab to reserve an eco-friendly campus bike.");
            activeCostLabel.setText("");
            returnBtn.setDisable(true);
            returnStationCombo.setDisable(true);
            reportDamageCheck.setDisable(true);
        }

        // Tables
        historyTable.setItems(FXCollections.observableArrayList(rentalService.getUserRentals(user.getId())));
        walletTable.setItems(FXCollections.observableArrayList(walletService.getTransactionHistory(user.getId())));
    }

    private void applyCycleFilter() {
        User user = authService.getCurrentUser();
        List<Cycle> cycles = cycleDao.findAll();
        String selectedType = typeFilterCombo != null ? typeFilterCombo.getValue() : "All Types";
        String selectedStation = stationFilterCombo != null ? stationFilterCombo.getValue() : "All Stations";
        String search = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        cyclesContainer.getChildren().clear();

        for (Cycle c : cycles) {
            boolean matchesType = selectedType.equals("All Types") || c.getType().getLabel().equals(selectedType);
            boolean matchesStation = selectedStation.equals("All Stations") || c.getStationName().equals(selectedStation);
            boolean matchesSearch = search.isEmpty() ||
                c.getModel().toLowerCase().contains(search) ||
                c.getBrand().toLowerCase().contains(search) ||
                c.getStationName().toLowerCase().contains(search);

            if (matchesType && matchesStation && matchesSearch) {
                CycleCard card = new CycleCard(c, user, this::openBookingModal);
                cyclesContainer.getChildren().add(card);
            }
        }
    }

    /**
     * Modal dialog for reserving a cycle.
     */
    private void openBookingModal(Cycle cycle) {
        User user = authService.getCurrentUser();

        if (rentalService.getActiveRentalForUser(user.getId()).isPresent()) {
            showAlert(Alert.AlertType.WARNING, "Active Ride Exists", "You already have an active cycle checkout. Return it before booking another.");
            return;
        }

        VBox modal = new VBox(16);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(24));
        modal.setAlignment(Pos.CENTER);
        modal.setMaxWidth(440);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 16, 0, 0, 4);");

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

        Label discountLabel = new Label(user.getPricingStrategy().getStrategyName());
        discountLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        Label finalPriceLabel = new Label();
        finalPriceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");

        Runnable updateCost = () -> {
            int hrs = hoursSpinner.getValue();
            double cost = cycle.calculateCost(hrs, user.getPricingStrategy());
            finalPriceLabel.setText(String.format("$%.2f", cost));
        };
        hoursSpinner.valueProperty().addListener((obs, oldV, newV) -> updateCost.run());
        updateCost.run();

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Trip destination (e.g. Library to Science complex)");
        notesArea.setPrefRowCount(2);

        grid.add(new Label("Current Dock:"), 0, 0);
        grid.add(new Label(cycle.getStationName()), 1, 0);

        grid.add(new Label("Duration (Hours):"), 0, 1);
        grid.add(hoursSpinner, 1, 1);

        grid.add(new Label("Discount Tier:"), 0, 2);
        grid.add(discountLabel, 1, 2);

        grid.add(new Label("Payment Method:"), 0, 3);
        grid.add(payCombo, 1, 3);

        grid.add(new Label("Total Estimated Cost:"), 0, 4);
        grid.add(finalPriceLabel, 1, 4);

        grid.add(new Label("Trip Note:"), 0, 5);
        grid.add(notesArea, 1, 5);

        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");

        Button confirm = new Button("Confirm & Unlock Cycle");
        confirm.getStyleClass().add("btn-primary");

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);");
        overlay.getChildren().add(modal);

        cancel.setOnAction(e -> rootStack.getChildren().remove(overlay));

        confirm.setOnAction(e -> {
            try {
                int hours = hoursSpinner.getValue();
                rentalService.rentCycle(user, cycle, hours, payCombo.getValue(), notesArea.getText());
                rootStack.getChildren().remove(overlay);
                showAlert(Alert.AlertType.INFORMATION, "Cycle Unlocked",
                    "Cycle " + cycle.getModel() + " is ready to ride for " + hours + " hr(s). Have a safe trip!");
                refreshAllData();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Checkout Error", ex.getMessage());
            }
        });

        btnBar.getChildren().addAll(cancel, confirm);
        modal.getChildren().addAll(modalTitle, grid, btnBar);
        rootStack.getChildren().add(overlay);
    }

    /**
     * Modal dialog to top up Campus Pay wallet balance.
     */
    private void openTopUpModal() {
        User user = authService.getCurrentUser();

        VBox modal = new VBox(16);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(24));
        modal.setAlignment(Pos.CENTER);
        modal.setMaxWidth(380);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 16, 0, 0, 4);");

        Label title = new Label("Top Up Campus Pay Wallet");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label desc = new Label("Add prepaid balance for instant contactless cycle unlocking.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER);
        Button b10 = new Button("+$10");
        Button b25 = new Button("+$25");
        Button b50 = new Button("+$50");
        b10.getStyleClass().add("btn-secondary");
        b25.getStyleClass().add("btn-secondary");
        b50.getStyleClass().add("btn-secondary");

        TextField amountField = new TextField("25.00");
        amountField.setPromptText("Enter amount ($)");
        amountField.setMaxWidth(200);

        b10.setOnAction(e -> amountField.setText("10.00"));
        b25.setOnAction(e -> amountField.setText("25.00"));
        b50.setOnAction(e -> amountField.setText("50.00"));
        presetBox.getChildren().addAll(b10, b25, b50);

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("bKash / Mobile Wallet", "University Student ID Card", "Credit / Debit Card");
        methodCombo.setValue("bKash / Mobile Wallet");
        methodCombo.setMaxWidth(220);

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");

        Button submit = new Button("Confirm Deposit");
        submit.getStyleClass().add("btn-success");

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);");
        overlay.getChildren().add(modal);

        cancel.setOnAction(e -> rootStack.getChildren().remove(overlay));

        submit.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(amountField.getText().trim());
                if (amt <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Please enter an amount greater than 0.");
                    return;
                }
                boolean ok = walletService.topUpBalance(user.getId(), amt, "Deposit via " + methodCombo.getValue());
                if (ok) {
                    rootStack.getChildren().remove(overlay);
                    showAlert(Alert.AlertType.INFORMATION, "Deposit Successful",
                        String.format("Successfully added $%.2f to your Campus Pay wallet.", amt));
                    refreshAllData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Deposit Failed", "Could not process wallet top-up.");
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid numeric amount.");
            }
        });

        btnBar.getChildren().addAll(cancel, submit);
        modal.getChildren().addAll(title, desc, presetBox, new Label("Deposit Amount ($):"), amountField, new Label("Payment Method:"), methodCombo, btnBar);
        rootStack.getChildren().add(overlay);
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
