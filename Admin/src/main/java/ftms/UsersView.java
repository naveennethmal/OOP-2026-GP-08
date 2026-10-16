package ftms;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Map;

public class UsersView extends CrudView {
    private final TextField username = new TextField(), fullName = new TextField(), email = new TextField(),
            phone = new TextField(), address = new TextField();
    private final PasswordField password = new PasswordField();
    private final ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(
            "ADMIN", "LECTURER", "TECHNICAL_OFFICER", "UNDERGRADUATE"));

    public UsersView() { role.setValue("UNDERGRADUATE"); password.setPromptText("Leave blank to keep current"); build(); }

    @Override protected String title() { return "User profiles"; }
    @Override protected String[][] columns() {
        return new String[][]{{"ID", "user_id"}, {"Username", "username"}, {"Full name", "full_name"},
                {"Role", "role"}, {"Email", "email"}, {"Phone", "phone"}};
    }
    @Override protected String selectSql() { return "SELECT user_id,username,full_name,role,email,phone,address FROM users ORDER BY user_id"; }

    @Override protected Node form() {
        return new VBox(10, field("Username", username), field("Password", password), field("Full name", fullName),
                field("Role", role), field("Email", email), field("Phone", phone), field("Address", address));
    }

    @Override protected void fill(Map<String, Object> r) {
        username.setText(r.get("username").toString()); fullName.setText(r.get("full_name").toString());
        email.setText(r.get("email").toString()); phone.setText(r.get("phone").toString());
        address.setText(r.get("address").toString()); role.setValue(r.get("role").toString()); password.clear();
    }
    @Override protected void reset() {
        username.clear(); fullName.clear(); email.clear(); phone.clear(); address.clear(); password.clear(); role.setValue("UNDERGRADUATE");
    }

    @Override protected void save() throws Exception {
        String u = need(username.getText(), "Username"), n = need(fullName.getText(), "Full name");
        if (!email.getText().isBlank() && !email.getText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Email format is invalid.");
        if (!phone.getText().isBlank() && !phone.getText().matches("^[0-9+\\- ]{9,15}$"))
            throw new IllegalArgumentException("Phone number is invalid.");
        if (selected == null) {
            String pw = need(password.getText(), "Password");
            Db.update("INSERT INTO users(username,password_hash,full_name,email,phone,address,role) VALUES(?,?,?,?,?,?,?)",
                    u, Db.sha256(pw), n, email.getText(), phone.getText(), address.getText(), role.getValue());
        } else {
            Object id = selected.get("user_id");
            Db.update("UPDATE users SET username=?,full_name=?,email=?,phone=?,address=?,role=? WHERE user_id=?",
                    u, n, email.getText(), phone.getText(), address.getText(), role.getValue(), id);
            if (!password.getText().isBlank()) Db.update("UPDATE users SET password_hash=? WHERE user_id=?", Db.sha256(password.getText()), id);
        }
    }

    @Override protected void delete(Map<String, Object> r) throws Exception {
        Db.update("DELETE FROM users WHERE user_id=?", r.get("user_id"));
    }
}
