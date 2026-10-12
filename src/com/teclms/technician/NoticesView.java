package com.teclms.technician;

import com.teclms.technician.DemoData.Notice;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.List;

/** Read-only: technical officers can only see notices. */
public class NoticesView extends VBox {

    private final List<Notice> notices = DemoData.notices();
    private final VBox list = new VBox(14);

    public NoticesView() {
        getStyleClass().add("page");
        setSpacing(20);

        TextField search = new TextField();
        search.setPromptText("Search notices");
        search.setPrefWidth(260);
        search.textProperty().addListener((o, a, text) -> render(text));

        getChildren().addAll(
                UiKit.pageHeader("Notices", "Announcements from the faculty and administration", search),
                list);
        render("");
    }

    private void render(String query) {
        String k = query.toLowerCase().trim();
        list.getChildren().clear();
        for (Notice n : notices) {
            if (k.isEmpty() || n.title().toLowerCase().contains(k) || n.body().toLowerCase().contains(k)) {
                list.getChildren().add(card(n));
            }
        }
        if (list.getChildren().isEmpty()) {
            Label none = new Label("No notices match your search.");
            none.getStyleClass().add("hint");
            list.getChildren().add(none);
        }
    }

    private VBox card(Notice n) {
        Label title = new Label(n.title());
        title.getStyleClass().add("notice-title");
        Label category = UiKit.pill(n.category(), n.category().equals("Exam") ? "bad"
                : n.category().equals("Event") ? "purple" : "info");
        Label date = new Label(n.date());
        date.getStyleClass().add("hint");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, title, category, spacer, date);
        top.setAlignment(Pos.CENTER_LEFT);
        if (n.isNew()) top.getChildren().add(2, UiKit.pill("NEW", "ok"));

        Label body = new Label(n.body());
        body.setWrapText(true);
        body.getStyleClass().add("notice-body");
        return UiKit.card(top, body);
    }
}
