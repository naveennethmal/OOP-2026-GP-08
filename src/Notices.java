import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class Notices extends VBox {
    public Notices() {
        super(18);
        getStyleClass().add("content");
        getChildren().add(Ui.header("Notices", "Latest announcements from the university"));

        // title, date, tag, color, description
        String[][] n = {
                {"Mid-semester examination schedule released", "2026-09-25", "New", "blue",
                        "The mid-semester examination timetable is now available. Please check your exam dates and venues."},
                {"Lab maintenance on Friday", "2026-09-24", "Info", "yellow",
                        "Computer Labs 1 to 3 will be closed on Friday from 8.00 AM to 12.00 PM for maintenance."},
                {"Medical submission deadline", "2026-09-20", "Urgent", "red",
                        "All medical certificates for the previous month must be submitted before the deadline."},
                {"Library hours extended", "2026-09-15", "Info", "green",
                        "The library will stay open until 9.00 PM during the examination weeks."}};

        for (String[] x : n) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox top = new HBox(10, Ui.text(x[0], "notice-title"), sp, Ui.tag(x[2], x[3]));
            Label desc = Ui.text(x[4], "notice-desc");
            desc.setWrapText(true);
            VBox card = new VBox(8, top, Ui.text(x[1], "muted-small"), desc);
            card.getStyleClass().add("card");
            getChildren().add(card);
        }
    }
}