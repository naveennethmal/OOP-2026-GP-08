package Admin;

import Common.PasswordResetRequests;
import Common.PasswordResetRequests.Request;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public class PasswordRequestsView extends VBox {
    private final TableView<Request> table = new TableView<>();
    private final Label message = new Label();
    private final TextField search = new TextField();
    private List<Request> allRequests = List.of();

    public PasswordRequestsView() {
        getStyleClass().add("page");
        setSpacing(16);

        Label title = new Label("Lost-password requests");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Students can send their username to the administrator if they lose their password.");
        subtitle.getStyleClass().add("page-sub");
        search.setPromptText("Search username, message or status");
        search.setPrefWidth(360);
        search.setMinWidth(260);
        search.setPrefHeight(44);
        search.getStyleClass().add("admin-search");
        search.textProperty().addListener((observable, oldValue, newValue) -> filterRequests());

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("password-requests-table");
        TableColumn<Request, String> username = column("Username", Request::username, 220);
        username.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        table.getColumns().add(username);
        table.getColumns().add(column("Message", Request::message, 380));
        table.getColumns().add(column("Requested", Request::createdAt, 240));
        TableColumn<Request, String> status = column("Status", Request::status, 150);
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setStyle(empty ? "" : "Pending".equalsIgnoreCase(value)
                        ? "-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 14px;"
                        : "-fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        });
        table.getColumns().add(status);
        table.setPlaceholder(new Label("No lost-password requests."));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("btn-ghost");
        refresh.setOnAction(event -> load());
        Button resolve = new Button("Mark resolved");
        resolve.getStyleClass().add("btn-primary");
        resolve.setOnAction(event -> resolveSelected());
        HBox actions = new HBox(10, refresh, resolve);

        HBox heading = new HBox(16, title, new Region(), search);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().addAll(heading, subtitle, actions, table, message);
        load();
    }

    private TableColumn<Request, String> column(String title, java.util.function.Function<Request, String> value,
                                                 double width) {
        TableColumn<Request, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void load() {
        try {
            List<Request> requests = PasswordResetRequests.list();
            allRequests = requests;
            filterRequests();
            message.setText("");
        } catch (Exception e) {
            message.setText("Could not load lost-password requests: " + e.getMessage());
        }
    }

    private void filterRequests() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        FilteredList<Request> filtered = new FilteredList<>(
                FXCollections.observableArrayList(allRequests),
                request -> query.isEmpty()
                        || request.username().toLowerCase(Locale.ROOT).contains(query)
                        || request.message().toLowerCase(Locale.ROOT).contains(query)
                        || request.status().toLowerCase(Locale.ROOT).contains(query));
        table.setItems(filtered);
    }

    private void resolveSelected() {
        Request request = table.getSelectionModel().getSelectedItem();
        if (request == null) {
            message.setText("Select a request first.");
            return;
        }
        try {
            PasswordResetRequests.markResolved(request.id());
            load();
            message.setText("Request marked as resolved.");
        } catch (Exception e) {
            message.setText("Could not update the request: " + e.getMessage());
        }
    }
}
