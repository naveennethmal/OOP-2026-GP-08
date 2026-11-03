package Technical_Officer;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static Technical_Officer.TechnicianDAO.text;
import static Technical_Officer.TechnicianDAO.toInt;


public class NoticesView extends VBox {

    private final List<Map<String, Object>> notices = new ArrayList<>();
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

        try {
            notices.addAll(TechnicianDAO.notices());
        } catch (Exception e) {
            UiKit.showLoadError(this, e);
        }
        render("");
    }

    private void render(String query) {
        String k = query.toLowerCase().trim();
        list.getChildren().clear();
        for (Map<String, Object> n : notices) {
            if (k.isEmpty() || text(n.get("title")).toLowerCase().contains(k)
                    || text(n.get("description")).toLowerCase().contains(k)) {
                list.getChildren().add(card(n));
            }
        }
        if (list.getChildren().isEmpty()) {
            Label none = new Label("No notices match your search.");
            none.getStyleClass().add("hint");
            list.getChildren().add(none);
        }
    }
    private static String categoryOf(String title) {
        String t = title.toLowerCase();
        if (t.contains("exam")) return "Exam";
        if (t.contains("meeting") || t.contains("event")) return "Event";
        return "General";
    }

    private VBox card(Map<String, Object> n) {
        String title = text(n.get("title"));
        String category = categoryOf(title);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notice-title");
        Label categoryPill = UiKit.pill(category, category.equals("Exam") ? "bad"
                : category.equals("Event") ? "purple" : "info");
        Label date = new Label(text(n.get("published")));
        date.getStyleClass().add("hint");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, titleLabel, categoryPill, spacer, date);
        top.setAlignment(Pos.CENTER_LEFT);
        if (toInt(n.get("is_new")) == 1) top.getChildren().add(2, UiKit.pill("NEW", "ok"));

        Label body = new Label(text(n.get("description")));
        body.setWrapText(true);
        body.getStyleClass().add("notice-body");
        return UiKit.card(top, body);
    }
}
