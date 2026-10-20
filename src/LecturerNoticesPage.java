import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LecturerNoticesPage extends VBox {

    public LecturerNoticesPage() {
        setSpacing(15);

        Label title = new Label("Notices");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField search = new TextField();
        search.setPromptText("Search notices...");
        search.setMaxWidth(350);

        Label status = new Label();

        ListView<Notice> list = new ListView<>();
        list.setPrefWidth(420);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Notice n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : n.getTitle() + "\n" + n.getPublishedDate());
            }
        });

        // Detail panel on the right
        Label dTitle = new Label("Select a notice to read it");
        dTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        dTitle.setWrapText(true);
        Label dDate = new Label();
        dDate.setStyle("-fx-text-fill: #666;");
        Label dBody = new Label();
        dBody.setWrapText(true);
        VBox detail = new VBox(10, dTitle, dDate, dBody);
        detail.setPadding(new Insets(20));
        detail.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        HBox.setHgrow(detail, Priority.ALWAYS);

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> {
            if (n != null) {
                dTitle.setText(n.getTitle());
                dDate.setText("Published: " + n.getPublishedDate());
                dBody.setText(n.getDescription());
            }
        });

        try {
            ObservableList<Notice> all =
                    FXCollections.observableArrayList(new NoticeDAO().getAllNotices());
            FilteredList<Notice> filtered = new FilteredList<>(all, n -> true);
            list.setItems(filtered);

            search.textProperty().addListener((obs, old, text) -> {
                String t = text.toLowerCase().trim();
                filtered.setPredicate(n -> t.isEmpty()
                        || n.getTitle().toLowerCase().contains(t)
                        || n.getDescription().toLowerCase().contains(t));
            });

            status.setText(all.size() + " notices");
            if (!all.isEmpty()) list.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            status.setStyle("-fx-text-fill: red;");
            status.setText("Could not load notices: " + ex.getMessage());
        }

        HBox body = new HBox(20, list, detail);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(title, search, status, body);
    }
}