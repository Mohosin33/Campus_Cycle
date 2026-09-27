package com.campuscycle.ui.view;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.dao.UserDao;
import com.campuscycle.model.*;
import com.campuscycle.service.*;
import com.campuscycle.ui.NavigationManager;
import com.campuscycle.ui.view.components.StatCard;
import com.campuscycle.ui.view.components.WeatherWidget;
import com.campuscycle.ui.view.components.DashboardLayout;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Enterprise Fleet Operations and Administration Console.
 * Provides CRUD for cycles, user governance, maintenance work orders,
 * and multi-threaded audit export.
 */
public class AdminDashboardView {

    // ---- Services / DAOs ----
    private final AuthService authService;
    private final CycleDao cycleDao;
    private final UserDao userDao;
    private final RentalDao rentalDao;
    private final ReportService reportService;
    private final MaintenanceService maintenanceService;
    private final WalletService walletService;

    // ---- Root layout ----
    private final StackPane rootStack;
    private final BorderPane mainLayout;

    // ---- KPI cards ----
    private StatCard totalFleetCard;
    private StatCard availableCard;
    private StatCard activeRentalsCard;
    private StatCard revenueCard;

    // ---- Tables ----
    private final TableView<Cycle> cycleTable   = new TableView<>();
    private final TableView<User>  userTable    = new TableView<>();
    private final TableView<Rental> rentalTable = new TableView<>();
    private final TableView<MaintenanceTicket> ticketTable = new TableView<>();

    // ============================================================
    //  Constructor
    // ============================================================
    public AdminDashboardView() {
        this.authService        = AuthService.getInstance();
        this.cycleDao           = new CycleDao();
        this.userDao            = new UserDao();
        this.rentalDao          = new RentalDao();
        this.reportService      = new ReportService();
        this.maintenanceService = new MaintenanceService();
        this.walletService      = new WalletService();

        this.rootStack  = new StackPane();
        this.mainLayout = new BorderPane();

        buildUI();
        refreshAllData();
    }

