package com.campuscycle.ui.view;

import com.campuscycle.dao.CycleDao;
import com.campuscycle.dao.RentalDao;
import com.campuscycle.model.*;
import com.campuscycle.service.*;
import com.campuscycle.ui.NavigationManager;
import com.campuscycle.ui.view.components.StatCard;
import com.campuscycle.ui.view.components.WeatherWidget;
import com.campuscycle.ui.view.components.DashboardLayout;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Owner Fleet Management Portal.
 * Owners can list, edit, and delete their own cycles,
 * view rentals of their cycles, and track their earnings.
 */
public class OwnerDashboardView {

    private final AuthService authService;
    private final CycleDao cycleDao;
    private final RentalDao rentalDao;
    private final MaintenanceService maintenanceService;

    private final StackPane rootStack;
    private final BorderPane mainLayout;

    private StatCard myCyclesCard;
    private StatCard availableCard;
    private StatCard activeRentalsCard;
    private StatCard earningsCard;

    private final TableView<Cycle>  cycleTable  = new TableView<>();
    private final TableView<Rental> rentalTable = new TableView<>();
    private final TableView<MaintenanceTicket> ticketTable = new TableView<>();

    public OwnerDashboardView() {
        this.authService        = AuthService.getInstance();
        this.cycleDao           = new CycleDao();
        this.rentalDao          = new RentalDao();
        this.maintenanceService = new MaintenanceService();

        this.rootStack  = new StackPane();
        this.mainLayout = new BorderPane();

        buildUI();
        refreshAllData();
    }

    private void buildUI() {
        User owner = authService.getCurrentUser();
        if (owner == null || owner.getRole() != UserRole.OWNER) {
            Platform.runLater(() -> NavigationManager.getInstance().showLoginView());
            return;
        }

        // ── Header ───────────────────────────────────────────────────
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        VBox brand = new VBox(2);
        Label title = new Label("🚲 CampusCycle · Owner Portal");
        title.getStyleClass().add("brand-title");
        Label sub = new Label("Welcome, " + owner.getFullName() + " — manage your cycle listings");
        sub.getStyleClass().add("brand-subtitle");
        brand.getChildren().addAll(title, sub);

        HBox right = new HBox(14);
        right.setAlignment(Pos.CENTER_RIGHT);
        WeatherWidget weather = new WeatherWidget();
        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> { authService.logout(); NavigationManager.getInstance().showLoginView(); });
        right.getChildren().addAll(weather, logoutBtn);

        header.setLeft(brand);
        header.setRight(right);
        mainLayout.setTop(header);

        // ── KPI Cards ────────────────────────────────────────────────
        myCyclesCard     = new StatCard("My Cycles",     "0",     "#2563eb");
        availableCard    = new StatCard("Available",     "0",     "#10b981");
        activeRentalsCard= new StatCard("Active Rentals","0",     "#f59e0b");
        earningsCard     = new StatCard("My Earnings",   "৳0.00", "#7c3aed");

        HBox statsBar = new HBox(14);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        for (StatCard sc : new StatCard[]{myCyclesCard, availableCard, activeRentalsCard, earningsCard}) {
            sc.prefWidthProperty().bind(statsBar.widthProperty().subtract(42).divide(4));
            statsBar.getChildren().add(sc);
        }

        HBox dashboard = DashboardLayout.create(List.of(
            new Tab("🚲 My Cycles",       buildMyCyclesTab()),
            new Tab("📋 Rental Activity", buildRentalsTab()),
            new Tab("🛠️ Maintenance",    buildMaintenanceTab())
        ));

