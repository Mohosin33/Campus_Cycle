package com.campuscycle.ui.view.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public final class DashboardLayout {
    private DashboardLayout() {}

    public static HBox create(List<Tab> tabs) {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("dashboard-sidebar");
        sidebar.setPrefWidth(245);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(260);

        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("dashboard-sidebar-heading");
        sidebar.getChildren().add(heading);

        StackPane content = new StackPane();
        content.getStyleClass().add("dashboard-main-content");
        content.setMinWidth(0);

        List<Button> buttons = new ArrayList<>();
        for (Tab tab : tabs) {
            Button button = new Button(tab.getText());
            button.getStyleClass().add("dashboard-nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(44);
            button.setWrapText(false);
            button.setOnAction(event -> {
                content.getChildren().setAll(tab.getContent());
                buttons.forEach(item -> item.getStyleClass().remove("active"));
                button.getStyleClass().add("active");
            });
            buttons.add(button);
            sidebar.getChildren().add(button);
        }

        if (!tabs.isEmpty()) {
            content.getChildren().setAll(tabs.get(0).getContent());
            buttons.get(0).getStyleClass().add("active");
        }

        HBox layout = new HBox(sidebar, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        layout.setMinWidth(0);
        return layout;
    }
}