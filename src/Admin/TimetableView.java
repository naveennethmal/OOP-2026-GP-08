package Admin;

import Database.Db;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.sql.Time;
import java.util.*;

/**
 * Timetable with clash prevention:
 *  - a hall is offered only if no other lecture overlaps the chosen slot (it frees up when that lecture ends)
 *  - a lecturer cannot be in two lectures at the same time
 * Overlap rule: existing.start < new.end AND existing.end > new.start (back-to-back slots are allowed).
 */
public class TimetableView extends CrudView {
    private static boolean timetableSchemaPrepared;
    private final ComboBox<Item> course = new ComboBox<>(items(
            "SELECT course_code k, CONCAT(course_code,' - ',course_name) l "
                    + "FROM courses ORDER BY course_code"));
    private final ComboBox<Item> lecturer = new ComboBox<>(items(
            "SELECT l.lecturer_id k, u.full_name l FROM lecturers l "
                    + "JOIN users u ON u.user_id=l.user_id ORDER BY u.full_name"));
    private final ComboBox<String> day = new ComboBox<>(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
    private final ComboBox<String> start = new ComboBox<>(times()), end = new ComboBox<>(times());
    private final ComboBox<String> level = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4"));
    private final ComboBox<String> semester = new ComboBox<>(
            FXCollections.observableArrayList("1", "2"));
    private final ComboBox<String> type = new ComboBox<>(
            FXCollections.observableArrayList("THEORY", "PRACTICAL", "THEORY/PRACTICAL", "TUTORIAL"));
    private final TextField location = new TextField();

    public TimetableView() {
        ensureTimetableSchema();
        type.setDisable(true);
        level.setDisable(true);
        semester.setDisable(true);
        lecturer.setDisable(true);
        course.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                type.setValue(null);
                level.setValue(null);
                semester.setValue(null);
            } else {
                loadCourseDetails(newValue.key());
            }
        });
        build();
    }

    private void loadCourseDetails(String courseCode) {
        try {
            var rows = Db.query("SELECT theory_practical, level, semester, lecturer_id FROM courses "
                    + "WHERE course_code=?", courseCode);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Selected course was not found.");
            }
            Map<String, Object> row = rows.get(0);
            type.setValue(row.get("theory_practical") == null
                    ? null : row.get("theory_practical").toString());
            level.setValue(row.get("level") == null ? null : row.get("level").toString());
            semester.setValue(row.get("semester") == null ? null : row.get("semester").toString());
            pick(lecturer, row.get("lecturer_id"));
        } catch (Exception ex) {
            type.setValue(null);
            level.setValue(null);
            semester.setValue(null);
            lecturer.setValue(null);
            fail("Cannot load course details: " + ex.getMessage());
        }
    }

    private static void ensureTimetableSchema() {
        if (timetableSchemaPrepared) {
            return;
        }
        addColumn("level");
        addColumn("semester");
        try {
            Db.update("ALTER TABLE time_table MODIFY COLUMN session_type VARCHAR(30) NOT NULL");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare the timetable session type column.", ex);
        }
        timetableSchemaPrepared = true;
    }

    private static void addColumn(String column) {
        try {
            Db.update("ALTER TABLE time_table ADD COLUMN " + column + " VARCHAR(30) NULL");
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException("Cannot prepare the timetable table.", ex);
            }
        }
    }

    private static boolean isDuplicateColumn(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sqlException
                    && sqlException.getErrorCode() == 1060) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static javafx.collections.ObservableList<String> times() {
        List<String> l = new ArrayList<>();
        for (int h = 8; h <= 18; h++) { l.add(String.format("%02d:00", h)); if (h < 18) l.add(String.format("%02d:30", h)); }
        return FXCollections.observableArrayList(l);
    }

    private boolean slotValid() {
        return day.getValue() != null && start.getValue() != null && end.getValue() != null && start.getValue().compareTo(end.getValue()) < 0;
    }
    private int selfId() { return selected == null ? 0 : Integer.parseInt(selected.get("timetable_id").toString()); }
    private static Time t(String hhmm) { return Time.valueOf(hhmm + ":00"); }

    @Override protected String title() { return "Timetables"; }
    @Override protected String searchColumn() { return "course_code"; }
    @Override protected String searchPrompt() { return "Search timetable"; }
    @Override protected String[][] columns() {
        return new String[][]{{"Day", "day"}, {"From", "start_time"}, {"To", "end_time"}, {"Course", "course_code"},
                {"Type", "session_type"}, {"Level", "level"}, {"Semester", "semester"},
                {"Lecturer", "lecturer"}, {"Location", "location"}};
    }
    @Override protected String selectSql() {
        return "SELECT t.timetable_id,t.course_code,t.lecturer_id,u.full_name lecturer,t.location,t.day,"
                + "t.level,t.semester,"
                + "TIME_FORMAT(t.start_time,'%H:%i') start_time,TIME_FORMAT(t.end_time,'%H:%i') end_time,t.session_type "
                + "FROM time_table t JOIN lecturers l ON l.lecturer_id=t.lecturer_id "
                + "JOIN users u ON u.user_id=l.user_id "
                + "ORDER BY FIELD(t.day,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'),t.start_time";
    }
    @Override protected Node form() {
        return new VBox(10, field("Course", course), field("Lecturer", lecturer), field("Session type", type), field("Day", day),
                field("Level", level), field("Semester", semester), field("Start time", start),
                field("End time", end), field("Location", location));
    }
    @Override protected void fill(Map<String, Object> r) {
        pick(course, r.get("course_code")); loadCourseDetails(r.get("course_code").toString());
        pick(lecturer, r.get("lecturer_id"));
        day.setValue(r.get("day").toString()); start.setValue(r.get("start_time").toString()); end.setValue(r.get("end_time").toString());
        location.setText(r.get("location") == null ? "" : r.get("location").toString());
    }
    @Override protected void reset() {
        course.setValue(null); lecturer.setValue(null); type.setValue(null); day.setValue(null);
        level.setValue(null); semester.setValue(null); start.setValue(null); end.setValue(null); location.clear();
    }

    @Override protected void save() throws Exception {
        if (course.getValue() == null || lecturer.getValue() == null || type.getValue() == null
                || level.getValue() == null || semester.getValue() == null
                || location.getText().isBlank())
            throw new IllegalArgumentException("Select course, lecturer, session type, level, semester and location.");
        if (!slotValid()) throw new IllegalArgumentException("End time must be after start time.");
        Object[] slot = {day.getValue(), t(end.getValue()), t(start.getValue()), selfId()};

        var lecClash = Db.query("SELECT CONCAT(course_code,' ',TIME_FORMAT(start_time,'%H:%i'),'-',TIME_FORMAT(end_time,'%H:%i')) info "
                        + "FROM time_table WHERE lecturer_id=? AND day=? AND start_time<? AND end_time>? AND timetable_id<>?",
                lecturer.getValue().key(), slot[0], slot[1], slot[2], slot[3]);
        if (!lecClash.isEmpty())
            throw new IllegalArgumentException("Lecturer already teaches " + lecClash.get(0).get("info") + " at this time.");

        if (selected == null)
            Db.update("INSERT INTO time_table(course_code,lecturer_id,day,start_time,end_time,location,session_type,level,semester) VALUES(?,?,?,?,?,?,?,?,?)",
                    course.getValue().key(), lecturer.getValue().key(), day.getValue(), t(start.getValue()), t(end.getValue()),
                    location.getText().trim(), type.getValue(), level.getValue(), semester.getValue());
        else
            Db.update("UPDATE time_table SET course_code=?,lecturer_id=?,day=?,start_time=?,end_time=?,location=?,session_type=?,level=?,semester=? WHERE timetable_id=?",
                    course.getValue().key(), lecturer.getValue().key(), day.getValue(), t(start.getValue()), t(end.getValue()),
                    location.getText().trim(), type.getValue(), level.getValue(), semester.getValue(), selfId());
    }
    @Override protected void delete(Map<String, Object> r) throws Exception {
        Db.update("DELETE FROM time_table WHERE timetable_id=?", r.get("timetable_id"));
    }
}
