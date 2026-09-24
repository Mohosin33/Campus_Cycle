package com.campuscycle.ui.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Metric card displaying system KPI statistics.
 */
public class StatCard extends VBox {
    private final Label valueLabel;
    private final Label titleLabel;

    public StatCard(String title, String initialValue, String accentColor) {
        setSpacing(4);
        setPadding(new Insets(14, 18, 14, 18));
        getStyleClass().add("card");
        setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-value");
        if (accentColor != null) {
            valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");
        }

        getChildren().addAll(titleLabel, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
