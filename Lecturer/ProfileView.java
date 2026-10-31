package Lecturer;

import Common.Session;
import Lecturer.LecturerDAO.Profile;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLIntegrityConstraintViolationException;

import static Lecturer.LecturerUi.*;

/** The lecturer can change everything except username, password and employee number. */
public class ProfileView extends VBox {

    private Profile profile;
    private File pickedPhoto;                        // chosen but not saved yet
    private final Runnable onChange;

    private final Circle avatar = new Circle(48, Color.web("#2563eb"));
    private final Label initials = new Label();
    private final Label message = new Label();

    public ProfileView(Runnable onChange) {
        this.onChange = onChange;
        getStyleClass().add("page");
        setSpacing(18);
        getChildren().add(header("My profile", "Update your personal details"));

        try {
            profile = LecturerDAO.profile();
            HBox body = new HBox(18, photoCard(), formCard());
            body.setFillHeight(false);
            getChildren().add(body);
        } catch (Exception e) {
            getChildren().add(errorLabel("Could not load your profile.", e));
        }
    }

    private VBox photoCard() {
        initials.getStyleClass().add("avatar-text");
        paintAvatar(avatar, initials, profile.picture(), profile.initials());

        Button change = ghost("Change photo");
        change.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) {
                pickedPhoto = file;
                paintAvatar(avatar, initials, file.getAbsolutePath(), profile.initials());
                say(message, "Photo selected. Press Save changes to keep it.", true);
            }
        });

        Button remove = ghost("Remove photo");
        remove.setOnAction(e -> {
            try {
                LecturerDAO.removePicture();
                pickedPhoto = null;
                profile = LecturerDAO.profile();
                paintAvatar(avatar, initials, null, profile.initials());
                onChange.run();
            } catch (Exception ex) {
                say(message, "Could not remove the photo: " + ex.getMessage(), false);
            }
        });

        Label name = new Label(profile.fullName());
        name.getStyleClass().add("profile-name");
        VBox card = card(new StackPane(avatar, initials), name,
                tag("Lecturer"),
                hint(profile.department()), new HBox(8, change, remove));
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(280);
        card.setMinWidth(280);
        return card;
    }

    private Label tag(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("pill", "pill-info");
        return l;
    }

    private VBox formCard() {
        TextField fullName = new TextField(profile.fullName());
        TextField email = new TextField(profile.email());
        TextField phone = new TextField(profile.phone());
        TextField address = new TextField(profile.address());

        TextField username = new TextField(profile.username());
        TextField employee = new TextField(profile.employeeNo());
        PasswordField password = new PasswordField();
        password.setText("********");
        username.setDisable(true);
        employee.setDisable(true);
        password.setDisable(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
        }
        grid.add(field("Full name", fullName), 0, 0);
        grid.add(field("Email", email), 1, 0);
        grid.add(field("Phone", phone), 0, 1);
        grid.add(field("Address", address), 1, 1);
        grid.add(field("Username (locked)", username), 0, 2);
        grid.add(field("Password (locked)", password), 1, 2);
        grid.add(field("Employee no (locked)", employee), 0, 3);

        Button save = primary("Save changes");
        save.setOnAction(e -> save(fullName.getText().trim(), email.getText().trim(),
                phone.getText().trim(), address.getText().trim()));

        VBox card = card(cardTitle("Personal details"), grid,
                hint("Your username and password can only be changed by an administrator."), save, message);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void save(String name, String email, String phone, String address) {
        if (name.isEmpty()) { say(message, "Full name cannot be empty.", false); return; }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            say(message, "Please enter a valid email address.", false);
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9+ ]{7,15}$")) {
            say(message, "Phone can only contain digits, + and spaces (7 to 15 characters).", false);
            return;
        }

        try {
            String picturePath = pickedPhoto == null ? null : copyPhoto(pickedPhoto);
            LecturerDAO.saveProfile(name, email, phone, address, picturePath);
            pickedPhoto = null;
            profile = LecturerDAO.profile();
            say(message, "Profile updated successfully.", true);
            onChange.run();
        } catch (SQLIntegrityConstraintViolationException ex) {
            say(message, "That email is already used by another user.", false);
        } catch (Exception ex) {
            say(message, "Could not save: " + ex.getMessage(), false);
        }
    }

    /** Same place and name style as the student profile: profile_pictures/profile_<userId>.<ext>. */
    private String copyPhoto(File file) throws Exception {
        Path folder = Paths.get(System.getProperty("user.dir"), "profile_pictures");
        Files.createDirectories(folder);
        String name = file.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : ".png";
        String newName = "profile_" + Session.getUserId() + ext;
        Files.copy(file.toPath(), folder.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
        return "profile_pictures/" + newName;
    }
}
