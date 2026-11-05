package Student;

import Common.Session;
import Database.Db;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Timetable extends VBox {

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private record ScheduleEntry(int day, String start, String end, String code,
                                 String name, String room, String sessionType) {}

    public Timetable() {
        super(20);
        getStyleClass().add("content");

        String regNo = Session.getRegNo();
        if (regNo == null || regNo.isBlank()) {
            getChildren().add(Ui.header("Timetable", "Please login to view your timetable"));
            return;
        }

        try {
            List<Map<String, Object>> rows = Db.query("""
                    SELECT t.day,
                           TIME_FORMAT(t.start_time, '%H:%i') AS start_time,
                           TIME_FORMAT(t.end_time, '%H:%i') AS end_time,
                           c.course_code, c.course_name, t.location, t.session_type,
                           d.department_name, ug.level, ug.semester
                    FROM undergraduates ug
                    JOIN departments d ON d.department_id = ug.department_id
                    JOIN courses c ON c.department_id = ug.department_id
                                   AND c.level = ug.level AND c.semester = ug.semester
                    JOIN time_table t ON t.course_code = c.course_code
                    WHERE ug.reg_no = ?
                    ORDER BY t.start_time,
                             FIELD(t.day, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday')
                    """, regNo);

            String department = rows.isEmpty() ? "" : value(rows.get(0), "department_name");
            String level = rows.isEmpty() ? "" : value(rows.get(0), "level");
            String semester = rows.isEmpty() ? "" : value(rows.get(0), "semester");
            List<ScheduleEntry> entries = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                int day = List.of(DAYS).indexOf(value(row, "day"));
                if (day < 0) continue;
                String session = value(row, "session_type");
                entries.add(new ScheduleEntry(day, value(row, "start_time"), value(row, "end_time"),
                        value(row, "course_code"), value(row, "course_name"), value(row, "location"), session));
            }

            String subtitle = "Weekly timetable" + (department.isBlank() ? "" : " · " + department)
                    + (level.isBlank() ? "" : " · Level " + level)
                    + (semester.isBlank() ? "" : " · Semester " + semester);
            VBox scheduleCard = Ui.card("Weekly schedule", entries.isEmpty()
                    ? Ui.text("No timetable entries found for your department, level and semester.", "muted-small")
                    : buildGrid(entries));
            getChildren().addAll(Ui.header("Timetable", subtitle), scheduleCard);
        } catch (Exception ex) {
            getChildren().addAll(Ui.header("Timetable", "Your weekly class schedule"),
                    Ui.card("Weekly schedule", Ui.text("Unable to load timetable: " + ex.getMessage(), "err")));
        }
    }

    private static GridPane buildGrid(List<ScheduleEntry> entries) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints timeColumn = new ColumnConstraints(110);
        grid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < DAYS.length; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(100);
            column.setPrefWidth(160);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
            Label day = new Label(DAYS[i]);
            day.getStyleClass().add("tt-day");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setAlignment(Pos.CENTER);
            grid.add(day, i + 1, 0);
        }

        Map<String, List<ScheduleEntry>> byTime = new TreeMap<>();
        for (ScheduleEntry entry : entries) {
            byTime.computeIfAbsent(entry.start() + " - " + entry.end(), ignored -> new ArrayList<>()).add(entry);
        }
        int row = 1;
        for (Map.Entry<String, List<ScheduleEntry>> time : byTime.entrySet()) {
            Label timeLabel = new Label(time.getKey());
            timeLabel.getStyleClass().add("tt-time");
            grid.add(timeLabel, 0, row);
            List<List<ScheduleEntry>> entriesByDay = new ArrayList<>();
            for (int day = 0; day < DAYS.length; day++) entriesByDay.add(new ArrayList<>());
            for (ScheduleEntry entry : time.getValue()) entriesByDay.get(entry.day()).add(entry);
            for (int day = 0; day < DAYS.length; day++) {
                List<ScheduleEntry> dayEntries = entriesByDay.get(day);
                Node cell;
                if (dayEntries.isEmpty()) {
                    VBox empty = new VBox();
                    empty.getStyleClass().add("tt-empty");
                    empty.setMinHeight(88);
                    cell = empty;
                } else {
                    VBox stack = new VBox(6);
                    for (ScheduleEntry entry : dayEntries) {
                        Label code = new Label(entry.code());
                        code.getStyleClass().add("tt-code");
                        Label name = new Label(entry.name());
                        name.setWrapText(true);
                        name.getStyleClass().add("tt-name");
                        Label meta = new Label(entry.room() + "  |  " + sessionLabel(entry.sessionType()));
                        meta.getStyleClass().add("tt-meta");
                        VBox card = new VBox(3, code, name, meta);
                        card.getStyleClass().add(entry.sessionType().toUpperCase().contains("PRACTICAL")
                                ? "tt-practical" : "tt-theory");
                        card.setMinHeight(88);
                        card.setMaxWidth(Double.MAX_VALUE);
                        stack.getChildren().add(card);
                    }
                    cell = stack;
                }
                grid.add(cell, day + 1, row);
            }
            row++;
        }
        return grid;
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "-" : value.toString();
    }

    private static String sessionLabel(String sessionType) {
        String normalized = sessionType == null ? "" : sessionType.toUpperCase();
        boolean hasTheory = normalized.contains("THEORY");
        boolean hasPractical = normalized.contains("PRACTICAL");
        if (hasTheory && hasPractical) return "Theory & Practical";
        if (hasPractical) return "Practical";
        if (hasTheory) return "Theory";
        return sessionType == null || sessionType.isBlank() ? "Session" : sessionType;
    }
}
