package Technical_Officer;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static Technical_Officer.TechnicianDAO.text;
import static Technical_Officer.TechnicianDAO.toInt;

// Weekly timetable by department, level and semester
public class TimetableView extends VBox {

    private static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    private final ComboBox<Map<String, Object>> department = new ComboBox<>();
    private final ComboBox<String> level = new ComboBox<>();
    private final ComboBox<String> semester = new ComboBox<>();
    private final Label scheduleTitle = UiKit.cardTitle("Weekly schedule");
    private final VBox scheduleCard = new VBox(scheduleTitle);

    public TimetableView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox legend = new HBox(8, UiKit.pill("Theory", "info"), UiKit.pill("Practical", "purple"));
        legend.setAlignment(Pos.CENTER_RIGHT);
        HBox header = UiKit.pageHeader("Timetable", "Weekly timetable by department, level and semester", legend);

        try {
            department.getItems().setAll(TechnicianDAO.departments());
            int ownDepartment = TechnicianDAO.deptId();
            department.getItems().stream()
                    .filter(d -> toInt(d.get("department_id")) == ownDepartment)
                    .findFirst().ifPresent(department::setValue);
        } catch (Exception e) {
            getChildren().addAll(header, UiKit.errorLabel("Could not load timetable filters.", e));
            return;
        }
        department.setConverter(new StringConverter<>() {
            @Override public String toString(Map<String, Object> d) {
                return d == null ? "" : text(d.get("department_name"));
            }
            @Override public Map<String, Object> fromString(String value) { return null; }
        });
        department.setPromptText("Select department");
        level.getItems().setAll(TechnicianDAO.LEVELS);
        semester.getItems().setAll(TechnicianDAO.SEMESTERS);
        level.setValue(TechnicianDAO.LEVELS.get(0));
        semester.setValue(TechnicianDAO.SEMESTERS.get(0));

        department.setPrefWidth(190);
        department.setMinWidth(190);
        level.setPrefWidth(115);
        level.setMinWidth(115);
        semester.setPrefWidth(125);
        semester.setMinWidth(125);
        department.valueProperty().addListener((o, a, b) -> reloadGrid());
        level.valueProperty().addListener((o, a, b) -> reloadGrid());
        semester.valueProperty().addListener((o, a, b) -> reloadGrid());

        HBox filters = new HBox(20,
                UiKit.field("Department", department),
                UiKit.field("Level", level),
                UiKit.field("Semester", semester));
        filters.setAlignment(Pos.TOP_LEFT);
        filters.getStyleClass().add("filter-bar");

        reloadGrid();
        getChildren().addAll(header, UiKit.card(filters), UiKit.card(scheduleCard));
    }

    private void reloadGrid() {
        if (department.getValue() == null || level.getValue() == null || semester.getValue() == null) return;
        scheduleTitle.setText("Weekly schedule - " + text(department.getValue().get("department_name"))
                + " - Level " + level.getValue() + " - Semester " + semester.getValue());
        scheduleCard.getChildren().removeIf(node -> node != scheduleTitle);
        try {
            scheduleCard.getChildren().add(buildGrid());
        } catch (Exception e) {
            scheduleCard.getChildren().add(UiKit.errorLabel("Could not load the timetable.", e));
        }
    }

    private GridPane buildGrid() throws Exception {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.getColumnConstraints().add(new ColumnConstraints(110));      // time column
        for (int i = 0; i < DAYS.size(); i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(120);
            cc.setPrefWidth(160);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int d = 0; d < DAYS.size(); d++) {
            Label day = new Label(DAYS.get(d));
            day.getStyleClass().add("tt-day");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setAlignment(Pos.CENTER);
            grid.add(day, d + 1, 0);
        }

        // time ("08:00 - 10:00") -> day number -> sessions at that time
        Map<String, Map<Integer, List<Map<String, Object>>>> schedule = new TreeMap<>();
        for (Map<String, Object> slot : TechnicianDAO.timetable(
                toInt(department.getValue().get("department_id")), level.getValue(), semester.getValue())) {
            int day = DAYS.indexOf(text(slot.get("day")));
            if (day < 0) continue;                                         // outside the Mon-Fri grid
            String time = text(slot.get("start_time")) + " - " + text(slot.get("end_time"));
            schedule.computeIfAbsent(time, t -> new TreeMap<>())
                    .computeIfAbsent(day, d -> new ArrayList<>())
                    .add(slot);
        }

        if (schedule.isEmpty()) {
            Label empty = new Label("No timetable entries for this level and semester.");
            empty.getStyleClass().add("muted-small");
            grid.add(empty, 0, 1, DAYS.size() + 1, 1);
            return grid;
        }

        int row = 1;
        for (Map.Entry<String, Map<Integer, List<Map<String, Object>>>> entry : schedule.entrySet()) {
            Label time = new Label(entry.getKey());
            time.getStyleClass().add("tt-time");
            grid.add(time, 0, row);
            for (int d = 0; d < DAYS.size(); d++) {
                List<Map<String, Object>> slots = entry.getValue().get(d);
                Node cell;
                if (slots == null) {
                    cell = emptyCell();
                } else {
                    VBox stack = new VBox(6);
                    for (Map<String, Object> slot : slots) stack.getChildren().add(slotCard(slot));
                    cell = stack;
                }
                grid.add(cell, d + 1, row);
            }
            row++;
        }
        return grid;
    }

    private VBox slotCard(Map<String, Object> slot) {
        boolean practical = text(slot.get("session_type")).toUpperCase().contains("PRACTICAL");
        String room = text(slot.get("location")).isBlank() ? "-" : text(slot.get("location"));

        Label code = new Label(text(slot.get("course_code")));
        code.getStyleClass().add("tt-code");
        Label name = new Label(text(slot.get("course_name")));
        name.setWrapText(true);
        name.getStyleClass().add("tt-name");
        Label meta = new Label(room + (practical ? "  |  Practical" : "  |  Theory"));
        meta.getStyleClass().add("tt-meta");

        VBox box = new VBox(3, code, name, meta);
        box.getStyleClass().add(practical ? "tt-practical" : "tt-theory");
        box.setMinHeight(88);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox emptyCell() {
        VBox box = new VBox();
        box.getStyleClass().add("tt-empty");
        box.setMinHeight(88);
        return box;
    }
}
