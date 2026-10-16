package ftms;

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
    private final ComboBox<Item> course = new ComboBox<>(items("SELECT course_code k, CONCAT(course_code,' - ',name) l FROM course ORDER BY course_code"));
    private final ComboBox<Item> lecturer = new ComboBox<>(items("SELECT user_id k, full_name l FROM users WHERE role='LECTURER' ORDER BY full_name"));
    private final ComboBox<String> day = new ComboBox<>(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
    private final ComboBox<String> start = new ComboBox<>(times()), end = new ComboBox<>(times());
    private final ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("THEORY", "PRACTICAL"));
    private final ComboBox<Item> hall = new ComboBox<>();
    private final Label hint = new Label("Pick day, start and end time to see free halls.");

    public TimetableView() {
        hint.getStyleClass().add("hint"); hint.setWrapText(true);
        day.valueProperty().addListener((o, a, b) -> refreshHalls());
        start.valueProperty().addListener((o, a, b) -> refreshHalls());
        end.valueProperty().addListener((o, a, b) -> refreshHalls());
        build();
    }

    private static javafx.collections.ObservableList<String> times() {
        List<String> l = new ArrayList<>();
        for (int h = 8; h <= 18; h++) { l.add(String.format("%02d:00", h)); if (h < 18) l.add(String.format("%02d:30", h)); }
        return FXCollections.observableArrayList(l);
    }

    private boolean slotValid() {
        return day.getValue() != null && start.getValue() != null && end.getValue() != null && start.getValue().compareTo(end.getValue()) < 0;
    }
    private int selfId() { return selected == null ? 0 : Integer.parseInt(selected.get("id").toString()); }
    private static Time t(String hhmm) { return Time.valueOf(hhmm + ":00"); }

    private void refreshHalls() {
        Item keep = hall.getValue();
        hall.getItems().clear();
        if (!slotValid()) { hint.setText("Pick day, start and end time (end must be after start) to see free halls."); return; }
        hall.setItems(items("SELECT hall_id k, CONCAT(name,' (',capacity,' seats)') l FROM lecture_hall WHERE hall_id NOT IN "
                + "(SELECT hall_id FROM timetable WHERE day=? AND start_time<? AND end_time>? AND id<>?) ORDER BY name",
                day.getValue(), t(end.getValue()), t(start.getValue()), selfId()));
        hint.setText(hall.getItems().size() + " hall(s) free for this slot.");
        if (keep != null) pick(hall, keep.key());
    }

    @Override protected String title() { return "Timetables"; }
    @Override protected String[][] columns() {
        return new String[][]{{"Day", "day"}, {"From", "start_time"}, {"To", "end_time"}, {"Course", "course_code"},
                {"Type", "session_type"}, {"Lecturer", "lecturer"}, {"Hall", "hall"}};
    }
    @Override protected String selectSql() {
        return "SELECT t.id,t.course_code,t.lecturer_id,u.full_name lecturer,t.hall_id,h.name hall,t.day,"
             + "TIME_FORMAT(t.start_time,'%H:%i') start_time,TIME_FORMAT(t.end_time,'%H:%i') end_time,t.session_type "
             + "FROM timetable t JOIN users u ON u.user_id=t.lecturer_id JOIN lecture_hall h ON h.hall_id=t.hall_id "
             + "ORDER BY FIELD(t.day,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'),t.start_time";
    }
    @Override protected Node form() {
        return new VBox(10, field("Course", course), field("Lecturer", lecturer), field("Session type", type), field("Day", day),
                field("Start time", start), field("End time", end), field("Lecture hall (free only)", hall), hint);
    }
    @Override protected void fill(Map<String, Object> r) {
        pick(course, r.get("course_code")); pick(lecturer, r.get("lecturer_id")); type.setValue(r.get("session_type").toString());
        day.setValue(r.get("day").toString()); start.setValue(r.get("start_time").toString()); end.setValue(r.get("end_time").toString());
        refreshHalls(); pick(hall, r.get("hall_id"));
    }
    @Override protected void reset() {
        course.setValue(null); lecturer.setValue(null); type.setValue(null); day.setValue(null);
        start.setValue(null); end.setValue(null); hall.getItems().clear(); hall.setValue(null);
    }

    @Override protected void save() throws Exception {
        if (course.getValue() == null || lecturer.getValue() == null || type.getValue() == null || hall.getValue() == null)
            throw new IllegalArgumentException("Select course, lecturer, session type and hall.");
        if (!slotValid()) throw new IllegalArgumentException("End time must be after start time.");
        Object[] slot = {day.getValue(), t(end.getValue()), t(start.getValue()), selfId()};

        var hallClash = Db.query("SELECT CONCAT(course_code,' ',TIME_FORMAT(start_time,'%H:%i'),'-',TIME_FORMAT(end_time,'%H:%i')) info "
                + "FROM timetable WHERE hall_id=? AND day=? AND start_time<? AND end_time>? AND id<>?",
                hall.getValue().key(), slot[0], slot[1], slot[2], slot[3]);
        if (!hallClash.isEmpty())
            throw new IllegalArgumentException("Hall is booked by " + hallClash.get(0).get("info") + ". It is free after that lecture ends.");

        var lecClash = Db.query("SELECT CONCAT(course_code,' ',TIME_FORMAT(start_time,'%H:%i'),'-',TIME_FORMAT(end_time,'%H:%i')) info "
                + "FROM timetable WHERE lecturer_id=? AND day=? AND start_time<? AND end_time>? AND id<>?",
                lecturer.getValue().key(), slot[0], slot[1], slot[2], slot[3]);
        if (!lecClash.isEmpty())
            throw new IllegalArgumentException("Lecturer already teaches " + lecClash.get(0).get("info") + " at this time.");

        if (selected == null)
            Db.update("INSERT INTO timetable(course_code,lecturer_id,hall_id,day,start_time,end_time,session_type) VALUES(?,?,?,?,?,?,?)",
                    course.getValue().key(), lecturer.getValue().key(), hall.getValue().key(), day.getValue(), t(start.getValue()), t(end.getValue()), type.getValue());
        else
            Db.update("UPDATE timetable SET course_code=?,lecturer_id=?,hall_id=?,day=?,start_time=?,end_time=?,session_type=? WHERE id=?",
                    course.getValue().key(), lecturer.getValue().key(), hall.getValue().key(), day.getValue(), t(start.getValue()), t(end.getValue()), type.getValue(), selfId());
    }
    @Override protected void delete(Map<String, Object> r) throws Exception { Db.update("DELETE FROM timetable WHERE id=?", r.get("id")); }
}
