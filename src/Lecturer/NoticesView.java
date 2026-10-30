package Lecturer;

import Lecturer.LecturerDAO.Notice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import static Lecturer.LecturerUi.*;

/** Lecturers can only read notices (the admin writes them). */
public class NoticesView extends VBox {

    public NoticesView() {
        getStyleClass().add("page");
        setSpacing(18);

        TextField search = searchField("Search notices...");
        ListView<Notice> list = new ListView<>();
        list.setPrefWidth(380);
        list.setMinWidth(380);
        list.setPrefHeight(460);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Notice n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : n.title() + "\n" + n.date());
            }
        });

        Label title = new Label("Select a notice to read it");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label date = hint("");
        Label body = new Label();
        body.setWrapText(true);
        VBox detail = card(title, date, body);
        HBox.setHgrow(detail, Priority.ALWAYS);

        list.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n == null) return;
            title.setText(n.title());
            date.setText("Published: " + n.date());
            body.setText(n.body());
        });

        getChildren().add(header("Notices", "Announcements from the administration"));
        try {
            ObservableList<Notice> all = FXCollections.observableArrayList(LecturerDAO.notices());
            list.setItems(searchable(all, search, n -> n.title() + " " + n.body()));
            getChildren().addAll(search, new HBox(18, list, detail));
            if (!all.isEmpty()) list.getSelectionModel().selectFirst();
        } catch (Exception e) {
            getChildren().add(errorLabel("Could not load notices.", e));
        }
    }
}