    // ============================================================
    //  UI Construction
    // ============================================================
    private void buildUI() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            Platform.runLater(() -> NavigationManager.getInstance().showLoginView());
            return;
        }

        // ---------- Header ----------
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        VBox brand = new VBox(2);
        Label brandTitle = new Label("🚲 CampusCycle · Campus Operations Console");
        brandTitle.getStyleClass().add("brand-title");
        Label brandSub = new Label("Administrator: " + currentUser.getFullName() + " (" + currentUser.getEmail() + ")");
        brandSub.getStyleClass().add("brand-subtitle");
        brand.getChildren().addAll(brandTitle, brandSub);

        HBox headerRight = new HBox(14);
        headerRight.setAlignment(Pos.CENTER_RIGHT);
        WeatherWidget weather = new WeatherWidget();
        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> { authService.logout(); NavigationManager.getInstance().showLoginView(); });
        headerRight.getChildren().addAll(weather, logoutBtn);

        header.setLeft(brand);
        header.setRight(headerRight);
        mainLayout.setTop(header);

        // ---------- KPI stat cards ----------
        totalFleetCard    = new StatCard("Total Cycles",  "0",     "#2563eb");
        availableCard     = new StatCard("Available Now", "0",     "#10b981");
        activeRentalsCard = new StatCard("Active Rides",  "0",     "#f59e0b");
        revenueCard       = new StatCard("Total Income", "৳0.00", "#7c3aed");

        HBox statsBar = new HBox(14);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        for (StatCard sc : new StatCard[]{totalFleetCard, availableCard, activeRentalsCard, revenueCard}) {
            sc.prefWidthProperty().bind(statsBar.widthProperty().subtract(42).divide(4));
            statsBar.getChildren().add(sc);
        }

        cycleTable.setPrefHeight(260);
        userTable.setPrefHeight(260);
        rentalTable.setPrefHeight(260);
        ticketTable.setPrefHeight(260);

        HBox dashboard = DashboardLayout.create(List.of(
            new Tab("Cycle Inventory", createAdminSection("Cycle Inventory", buildFleetTab())),
            new Tab("User Accounts", createAdminSection("User Accounts", buildUsersTab())),
            new Tab("Rental Activity", createAdminSection("Rental Activity", buildRentalsTab())),
            new Tab("Maintenance", createAdminSection("Maintenance", buildMaintenanceTab()))
        ));

        VBox center = new VBox(14, statsBar, dashboard);
        center.setPadding(new Insets(14, 20, 14, 20));
        VBox.setVgrow(dashboard, Priority.ALWAYS);
        mainLayout.setCenter(center);
        rootStack.getChildren().add(mainLayout);
    }

    private VBox createAdminSection(String title, Parent content) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox section = new VBox(8, heading, content);
        section.setFillWidth(true);
        return section;
    }

    // ============================================================
    //  FLEET MANAGEMENT TAB
    // ============================================================
    private Parent buildFleetTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        // Toolbar
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Cycle sel = cycleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a cycle to delete."); return; }
            confirm("Delete cycle " + sel.getModel() + "?", () -> {
                cycleDao.delete(sel.getId());
                refreshAllData();
            });
        });

        Button maintBtn = new Button("🛠️ Flag Maintenance");
        maintBtn.getStyleClass().add("btn-secondary");
        maintBtn.setOnAction(e -> {
            Cycle sel = cycleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a cycle first."); return; }
            cycleDao.updateLocationAndStatus(sel.getId(), CycleStatus.MAINTENANCE, sel.getLocation());
            info(sel.getModel() + " flagged for maintenance.");
            refreshAllData();
        });

        TextField search = new TextField();
        search.setPromptText("Search model / brand / location…");
        search.textProperty().addListener((obs, o, n) -> filterCycles(n));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshAllData());

        HBox toolbar = new HBox(10, deleteBtn, maintBtn, spacer, search, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        pane.setTop(toolbar);

        // Columns
        TableColumn<Cycle, String> cBrand = new TableColumn<>("Display Name");
        cBrand.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDisplayName()));
        cBrand.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.22));

        TableColumn<Cycle, String> cType = new TableColumn<>("Type");
        cType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLabel()));
        cType.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.15));

        TableColumn<Cycle, String> cRate = new TableColumn<>("৳/hr");
        cRate.setCellValueFactory(c -> new SimpleStringProperty(String.format("৳%.2f", c.getValue().getHourlyRate())));
        cRate.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.09));

        TableColumn<Cycle, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getStatusText()));
        cStatus.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.12));

        TableColumn<Cycle, String> cLoc = cycleCol("Location", "location");
        cLoc.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.18));

        TableColumn<Cycle, Integer> cRides = cycleCol("Rides", "totalRides");
        cRides.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.09));

        cycleTable.getColumns().addAll(cBrand, cType, cRate, cStatus, cLoc, cRides);
        cycleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(cycleTable);
        return pane;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Cycle, T> cycleCol(String title, String prop) {
        TableColumn<Cycle, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }

    private void filterCycles(String q) {
        if (q == null || q.isBlank()) {
            cycleTable.setItems(FXCollections.observableArrayList(cycleDao.findAll()));
            return;
        }
        String lq = q.toLowerCase();
        cycleTable.setItems(FXCollections.observableArrayList(
            cycleDao.findAll().stream().filter(c ->
                c.getModel().toLowerCase().contains(lq) ||
                c.getBrand().toLowerCase().contains(lq) ||
                c.getLocation().toLowerCase().contains(lq) ||
                c.getType().name().toLowerCase().contains(lq)
            ).toList()
        ));
    }

    // ============================================================
    //  USER ACCOUNTS TAB
    // ============================================================
    private Parent buildUsersTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        Button walletBtn = new Button("💳 Adjust Wallet");
        walletBtn.getStyleClass().add("btn-secondary");
        walletBtn.setOnAction(e -> {
            User sel = userTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a user."); return; }
            openAdminWalletModal(sel);
        });

        Button deleteBtn = new Button("🗑️ Delete Account");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            User sel = userTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a user."); return; }
            if (sel.getId() == authService.getCurrentUser().getId()) { warn("Cannot delete your own account."); return; }
            confirm("Permanently delete " + sel.getFullName() + "?", () -> { userDao.delete(sel.getId()); refreshAllData(); });
        });

        HBox toolbar = new HBox(10, walletBtn, deleteBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        pane.setTop(toolbar);

        // Columns
        TableColumn<User, String> uName = new TableColumn<>("Full Name");
        uName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        uName.prefWidthProperty().bind(userTable.widthProperty().multiply(0.18));

        TableColumn<User, String> uUser = new TableColumn<>("Username");
        uUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        uUser.prefWidthProperty().bind(userTable.widthProperty().multiply(0.12));

        TableColumn<User, String> uRole = new TableColumn<>("Role");
        uRole.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getRole().getDisplayName()));
        uRole.prefWidthProperty().bind(userTable.widthProperty().multiply(0.12));

        TableColumn<User, String> uEmail = new TableColumn<>("Email");
        uEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        uEmail.prefWidthProperty().bind(userTable.widthProperty().multiply(0.22));

        TableColumn<User, String> uWallet = new TableColumn<>("Wallet (৳)");
        uWallet.setCellValueFactory(u -> new SimpleStringProperty(String.format("৳%.2f", u.getValue().getWalletBalance())));
        uWallet.prefWidthProperty().bind(userTable.widthProperty().multiply(0.10));

        TableColumn<User, String> uStatus = new TableColumn<>("Status");
        uStatus.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().isActive() ? "✅ Active" : "⛔ Suspended"));
        uStatus.prefWidthProperty().bind(userTable.widthProperty().multiply(0.10));

        userTable.getColumns().addAll(uName, uUser, uRole, uEmail, uWallet, uStatus);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(userTable);
        return pane;
    }

    private void openAdminWalletModal(User target) {
        ComboBox<String> actionC = new ComboBox<>();
        actionC.getItems().addAll("Add Credit", "Deduct Amount (Fine/Override)");
        actionC.setValue("Add Credit");

        TextField amtF    = new TextField(); amtF.setPromptText("Amount in Taka");
        TextField reasonF = new TextField(); reasonF.setPromptText("Reason / Reference");

        Label curBal = new Label(String.format("Current Balance: ৳%.2f", target.getWalletBalance()));
        curBal.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("btn-secondary");
        Button applyBtn  = new Button("Apply");  applyBtn.getStyleClass().add("btn-primary");
        HBox btnBar = new HBox(10, cancelBtn, applyBtn); btnBar.setAlignment(Pos.CENTER_RIGHT);

        Label titleLbl = new Label("Adjust Wallet — " + target.getFullName());
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox modal = new VBox(10, titleLbl, curBal,
            new Label("Action:"), actionC,
            new Label("Amount (৳):"), amtF,
            new Label("Reason:"), reasonF, btnBar);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(22));
        modal.setMaxWidth(360);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.3),16,0,0,4);");

        StackPane overlay = createOverlay(modal);
        cancelBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));
        applyBtn.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(amtF.getText().trim());
                if (amt <= 0) { warn("Amount must be positive."); return; }
                String reason = reasonF.getText().trim().isEmpty() ? "Admin adjustment" : "Admin: " + reasonF.getText().trim();
                boolean ok;
                if (actionC.getValue().startsWith("Add")) {
                    ok = walletService.topUpBalance(target.getId(), amt, reason);
                } else {
                    ok = walletService.deductBalance(target.getId(), amt, WalletTransaction.TransactionType.OVERDUE_FINE, reason);
                }
                rootStack.getChildren().remove(overlay);
                if (ok) info(String.format("৳%.2f adjustment applied to %s.", amt, target.getFullName()));
                else    warn("Adjustment failed — insufficient balance or user not found.");
                refreshAllData();
            } catch (NumberFormatException ex) {
                warn("Enter a valid numeric amount.");
            }
        });
        rootStack.getChildren().add(overlay);
    }

    // ============================================================
    //  RENTAL LEDGER TAB
    // ============================================================
    private Parent buildRentalsTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        TableColumn<Rental, String> rUser = new TableColumn<>("User");
        rUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        rUser.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.16));

        TableColumn<Rental, String> rCycle = new TableColumn<>("Cycle");
        rCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        rCycle.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.20));

        TableColumn<Rental, String> rStart = new TableColumn<>("Start Time");
        rStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        rStart.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.16));

        TableColumn<Rental, String> rEnd = new TableColumn<>("Return Time");
        rEnd.setCellValueFactory(r -> new SimpleStringProperty(
            r.getValue().getEndTime() != null ? r.getValue().getEndTime() : "⚡ Active"
        ));
        rEnd.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.16));

        TableColumn<Rental, String> rCost = new TableColumn<>("Total (৳)");
        rCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("৳%.2f", r.getValue().getTotalCost())));
        rCost.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.10));

        TableColumn<Rental, String> rStat = new TableColumn<>("Status");
        rStat.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getStatus().name()));
        rStat.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.10));

        TableColumn<Rental, Integer> rDur = new TableColumn<>("Hrs");
        rDur.setCellValueFactory(new PropertyValueFactory<>("durationHours"));
        rDur.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.06));

        rentalTable.getColumns().addAll(rUser, rCycle, rStart, rEnd, rCost, rStat, rDur);
        rentalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(rentalTable);
        return pane;
    }

    // ============================================================
    //  MAINTENANCE TICKETS TAB
    // ============================================================
    private Parent buildMaintenanceTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        Button resolveBtn = new Button("✅ Resolve & Re-commission Cycle");
        resolveBtn.getStyleClass().add("btn-success");
        resolveBtn.setOnAction(e -> {
            MaintenanceTicket sel = ticketTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a ticket to resolve."); return; }
            if (sel.getStatus() == MaintenanceTicket.TicketStatus.RESOLVED) { info("Ticket already resolved."); return; }
            openResolveModal(sel);
        });

        Button newTicketBtn = new Button("➕ Create Manual Ticket");
        newTicketBtn.getStyleClass().add("btn-secondary");
        newTicketBtn.setOnAction(e -> openCreateTicketModal());

        HBox toolbar = new HBox(10, resolveBtn, newTicketBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        pane.setTop(toolbar);

        // Columns
        TableColumn<MaintenanceTicket, String> tCycle = new TableColumn<>("Cycle");
        tCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        tCycle.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.22));

        TableColumn<MaintenanceTicket, String> tReporter = new TableColumn<>("Reported By");
        tReporter.setCellValueFactory(new PropertyValueFactory<>("reportedByUserName"));
        tReporter.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.15));

        TableColumn<MaintenanceTicket, String> tCat = new TableColumn<>("Issue");
        tCat.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getIssueCategory().getLabel()));
        tCat.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.20));

        TableColumn<MaintenanceTicket, String> tStat = new TableColumn<>("Status");
        tStat.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getStatus().getLabel()));
        tStat.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.12));

        TableColumn<MaintenanceTicket, String> tDate = new TableColumn<>("Reported At");
        tDate.setCellValueFactory(new PropertyValueFactory<>("reportedAt"));
        tDate.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.15));

        TableColumn<MaintenanceTicket, String> tCost = new TableColumn<>("Repair (৳)");
        tCost.setCellValueFactory(t -> new SimpleStringProperty(String.format("৳%.2f", t.getValue().getRepairCost())));
        tCost.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.10));

        ticketTable.getColumns().addAll(tCycle, tReporter, tCat, tStat, tDate, tCost);
        ticketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(ticketTable);
        return pane;
    }

    private void openResolveModal(MaintenanceTicket ticket) {
        TextField returnLocF = new TextField("Campus Central Plaza");
        returnLocF.setPromptText("Campus location (e.g. Science Complex rack, Library Entrance)");

        TextArea notesA = new TextArea();
        notesA.setPromptText("Technician notes (parts replaced, adjustments)");
        notesA.setPrefRowCount(3);

        TextField costF = new TextField("0.00");

        Label titleLbl = new Label("Resolve Ticket — " + ticket.getCycleName());
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label issueLbl = new Label("Issue: " + ticket.getIssueCategory().getLabel());
        issueLbl.setStyle("-fx-text-fill: #b91c1c;");

        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("btn-secondary");
        Button resolveBtn = new Button("Mark Resolved & Return to Campus"); resolveBtn.getStyleClass().add("btn-primary");
        HBox btnBar = new HBox(10, cancelBtn, resolveBtn); btnBar.setAlignment(Pos.CENTER_RIGHT);

        VBox modal = new VBox(10, titleLbl, issueLbl,
            new Label("Return Location:"), returnLocF,
            new Label("Technician Notes:"), notesA,
            new Label("Repair Cost (৳):"), costF, btnBar);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(22));
        modal.setMaxWidth(430);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.3),16,0,0,4);");

        StackPane overlay = createOverlay(modal);
        cancelBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));
        resolveBtn.setOnAction(e -> {
            try {
                double cost = Double.parseDouble(costF.getText().trim());
                String loc = returnLocF.getText().trim();
                if (loc.isEmpty()) loc = "Campus Central Plaza";
                boolean ok = maintenanceService.resolveTicket(ticket.getId(), notesA.getText(), cost, loc);
                rootStack.getChildren().remove(overlay);
                if (ok) { info("Cycle re-commissioned at " + loc + "."); refreshAllData(); }
                else    warn("Failed to resolve ticket.");
            } catch (NumberFormatException ex) {
                warn("Enter a valid repair cost.");
            }
        });
        rootStack.getChildren().add(overlay);
    }

    private void openCreateTicketModal() {
        List<Cycle> cycles = cycleDao.findAll();
        ComboBox<Cycle> cycleC = new ComboBox<>(FXCollections.observableArrayList(cycles));
        if (!cycles.isEmpty()) cycleC.setValue(cycles.get(0));

        ComboBox<MaintenanceTicket.IssueCategory> catC = new ComboBox<>(
            FXCollections.observableArrayList(MaintenanceTicket.IssueCategory.values()));
        catC.setValue(MaintenanceTicket.IssueCategory.ROUTINE_CHECKUP);

        TextArea descA = new TextArea(); descA.setPromptText("Describe the issue"); descA.setPrefRowCount(2);

        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("btn-secondary");
        Button submitBtn = new Button("Create Ticket"); submitBtn.getStyleClass().add("btn-primary");
        HBox btnBar = new HBox(10, cancelBtn, submitBtn); btnBar.setAlignment(Pos.CENTER_RIGHT);

        Label titleLbl = new Label("Create Maintenance Ticket");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox modal = new VBox(10, titleLbl,
            new Label("Cycle:"), cycleC,
            new Label("Issue Category:"), catC,
            new Label("Description:"), descA, btnBar);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(22));
        modal.setMaxWidth(400);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.3),16,0,0,4);");

        StackPane overlay = createOverlay(modal);
        cancelBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));
        submitBtn.setOnAction(e -> {
            Cycle sel = cycleC.getValue();
            if (sel == null) { warn("Select a cycle."); return; }
            maintenanceService.reportIssue(sel.getId(), authService.getCurrentUser().getId(), catC.getValue(), descA.getText());
            rootStack.getChildren().remove(overlay);
            info("Maintenance ticket created for " + sel.getDisplayName());
            refreshAllData();
        });
        rootStack.getChildren().add(overlay);
    }


    // ============================================================
    //  DATA REFRESH
    // ============================================================
    public void refreshAllData() {
        ReportService.DashboardStats stats = reportService.getQuickStats();
        totalFleetCard.setValue(String.valueOf(stats.totalCycles()));
        availableCard.setValue(String.valueOf(stats.availableCycles()));
        activeRentalsCard.setValue(String.valueOf(stats.activeRentals()));
        revenueCard.setValue(String.format("৳%.2f", stats.totalRevenue()));

        cycleTable.setItems(FXCollections.observableArrayList(cycleDao.findAll()));
        userTable.setItems(FXCollections.observableArrayList(userDao.findAll()));
        rentalTable.setItems(FXCollections.observableArrayList(rentalDao.findAll()));
        ticketTable.setItems(FXCollections.observableArrayList(maintenanceService.getAllTickets()));
    }

    // ============================================================
    //  UTILITY HELPERS
    // ============================================================
    private StackPane createOverlay(VBox modal) {
        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.45);");
        return overlay;
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void confirm(String msg, Runnable onYes) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> onYes.run());
    }

    public Parent getView() {
        return rootStack;
    }
}
