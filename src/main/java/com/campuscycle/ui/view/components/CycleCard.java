package com.campuscycle.ui.view.components;

import com.campuscycle.model.Cycle;
import com.campuscycle.model.CycleStatus;
import com.campuscycle.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Visual card displaying cycle specifications, availability badge, and rental trigger.
 */
public class CycleCard extends VBox {
    private final Cycle cycle;
    private final Consumer<Cycle> onRentAction;

    public CycleCard(Cycle cycle, User currentUser, Consumer<Cycle> onRentAction) {
        this.cycle = cycle;
        this.onRentAction = onRentAction;

        setSpacing(10);
        setPadding(new Insets(14));
        getStyleClass().add("card");
        setPrefWidth(260);
        setMinWidth(240);

        // Header: Display Name + Status Badge
        Label titleLabel = new Label(cycle.getDisplayName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);

        Label statusBadge = new Label(cycle.getStatus().getStatusText());
        switch (cycle.getStatus()) {
            case AVAILABLE:
                statusBadge.getStyleClass().add("badge-available");
                break;
            case RENTED:
                statusBadge.getStyleClass().add("badge-rented");
                break;
            case MAINTENANCE:
            default:
                statusBadge.getStyleClass().add("badge-maintenance");
                break;
        }

        HBox header = new HBox(8, titleLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        header.getChildren().add(statusBadge);

        // Cycle type
        String typeDesc = "🚲 " + cycle.getType().getLabel();
        Label typeLabel = new Label(typeDesc);
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        // Campus Location
        Label locationLabel = new Label("📍 " + cycle.getLocation());
        locationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        double effectiveRate = cycle.calculateCost(1, currentUser.getPricingStrategy());
        Label priceLabel = new Label(String.format("৳%.2f / hr", effectiveRate));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");
        getChildren().addAll(header, typeLabel, locationLabel, priceLabel);

        // Action Button
        Button rentBtn = new Button("Reserve & Rent");
        rentBtn.getStyleClass().add("btn-primary");
        rentBtn.setMaxWidth(Double.MAX_VALUE);
        rentBtn.setDisable(!cycle.isAvailable());

        rentBtn.setOnAction(e -> {
            if (onRentAction != null) {
                onRentAction.accept(cycle);
            }
        });

        getChildren().add(rentBtn);
    }

    public Cycle getCycle() {
        return cycle;
    }
}
