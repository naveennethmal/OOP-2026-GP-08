package Technical_Officer;

import Technical_Officer.TimetableDAO.Slot;
import Technical_Officer.TimetableDAO.Department;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Time table*/
public class TimetableView extends VBox {

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final ComboBox<Department> department = new ComboBox<>();
    private final ComboBox<String> level = new ComboBox<>();
    private final ComboBox<String> semester = new ComboBox<>();
    private final VBox scheduleCard = new VBox();
    private final Label scheduleTitle = UiKit.cardTitle("Weekly schedule");

    public TimetableView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox legend = new HBox(8, UiKit.pill("Theory", "info"), UiKit.pill("Practical", "purple"));
        legend.setAlignment(Pos.CENTER_RIGHT);

        String subtitle = "Weekly timetable by department, level and semester";
        try {
            department.getItems().setAll(TimetableDAO.departments());
            level.getItems().setAll(AttendanceDAO.levels());
            semester.getItems().setAll(AttendanceDAO.semesters());
            int ownDepartmentId = TimetableDAO.currentDepartmentId();
            department.getItems().stream().filter(d -> d.id() == ownDepartmentId)
                    .findFirst().ifPresent(department::setValue);
        } catch (SQLException e) {
            getChildren().addAll(UiKit.pageHeader("Timetable", subtitle, legend),
                    UiKit.errorLabel("Could not load timetable filters.", e));
            return;
        }
        department.setPromptText("Select department");
        department.setConverter(new StringConverter<>() {
            @Override public String toString(Department value) {
                return value == null ? "" : value.name();
            }
            @Override public Department fromString(String value) { return null; }
        });
        department.setPrefWidth(190);
        department.setMinWidth(190);
        level.setPromptText("Select level");
        semester.setPromptText("Select semester");
        level.setPrefWidth(115);
        level.setMinWidth(115);
        semester.setPrefWidth(125);
        semester.setMinWidth(125);
        if (!level.getItems().isEmpty()) level.setValue(level.getItems().get(0));
        if (!semester.getItems().isEmpty()) semester.setValue(semester.getItems().get(0));
        level.valueProperty().addListener((o, oldValue, newValue) -> reloadGrid());
        semester.valueProperty().addListener((o, oldValue, newValue) -> reloadGrid());
        department.valueProperty().addListener((o, oldValue, newValue) -> reloadGrid());

        VBox departmentField = UiKit.field("Department", department);
        VBox levelField = UiKit.field("Level", level);
        VBox semesterField = UiKit.field("Semester", semester);
        departmentField.setPrefWidth(190);
        levelField.setPrefWidth(115);
        semesterField.setPrefWidth(125);
        HBox filters = new HBox(20, departmentField, levelField, semesterField);
        filters.setAlignment(Pos.TOP_LEFT);
        filters.getStyleClass().add("filter-bar");
        scheduleCard.getChildren().add(scheduleTitle);
        reloadGrid();

        getChildren().addAll(
                UiKit.pageHeader("Timetable", subtitle, legend),
                UiKit.card(filters),
                UiKit.card(scheduleCard));
    }

    private void reloadGrid() {
        if (department.getValue() == null || level.getValue() == null || semester.getValue() == null) return;
        scheduleTitle.setText("Weekly schedule - " + department.getValue().name()
                + " - Level " + level.getValue()
                + " - Semester " + semester.getValue());
        scheduleCard.getChildren().removeIf(node -> node != scheduleTitle);
        try {
            scheduleCard.getChildren().add(buildGrid(department.getValue().id(),
                    level.getValue(), semester.getValue()));
        } catch (SQLException e) {
            scheduleCard.getChildren().add(UiKit.errorLabel("Could not load the timetable.", e));
        }
    }

    private GridPane buildGrid(int departmentId, String selectedLevel, String selectedSemester) throws SQLException {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints timeCol = new ColumnConstraints(110);
        grid.getColumnConstraints().add(timeCol);
        for (int i = 0; i < DAYS.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(120);
            cc.setPrefWidth(160);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        for (int d = 0; d < DAYS.length; d++) {
            Label day = new Label(DAYS[d]);
            day.getStyleClass().add("tt-day");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setAlignment(Pos.CENTER);
            grid.add(day, d + 1, 0);
        }

        Map<String, Map<Integer, List<Slot>>> schedule = new TreeMap<>();
        for (Slot slot : TimetableDAO.list(departmentId, selectedLevel, selectedSemester)) {
            String time = slot.start() + " - " + slot.end();
            schedule.computeIfAbsent(time, ignored -> new TreeMap<>())
                    .computeIfAbsent(slot.day(), ignored -> new ArrayList<>())
                    .add(slot);
        }

        if (schedule.isEmpty()) {
            Label empty = new Label("No timetable entries for this level and semester.");
            empty.getStyleClass().add("muted-small");
            grid.add(empty, 0, 1, DAYS.length + 1, 1);
            return grid;
        }

        int row = 1;
        for (Map.Entry<String, Map<Integer, List<Slot>>> timeEntry : schedule.entrySet()) {
            Label time = new Label(timeEntry.getKey());
            time.getStyleClass().add("tt-time");
            grid.add(time, 0, row);
            for (int d = 0; d < DAYS.length; d++) {
                List<Slot> daySlots = timeEntry.getValue().get(d);
                Node cell;
                if (daySlots == null || daySlots.isEmpty()) {
                    cell = emptyCell();
                } else {
                    VBox stack = new VBox(6);
                    for (Slot slot : daySlots) stack.getChildren().add(slotCard(slot));
                    cell = stack;
                }
                grid.add(cell, d + 1, row);
            }
            row++;
        }
        return grid;
    }

    private VBox slotCard(Slot s) {
        Label code = new Label(s.code());
        code.getStyleClass().add("tt-code");
        Label name = new Label(s.name());
        name.setWrapText(true);
        name.getStyleClass().add("tt-name");
        Label room = new Label(s.room() + (s.practical() ? "  |  Practical" : "  |  Theory"));
        room.getStyleClass().add("tt-meta");
        VBox box = new VBox(3, code, name, room);
        box.getStyleClass().add(s.practical() ? "tt-practical" : "tt-theory");
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
