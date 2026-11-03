package Admin;

import Common.PasswordResetRequests;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PasswordRequestsView extends BorderPane {

    private final TableView<PasswordResetRequests.Request> table = new TableView<>();
    private final Label message = new Label();
    private final Label pendingCount = new Label();

    public PasswordRequestsView() {
        setStyle("-fx-background-color: #F3F6FC;");
        setPadding(new javafx.geometry.Insets(22));

        Label title = new Label("Password Reset Requests");
        title.setStyle("-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: #172554;");

        Label subtitle = new Label("Review password help requests submitted from the login page.");
        subtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> reload());
        refresh.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 9px 14px; -fx-background-radius: 7px;");

        HBox heading = new HBox(12, new VBox(5, title, subtitle), pendingCount, refresh);
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(heading.getChildren().get(0), Priority.ALWAYS);

        configureTable();
        message.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");

        VBox content = new VBox(14, heading, message, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(content);
        reload();
    }

    private void configureTable() {
        table.getStyleClass().add("password-requests-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No password reset requests."));

        addColumn("Username", PasswordResetRequests.Request::username, 130);
        addColumn("Message", PasswordResetRequests.Request::message, 280);
        addColumn("Submitted", PasswordResetRequests.Request::createdAt, 180);
        addColumn("Status", PasswordResetRequests.Request::status, 100);

        TableColumn<PasswordResetRequests.Request, Void> action = new TableColumn<>("Action");
        action.setMinWidth(130);
        action.setMaxWidth(150);
        action.setSortable(false);
        action.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final Button resolve = new Button("Mark resolved");

            {
                resolve.setOnAction(event -> {
                    PasswordResetRequests.Request request = getTableRow().getItem();
                    if (request != null) {
                        resolve(request);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                PasswordResetRequests.Request request = empty ? null : getTableRow().getItem();
                setGraphic(request != null && "Pending".equalsIgnoreCase(request.status())
                        ? resolve : null);
            }
        });
        table.getColumns().add(action);
    }

    private void addColumn(String title, java.util.function.Function<PasswordResetRequests.Request, String> value,
                           double width) {
        TableColumn<PasswordResetRequests.Request, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private void reload() {
        try {
            var requests = PasswordResetRequests.list();
            table.setItems(FXCollections.observableArrayList(requests));
            long pending = requests.stream()
                    .filter(request -> "Pending".equalsIgnoreCase(request.status()))
                    .count();
            pendingCount.setText(pending + " pending");
            pendingCount.setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
            message.setText("");
        } catch (Exception ex) {
            message.setText("Could not load password reset requests: " + ex.getMessage());
        }
    }

    private void resolve(PasswordResetRequests.Request request) {
        try {
            PasswordResetRequests.markResolved(request.id());
            reload();
        } catch (Exception ex) {
            message.setText("Could not update the request: " + ex.getMessage());
        }
    }

}
