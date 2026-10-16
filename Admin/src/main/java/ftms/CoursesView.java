package ftms;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Map;

public class CoursesView extends CrudView {
    private final TextField code = new TextField(), name = new TextField(), credits = new TextField();
    private final ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("THEORY", "PRACTICAL", "BOTH"));
    private final ComboBox<String> semester = new ComboBox<>(FXCollections.observableArrayList("1", "2"));
    private final ComboBox<Item> dept = new ComboBox<>(items("SELECT department_id k, name l FROM department ORDER BY name"));

    public CoursesView() { build(); }

    @Override protected String title() { return "Courses"; }
    @Override protected String[][] columns() {
        return new String[][]{{"Code", "course_code"}, {"Name", "name"}, {"Credits", "credits"}, {"Type", "type"},
                {"Sem", "semester"}, {"Department", "department"}};
    }
    @Override protected String selectSql() {
        return "SELECT c.course_code,c.name,c.credits,c.type,c.semester,c.department_id,d.name department FROM course c JOIN department d USING(department_id) ORDER BY c.course_code";
    }
    @Override protected Node form() {
        return new VBox(10, field("Course code", code), field("Course name", name), field("Credits", credits),
                field("Type", type), field("Semester", semester), field("Department", dept));
    }
    @Override protected void fill(Map<String, Object> r) {
        code.setText(r.get("course_code").toString()); code.setDisable(true); name.setText(r.get("name").toString());
        credits.setText(r.get("credits").toString()); type.setValue(r.get("type").toString());
        semester.setValue(r.get("semester").toString()); pick(dept, r.get("department_id"));
    }
    @Override protected void reset() {
        code.clear(); code.setDisable(false); name.clear(); credits.clear(); type.setValue(null); semester.setValue(null); dept.setValue(null);
    }
    @Override protected void save() throws Exception {
        String c = need(code.getText(), "Course code").toUpperCase(), n = need(name.getText(), "Course name");
        int cr;
        try { cr = Integer.parseInt(credits.getText().trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Credits must be a number."); }
        if (type.getValue() == null || semester.getValue() == null || dept.getValue() == null)
            throw new IllegalArgumentException("Select type, semester and department.");
        if (selected == null)
            Db.update("INSERT INTO course VALUES(?,?,?,?,?,?)", c, n, cr, type.getValue(), semester.getValue(), dept.getValue().key());
        else
            Db.update("UPDATE course SET name=?,credits=?,type=?,semester=?,department_id=? WHERE course_code=?",
                    n, cr, type.getValue(), semester.getValue(), dept.getValue().key(), c);
    }
    @Override protected void delete(Map<String, Object> r) throws Exception {
        Db.update("DELETE FROM course WHERE course_code=?", r.get("course_code"));
    }
}
