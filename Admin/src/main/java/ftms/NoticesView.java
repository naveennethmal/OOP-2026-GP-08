package ftms;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Map;

public class NoticesView extends CrudView {
    private static final int ADMIN_ID = 1; // replace with the logged-in admin's id once you add login
    private final TextField titleField = new TextField();
    private final TextArea description = new TextArea();

    public NoticesView() { description.setPrefRowCount(6); description.setWrapText(true); build(); }

    @Override protected String title() { return "Notices"; }
    @Override protected String[][] columns() { return new String[][]{{"ID", "notice_id"}, {"Title", "title"}, {"Published", "published_date"}, {"Description", "description"}}; }
    @Override protected String selectSql() { return "SELECT notice_id,title,published_date,description FROM notice ORDER BY published_date DESC, notice_id DESC"; }
    @Override protected Node form() { return new VBox(10, field("Title", titleField), field("Description", description)); }
    @Override protected void fill(Map<String, Object> r) { titleField.setText(r.get("title").toString()); description.setText(r.get("description").toString()); }
    @Override protected void reset() { titleField.clear(); description.clear(); }
    @Override protected void save() throws Exception {
        String t = need(titleField.getText(), "Title");
        if (selected == null) Db.update("INSERT INTO notice(title,description,published_date,created_by) VALUES(?,?,CURDATE(),?)", t, description.getText(), ADMIN_ID);
        else Db.update("UPDATE notice SET title=?,description=? WHERE notice_id=?", t, description.getText(), selected.get("notice_id"));
    }
    @Override protected void delete(Map<String, Object> r) throws Exception { Db.update("DELETE FROM notice WHERE notice_id=?", r.get("notice_id")); }
}
