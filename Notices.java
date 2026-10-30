package Student;

import Database.Db;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class Notices extends VBox {

    public Notices() {
        super(18);
        getStyleClass().add("content");
        getChildren().add(Ui.header(
                "Notices",
                "Latest announcements from the university"));

        try {
            List<Map<String, Object>> notices = Db.query(
                    "SELECT title, published_date, description "
                            + "FROM notices "
                            + "ORDER BY published_date DESC, notice_id DESC");

            if (notices.isEmpty()) {
                getChildren().add(Ui.text(
                        "No notices are available at the moment.",
                        "muted-small"));
            } else {
                for (Map<String, Object> notice : notices) {
                    addNotice(notice);
                }
            }
        } catch (Exception ex) {
            getChildren().add(Ui.text(
                    "Unable to load notices: " + ex.getMessage(),
                    "err"));
        }
    }

    private void addNotice(Map<String, Object> notice) {
        String title = value(notice, "title");
        String date = value(notice, "published_date");
        String description = value(notice, "description");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(
                10,
                Ui.text(title, "notice-title"),
                spacer);

        Label desc = Ui.text(description, "notice-desc");
        desc.setWrapText(true);

        VBox card = new VBox(
                8,
                top,
                Ui.text(date, "muted-small"),
                desc);
        card.getStyleClass().add("card");
        getChildren().add(card);
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "-" : value.toString();
    }
}
