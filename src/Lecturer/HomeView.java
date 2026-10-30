package Lecturer;

import Lecturer.LecturerDAO.Course;
import Lecturer.LecturerDAO.Notice;
import Lecturer.LecturerDAO.Profile;
import Lecturer.LecturerDAO.Stats;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static Lecturer.LecturerUi.*;

/** Dashboard: totals, my courses and the latest notices. */
public class HomeView extends VBox {

    public HomeView(Consumer<String> go) {
        getStyleClass().add("page");
        setSpacing(18);

        try {
            Profile me = LecturerDAO.profile();
            Stats st = LecturerDAO.stats();
            String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            getChildren().add(header("Welcome back, " + me.fullName(),
                    today + ", " + LocalDate.now() + "   |   Employee No: " + me.employeeNo()));

            getChildren().add(statRow(
                    link(stat("My courses", String.valueOf(st.courses()), "📚", "blue"), "Course materials", go),
                    link(stat("Students", String.valueOf(st.students()), "👥", "green"), "Student details", go),
                    link(stat("Marks to upload", String.valueOf(st.pendingMarks()), "📝", "amber"), "Upload marks", go),
                    link(stat("Notices", String.valueOf(st.notices()), "🔔", "red"), "Notices", go)));

            VBox courses = coursesCard(LecturerDAO.courses());
            VBox notices = noticesCard(LecturerDAO.notices(), go);
            HBox.setHgrow(courses, Priority.ALWAYS);
            HBox.setHgrow(notices, Priority.ALWAYS);
            courses.setPrefWidth(10);
            notices.setPrefWidth(10);
            getChildren().add(new HBox(18, courses, notices));
        } catch (Exception e) {
            getChildren().add(errorLabel("Could not load the dashboard.", e));
        }
    }

    /** Makes a stat card clickable. */
    private VBox link(VBox card, String page, Consumer<String> go) {
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> go.accept(page));
        return card;
    }

    private VBox coursesCard(List<Course> courses) {
        VBox box = card(cardTitle("My courses"));
        if (courses.isEmpty()) box.getChildren().add(hint("No courses are assigned to you yet."));
        for (Course c : courses) {
            Label name = new Label(c.toString());
            name.getStyleClass().add("row-strong");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label type = new Label(c.type());
            type.getStyleClass().addAll("pill", "pill-info");
            HBox row = new HBox(10, name, spacer, type);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox noticesCard(List<Notice> notices, Consumer<String> go) {
        VBox box = card(cardTitle("Recent notices"));
        if (notices.isEmpty()) box.getChildren().add(hint("No notices yet."));
        for (int i = 0; i < Math.min(3, notices.size()); i++) {
            Label title = new Label(notices.get(i).title());
            title.getStyleClass().add("row-strong");
            title.setWrapText(true);
            box.getChildren().add(new VBox(2, title, hint(notices.get(i).date())));
        }
        Hyperlink all = new Hyperlink("View all notices →");
        all.setOnAction(e -> go.accept("Notices"));
        box.getChildren().add(all);
        return box;
    }
}
