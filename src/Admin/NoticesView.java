package Admin;

import Database.Db;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public class NoticesView extends CrudView {
    private static final int ADMIN_ID = 1; // replace with the logged-in admin's id once you add login
    private static final String[] TARGET_ROLES = {
            "All", "Admin", "Lecturer", "Technical Officer", "Undergraduate"
    };
    private final TextField titleField = new TextField();
    private final TextArea description = new TextArea();
    private final ComboBox<String> targetRole = new ComboBox<>();
    private final DatePicker startDate = new DatePicker();
    private final DatePicker endDate = new DatePicker();

    public NoticesView() {
        try {
            Db.ensureNoticeSchema();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare notice targeting and date fields.", ex);
        }
        targetRole.getItems().addAll(TARGET_ROLES);
        targetRole.setValue("All");
        targetRole.setMaxWidth(Double.MAX_VALUE);
        startDate.setMaxWidth(Double.MAX_VALUE);
        endDate.setMaxWidth(Double.MAX_VALUE);
        description.setPrefRowCount(6);
        description.setWrapText(true);
        build();
    }

    @Override protected String title() { return "Notices"; }
    @Override protected String searchColumn() { return "title"; }
    @Override protected String searchPrompt() { return "Search notices"; }
    @Override protected String[][] columns() {
        return new String[][]{
                {"ID", "notice_id"}, {"Title", "title"}, {"Target Role", "target_role"},
                {"Start Date", "start_date"}, {"End Date", "end_date"},
                {"Published", "published_date"}, {"Description", "description"}
        };
    }
    @Override protected String selectSql() {
        return "SELECT notice_id,title,target_role,start_date,end_date,published_date,description "
                + "FROM notices ORDER BY published_date DESC, notice_id DESC";
    }
    @Override protected Node form() {
        return new VBox(10,
                field("Title", titleField),
                field("Send to role", targetRole),
                field("Start date", startDate),
                field("End date", endDate),
                field("Description", description));
    }
    @Override protected void fill(Map<String, Object> row) {
        titleField.setText(row.get("title").toString());
        description.setText(row.get("description").toString());
        targetRole.setValue(row.get("target_role") == null ? "All" : row.get("target_role").toString());
        startDate.setValue(asLocalDate(row.get("start_date")));
        endDate.setValue(asLocalDate(row.get("end_date")));
    }
    @Override protected void reset() {
        titleField.clear();
        description.clear();
        targetRole.setValue("All");
        startDate.setValue(null);
        endDate.setValue(null);
    }
    @Override protected void save() throws Exception {
        String t = need(titleField.getText(), "Title");
        String role = targetRole.getValue();
        if (role == null || !Arrays.asList(TARGET_ROLES).contains(role)) {
            throw new IllegalArgumentException("Select a target role.");
        }
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();
        if (start == null || end == null) {
            throw new IllegalArgumentException("Select a start date and an end date.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (selected == null) {
            Db.update("INSERT INTO notices(admin_id,title,description,published_date,target_role,start_date,end_date) "
                            + "VALUES(?,?,?,CURDATE(),?,?,?)",
                    ADMIN_ID, t, description.getText(), role, start, end);
        } else {
            Db.update("UPDATE notices SET title=?,description=?,target_role=?,start_date=?,end_date=? "
                            + "WHERE notice_id=?",
                    t, description.getText(), role, start, end, selected.get("notice_id"));
        }
    }
    @Override protected void delete(Map<String, Object> r) throws Exception { Db.update("DELETE FROM notices WHERE notice_id=?", r.get("notice_id")); }

    private static LocalDate asLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
