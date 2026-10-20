import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLIntegrityConstraintViolationException;

public class LecturerProfilePage extends VBox {

    private final Lecturer lecturer;
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField addressField = new TextField();
    private final ImageView photo = new ImageView();
    private final Label message = new Label();
    private String pictureName;

    public LecturerProfilePage(Lecturer lecturer) {
        this.lecturer = lecturer;
        setSpacing(18);

        Label title = new Label("My Profile");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Read-only fields (cannot be changed by the lecturer)
        TextField usernameField = new TextField(lecturer.getUsername());
        usernameField.setEditable(false);
        usernameField.setStyle("-fx-opacity: 0.7;");
        TextField empField = new TextField(lecturer.getEmployeeNo());
        empField.setEditable(false);
        empField.setStyle("-fx-opacity: 0.7;");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);
        form.setMaxWidth(520);
        form.setPrefWidth(520);
        ColumnConstraints c1 = new ColumnConstraints(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        form.addRow(0, new Label("Username"), usernameField);
        form.addRow(1, new Label("Employee No"), empField);
        form.addRow(2, new Label("Full Name"), nameField);
        form.addRow(3, new Label("Email"), emailField);
        form.addRow(4, new Label("Phone"), phoneField);
        form.addRow(5, new Label("Address"), addressField);

        photo.setFitWidth(120);
        photo.setFitHeight(120);
        photo.setPreserveRatio(true);
        Button choosePic = new Button("Change Picture");
        choosePic.setOnAction(e -> choosePicture());
        VBox picBox = new VBox(10, photo, choosePic);

        Button save = new Button("Save Changes");
        Button cancel = new Button("Cancel");
        save.setOnAction(e -> save());
        cancel.setOnAction(e -> { fillFields(); message.setText(""); });
        HBox buttons = new HBox(10, save, cancel);

        Label note = new Label("Username and password can only be changed by the Admin.");
        note.setStyle("-fx-text-fill: #666;");

        HBox top = new HBox(40, form, picBox);
        getChildren().addAll(title, top, buttons, message, note);
        setPadding(new Insets(0));

        fillFields();
    }

    private void fillFields() {
        nameField.setText(lecturer.getFullName());
        emailField.setText(lecturer.getEmail());
        phoneField.setText(lecturer.getPhone());
        addressField.setText(lecturer.getAddress());
        pictureName = lecturer.getProfilePicture();
        loadPhoto();
    }

    private void loadPhoto() {
        try {
            if (pictureName != null) {
                File f = new File("images/" + pictureName);
                if (f.exists()) {
                    photo.setImage(new Image(f.toURI().toString(), 120, 120, true, true));
                    return;
                }
            }
        } catch (Exception ignored) { }
        photo.setImage(null);
    }

    private void choosePicture() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File chosen = fc.showOpenDialog(getScene().getWindow());
        if (chosen == null) return;
        try {
            String name = chosen.getName();
            String ext = name.substring(name.lastIndexOf('.'));
            String newName = "lecturer_" + lecturer.getLecturerId() + ext;
            File dir = new File("images");
            Files.createDirectories(dir.toPath());
            Files.copy(chosen.toPath(), new File(dir, newName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            pictureName = newName;   // only the file name goes in the database
            loadPhoto();
        } catch (Exception ex) {
            showError("Could not load picture: " + ex.getMessage());
        }
    }

    private void save() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty()) { showError("Full name cannot be empty"); return; }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            showError("Please enter a valid email"); return;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9+ ]{7,15}$")) {
            showError("Phone can only contain digits, + and spaces (7-15 characters)"); return;
        }

        try {
            lecturer.setFullName(name);
            lecturer.setEmail(email);
            lecturer.setPhone(phone);
            lecturer.setAddress(address);
            lecturer.setProfilePicture(pictureName);

            new LecturerDAO().updateProfile(lecturer);

            message.setStyle("-fx-text-fill: green;");
            message.setText("Profile updated successfully");
        } catch (SQLIntegrityConstraintViolationException ex) {
            showError("That email is already used by another user");
        } catch (Exception ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String text) {
        message.setStyle("-fx-text-fill: red;");
        message.setText(text);
    }
}