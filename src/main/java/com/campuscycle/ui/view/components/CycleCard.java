package com.campuscycle.ui.view.components;

import com.campuscycle.model.Cycle;
import com.campuscycle.model.CycleStatus;
import com.campuscycle.model.CycleType;
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

        // Header: Brand & Model + Status Badge
        Label titleLabel = new Label(cycle.getBrand() + " " + cycle.getModel());
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

        // Type & Battery Info
        String typeDesc = "🚲 " + cycle.getType().getLabel();
        if (cycle.getType() == CycleType.ELECTRIC && cycle.getBatteryPercentage() >= 0) {
            typeDesc += " (⚡ " + cycle.getBatteryPercentage() + "%)";
        }
        Label typeLabel = new Label(typeDesc);
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        // Station Dock Location
        Label stationLabel = new Label("📍 " + cycle.getStationName());
        stationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        // Price display (showing effective rate with user discount if applicable)
        double baseRate = cycle.getHourlyRate();
        double effectiveRate = cycle.calculateCost(1, currentUser.getPricingStrategy());
        String priceText = String.format("$%.2f / hr", effectiveRate);
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");

        if (effectiveRate < baseRate) {
            Label discountNote = new Label(String.format("($%.2f base)", baseRate));
            discountNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-strikethrough: true;");
            HBox priceBox = new HBox(6, priceLabel, discountNote);
            priceBox.setAlignment(Pos.BASELINE_LEFT);
            getChildren().addAll(header, typeLabel, stationLabel, priceBox);
        } else {
            getChildren().addAll(header, typeLabel, stationLabel, priceLabel);
        }

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
