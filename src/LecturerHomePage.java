import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class LecturerHomePage extends VBox {

    public LecturerHomePage(Lecturer lecturer, Consumer<String> navigate) {
        setSpacing(18);
        setMaxWidth(Double.MAX_VALUE);

        String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        Label welcome = new Label("Welcome back, " + lecturer.getFullName() + "!");
        welcome.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label sub = new Label(today + ", " + LocalDate.now()
                + "   |   Employee No: " + lecturer.getEmployeeNo());
        sub.setStyle("-fx-text-fill: #666;");
        getChildren().addAll(welcome, sub);

        HomeDAO dao = new HomeDAO();
        try {
            int id = lecturer.getLecturerId();

            // Summary cards
            HBox cards = new HBox(15,
                    card("My Courses", String.valueOf(dao.countCourses(id)),
                            "#eef3ff", "#2f6bff", "Course Materials", navigate),
                    card("Students", String.valueOf(dao.countStudents(id)),
                            "#e9f9f0", "#1a9c5b", "Student Details", navigate),
                    card("Marks to Upload", String.valueOf(dao.countPendingMarks(id)),
                            "#fff2e0", "#e08a00", "Upload Marks", navigate),
                    card("Notices", String.valueOf(dao.countNotices()),
                            "#f3ecff", "#7c5cff", "Notices", navigate));
            getChildren().add(cards);

            // Timetable + notices side by side
            VBox timetablePanel = buildTimetable(dao.getTimetable(id), today);
            VBox noticesPanel = buildNotices(navigate);
            HBox.setHgrow(timetablePanel, Priority.ALWAYS);
            getChildren().add(new HBox(18, timetablePanel, noticesPanel));

        } catch (Exception ex) {
            Label err = new Label("Could not load dashboard data: " + ex.getMessage());
            err.setStyle("-fx-text-fill: red;");
            getChildren().add(err);
        }
    }

    private VBox card(String label, String value, String bg, String fg,
                      String target, Consumer<String> navigate) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 14px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 34px; -fx-font-weight: bold;");
        Hyperlink view = new Hyperlink("View \u2192");
        view.setStyle("-fx-text-fill: " + fg + ";");
        view.setOnAction(e -> navigate.accept(target));

        VBox box = new VBox(4, l, v, view);
        box.setPadding(new Insets(16));
        box.setPrefWidth(200);
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14;");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox buildTimetable(List<TimetableEntry> entries, String today) {
        Label title = new Label("My Timetable");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<TimetableEntry> table = new TableView<>();
        table.getColumns().add(col("Day", "day", 95));
        table.getColumns().add(col("Time", "time", 110));
        table.getColumns().add(col("Course", "course", 260));
        table.getColumns().add(col("Location", "location", 130));
        table.getColumns().add(col("Type", "type", 90));
        table.setItems(FXCollections.observableArrayList(entries));
        table.setPlaceholder(new Label("No sessions in your timetable"));
        table.setPrefHeight(230);

        // Highlight today's sessions
        table.setRowFactory(tv -> new TableRow<TimetableEntry>() {
            @Override
            protected void updateItem(TimetableEntry t, boolean empty) {
                super.updateItem(t, empty);
                setStyle(!empty && t != null && t.getDay().equals(today)
                        ? "-fx-background-color: #fff7d6;" : "");
            }
        });

        Label hint = new Label("Today's sessions are highlighted in yellow.");
        hint.setStyle("-fx-text-fill: #666;");

        VBox panel = new VBox(10, title, table, hint);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-background-radius: 8;");
        return panel;
    }

    private VBox buildNotices(Consumer<String> navigate) {
        Label title = new Label("Recent Notices");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox panel = new VBox(12, title);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(340);
        panel.setMinWidth(340);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-background-radius: 8;");

        try {
            List<Notice> notices = new NoticeDAO().getAllNotices();
            if (notices.isEmpty()) panel.getChildren().add(new Label("No notices yet"));
            for (int i = 0; i < Math.min(3, notices.size()); i++) {
                Notice n = notices.get(i);
                Label t = new Label(n.getTitle());
                t.setWrapText(true);
                t.setStyle("-fx-font-weight: bold;");
                Label d = new Label(n.getPublishedDate());
                d.setStyle("-fx-text-fill: #666;");
                panel.getChildren().add(new VBox(2, t, d));
            }
        } catch (Exception ex) {
            Label err = new Label("Could not load notices");
            err.setStyle("-fx-text-fill: red;");
            panel.getChildren().add(err);
        }

        Hyperlink all = new Hyperlink("View all \u2192");
        all.setOnAction(e -> navigate.accept("Notices"));
        panel.getChildren().add(all);
        return panel;
    }

    private TableColumn<TimetableEntry, String> col(String header, String property, double width) {
        TableColumn<TimetableEntry, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}