        VBox center = new VBox(14, statsBar, dashboard);
        center.setPadding(new Insets(14, 20, 14, 20));
        VBox.setVgrow(dashboard, Priority.ALWAYS);
        mainLayout.setCenter(center);
        rootStack.getChildren().add(mainLayout);
    }

    // ── MY CYCLES TAB ────────────────────────────────────────────────
    private Parent buildMyCyclesTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        User owner = authService.getCurrentUser();

        Button addBtn = new Button("➕ Add Cycle");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openCycleModal(owner.getId()));

        Button deleteBtn = new Button("🗑️ Remove Listing");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Cycle sel = cycleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a cycle."); return; }
            if (sel.getStatus() == CycleStatus.RENTED) { warn("Cannot remove a cycle that is currently rented."); return; }
            confirm("Remove " + sel.getDisplayName() + " from your listing?", () -> {
                cycleDao.delete(sel.getId());
                refreshAllData();
            });
        });

        Button maintBtn = new Button("🛠️ Flag for Maintenance");
        maintBtn.getStyleClass().add("btn-secondary");
        maintBtn.setOnAction(e -> {
            Cycle sel = cycleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("Select a cycle."); return; }
            cycleDao.updateStatus(sel.getId(), CycleStatus.MAINTENANCE, sel.getStationId());
            info(sel.getModel() + " flagged for maintenance.");
            refreshAllData();
        });

        HBox toolbar = new HBox(10, addBtn, deleteBtn, maintBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        pane.setTop(toolbar);

        // Table columns
        TableColumn<Cycle, String> cName = new TableColumn<>("Display Name");
        cName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDisplayName()));
        cName.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.24));

        TableColumn<Cycle, String> cType = new TableColumn<>("Type");
        cType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLabel()));
        cType.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.14));

        TableColumn<Cycle, String> cRate = new TableColumn<>("Rate/hr");
        cRate.setCellValueFactory(c -> new SimpleStringProperty(String.format("৳%.2f", c.getValue().getHourlyRate())));
        cRate.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.10));

        TableColumn<Cycle, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getStatusText()));
        cStatus.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.14));

        TableColumn<Cycle, String> cLocation = new TableColumn<>("Location");
        cLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        cLocation.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.18));

        TableColumn<Cycle, Integer> cRides = new TableColumn<>("Rides");
        cRides.setCellValueFactory(new PropertyValueFactory<>("totalRides"));
        cRides.prefWidthProperty().bind(cycleTable.widthProperty().multiply(0.08));

        cycleTable.getColumns().addAll(cName, cType, cRate, cStatus, cLocation, cRides);
        cycleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pane.setCenter(cycleTable);
        return pane;
    }

    private void openCycleModal(int ownerId) {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        ComboBox<CycleType> typeC = new ComboBox<>(FXCollections.observableArrayList(CycleType.STANDARD, CycleType.GEARED));
        typeC.setValue(CycleType.STANDARD);
        TextField displayNameF = new TextField();
        displayNameF.setPromptText("Enter a unique cycle name");
        TextField rateF = new TextField("15.0");
        List<String> locations = cycleDao.findAll().stream()
            .map(Cycle::getLocation)
            .filter(location -> location != null && !location.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        ComboBox<String> locC = new ComboBox<>(FXCollections.observableArrayList(locations));
        locC.setEditable(true);
        locC.setPromptText("Choose or enter a campus location");
        if (!locC.getItems().stream().anyMatch(location -> location.equalsIgnoreCase("Central Library"))) {
            locC.getItems().add("Central Library");
        }
        locC.setValue("Central Library");

        grid.add(new Label("Display Name:"), 0, 0); grid.add(displayNameF, 1, 0);
        grid.add(new Label("Type:"),         0, 1); grid.add(typeC,        1, 1);
        grid.add(new Label("Rate (৳/hr):"),  0, 2); grid.add(rateF,        1, 2);
        grid.add(new Label("Location:"),     0, 3); grid.add(locC,         1, 3);

        Label titleLbl = new Label("List a New Cycle");
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("btn-secondary");
        Button saveBtn   = new Button("Save");   saveBtn.getStyleClass().add("btn-primary");
        HBox btnBar = new HBox(10, cancelBtn, saveBtn); btnBar.setAlignment(Pos.CENTER_RIGHT);

        VBox modal = new VBox(14, titleLbl, grid, btnBar);
        modal.getStyleClass().add("card");
        modal.setPadding(new Insets(22));
        modal.setMaxWidth(420);
        modal.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.3),16,0,0,4);");

        StackPane overlay = createOverlay(modal);
        cancelBtn.setOnAction(e -> rootStack.getChildren().remove(overlay));
        saveBtn.setOnAction(e -> {
            try {
                String displayName = displayNameF.getText().trim();
                if (displayName.isEmpty()) {
                    warn("Enter a display name for the cycle.");
                    return;
                }
                boolean duplicateName = cycleDao.findAll().stream()
                    .anyMatch(cycle -> cycle.getDisplayName().equalsIgnoreCase(displayName));
                if (duplicateName) {
                    warn("That display name is already in use. Please change the name.");
                    return;
                }
                double rate = Double.parseDouble(rateF.getText().trim());
                String loc  = locC.getEditor().getText().trim();
                if (loc.isEmpty() && locC.getValue() != null) loc = locC.getValue().trim();
                if (loc.isEmpty()) loc = "Campus Core";

                Cycle cycle = new Cycle(0, displayName, "", typeC.getValue(), rate,
                    CycleStatus.AVAILABLE, loc, -1, ownerId);
                cycleDao.save(cycle);
                rootStack.getChildren().remove(overlay);
                refreshAllData();
            } catch (NumberFormatException ex) {
                warn("Rate must be a valid number.");
            }
        });
        rootStack.getChildren().add(overlay);
    }

    // ── RENTAL ACTIVITY TAB ─────────────────────────────────────────
    private Parent buildRentalsTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        Label notice = new Label("📊 Showing rentals of YOUR cycles only");
        notice.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 0 0 8 0;");

        TableColumn<Rental, String> rRider = new TableColumn<>("Rider");
        rRider.setCellValueFactory(new PropertyValueFactory<>("userName"));
        rRider.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.18));

        TableColumn<Rental, String> rCycle = new TableColumn<>("Cycle");
        rCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        rCycle.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.22));

        TableColumn<Rental, String> rStart = new TableColumn<>("Start Time");
        rStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        rStart.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.16));

        TableColumn<Rental, String> rEnd = new TableColumn<>("Return Time");
        rEnd.setCellValueFactory(r -> new SimpleStringProperty(
            r.getValue().getEndTime() != null ? r.getValue().getEndTime() : "⚡ Active"
        ));
        rEnd.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.16));

        TableColumn<Rental, String> rCost = new TableColumn<>("Earned (৳)");
        rCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("৳%.2f", r.getValue().getTotalCost())));
        rCost.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.12));

        TableColumn<Rental, String> rStat = new TableColumn<>("Status");
        rStat.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getStatus().name()));
        rStat.prefWidthProperty().bind(rentalTable.widthProperty().multiply(0.10));

        rentalTable.getColumns().addAll(rRider, rCycle, rStart, rEnd, rCost, rStat);
        rentalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox content = new VBox(6, notice, rentalTable);
        VBox.setVgrow(rentalTable, Priority.ALWAYS);
        pane.setCenter(content);
        return pane;
    }

    // ── MAINTENANCE TAB ──────────────────────────────────────────────
    private Parent buildMaintenanceTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        Label notice = new Label("🛠️ Maintenance tickets for YOUR cycles");
        notice.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 0 0 8 0;");

        TableColumn<MaintenanceTicket, String> tCycle = new TableColumn<>("Cycle");
        tCycle.setCellValueFactory(new PropertyValueFactory<>("cycleName"));
        tCycle.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.24));

        TableColumn<MaintenanceTicket, String> tCat = new TableColumn<>("Issue");
        tCat.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getIssueCategory().getLabel()));
        tCat.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.22));

        TableColumn<MaintenanceTicket, String> tStat = new TableColumn<>("Status");
        tStat.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getStatus().getLabel()));
        tStat.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.14));

        TableColumn<MaintenanceTicket, String> tDate = new TableColumn<>("Reported At");
        tDate.setCellValueFactory(new PropertyValueFactory<>("reportedAt"));
        tDate.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.16));

        TableColumn<MaintenanceTicket, String> tCost = new TableColumn<>("Repair (৳)");
        tCost.setCellValueFactory(t -> new SimpleStringProperty(String.format("৳%.2f", t.getValue().getRepairCost())));
        tCost.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.10));

        TableColumn<MaintenanceTicket, String> tNotes = new TableColumn<>("Technician Notes");
        tNotes.setCellValueFactory(t -> new SimpleStringProperty(
            t.getValue().getTechnicianNotes() != null ? t.getValue().getTechnicianNotes() : "—"
        ));
        tNotes.prefWidthProperty().bind(ticketTable.widthProperty().multiply(0.08));

        ticketTable.getColumns().addAll(tCycle, tCat, tStat, tDate, tCost, tNotes);
        ticketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox content = new VBox(6, notice, ticketTable);
        VBox.setVgrow(ticketTable, Priority.ALWAYS);
        pane.setCenter(content);
        return pane;
    }

    // ── REFRESH ─────────────────────────────────────────────────────
    private void refreshAllData() {
        User owner = authService.getCurrentUser();
        if (owner == null) return;
        int ownerId = owner.getId();

        List<Cycle> myCycles = cycleDao.findByOwner(ownerId);
        long available  = myCycles.stream().filter(c -> c.getStatus() == CycleStatus.AVAILABLE).count();
        long activeRent = myCycles.stream().filter(c -> c.getStatus() == CycleStatus.RENTED).count();

        // Earnings: sum all rental costs for this owner's cycles
        List<Integer> myIds = myCycles.stream().map(Cycle::getId).toList();
        double earnings = rentalDao.findAll().stream()
            .filter(r -> myIds.contains(r.getCycleId()) && r.getStatus() == RentalStatus.COMPLETED)
            .mapToDouble(Rental::getTotalCost).sum();

        myCyclesCard.setValue(String.valueOf(myCycles.size()));
        availableCard.setValue(String.valueOf(available));
        activeRentalsCard.setValue(String.valueOf(activeRent));
        earningsCard.setValue(String.format("৳%.2f", earnings));
        cycleTable.setItems(FXCollections.observableArrayList(myCycles));

        // Rentals for my cycles only
        List<Rental> myRentals = rentalDao.findAll().stream()
            .filter(r -> myIds.contains(r.getCycleId())).toList();
        rentalTable.setItems(FXCollections.observableArrayList(myRentals));

        // Maintenance tickets for my cycles only
        List<MaintenanceTicket> myTickets = maintenanceService.getAllTickets().stream()
            .filter(t -> myIds.contains(t.getCycleId())).toList();
        ticketTable.setItems(FXCollections.observableArrayList(myTickets));
    }

    // ── HELPERS ──────────────────────────────────────────────────────
    private StackPane createOverlay(VBox modal) {
        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.45);");
        return overlay;
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void confirm(String msg, Runnable onYes) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> onYes.run());
    }

    public Parent getView() { return rootStack; }
}
