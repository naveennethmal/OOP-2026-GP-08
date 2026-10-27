package Technical_Officer;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Map;

import static Technical_Officer.TechnicianDAO.text;

// Username and password are shown but locked; everything else can be edited.
public class ProfileView extends VBox {

    private Map<String, Object> profile;
    private String pickedPhoto;                       // new photo chosen but not saved yet
    private final Runnable onProfileChange;

    private final Circle avatar = new Circle(48, Color.web("#2563eb"));
    private final Label initials = new Label();
    private final Label nameLabel = new Label();

    public ProfileView(Runnable onProfileChange) {
        this.onProfileChange = onProfileChange;
        getStyleClass().add("page");
        setSpacing(20);
        getChildren().add(UiKit.pageHeader("My profile", "Update your personal details"));

        try {
            profile = TechnicianDAO.profile();
            HBox body = new HBox(18, buildProfileCard(), buildFormCard());
            body.setFillHeight(false);
            getChildren().add(body);
        } catch (Exception e) {
            UiKit.showLoadError(this, e);
        }
    }

    private void showPhoto(String path) {
        UiKit.setAvatar(avatar, initials, text(profile.get("full_name")), path);
    }

    private VBox buildProfileCard() {
        initials.getStyleClass().add("avatar-text");
        showPhoto(text(profile.get("profile_picture")));

        Button change = UiKit.ghostButton("Change photo", "fas-camera");
        change.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) {
                pickedPhoto = file.getAbsolutePath();
                showPhoto(pickedPhoto);
            }
        });

        Button remove = UiKit.ghostButton("Remove photo", "fas-trash");
        remove.setOnAction(e -> {
            try {
                TechnicianDAO.removePicture();
                profile = TechnicianDAO.profile();
                pickedPhoto = null;
                showPhoto(text(profile.get("profile_picture")));
                onProfileChange.run();
            } catch (Exception ex) {
                UiKit.showError("Could not remove the profile photo.", ex);
            }
        });

        nameLabel.setText(text(profile.get("full_name")));
        nameLabel.getStyleClass().add("profile-name");
        Label dept = new Label(text(profile.get("department_name")));
        dept.getStyleClass().add("hint");

        VBox card = UiKit.card(new StackPane(avatar, initials), nameLabel,
                UiKit.pill("Technical Officer", "info"), dept, new HBox(8, change, remove));
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(280);
        card.setMinWidth(280);
        return card;
    }

    private VBox buildFormCard() {
        TextField fullName = new TextField();
        TextField email = new TextField();
        TextField phone = new TextField();
        TextField address = new TextField();
        fillForm(fullName, email, phone, address);

        TextField username = new TextField(text(profile.get("user_name")));
        username.setDisable(true);
        PasswordField password = new PasswordField();
        password.setText("********");
        password.setDisable(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
        }
        grid.add(UiKit.field("Full name", fullName), 0, 0);
        grid.add(UiKit.field("Email", email), 1, 0);
        grid.add(UiKit.field("Phone", phone), 0, 1);
        grid.add(UiKit.field("Address", address), 1, 1);
        grid.add(UiKit.field("Username (locked)", username), 0, 2);
        grid.add(UiKit.field("Password (locked)", password), 1, 2);

        Label note = new Label("Your username and password can only be changed by an administrator.");
        note.getStyleClass().add("hint");

        Button save = UiKit.primaryButton("Save changes", "fas-save");
        save.setOnAction(e -> {
            if (fullName.getText().isBlank() || email.getText().isBlank()) {
                UiKit.warn("Full name and email cannot be empty.");
                return;
            }
            try {
                TechnicianDAO.saveProfile(fullName.getText().trim(), email.getText().trim(),
                        phone.getText().trim(), address.getText().trim(), pickedPhoto);
                profile = TechnicianDAO.profile();
                pickedPhoto = null;
                nameLabel.setText(text(profile.get("full_name")));
                showPhoto(text(profile.get("profile_picture")));
                onProfileChange.run();
                UiKit.info("Profile updated.");
            } catch (Exception ex) {
                UiKit.showError("Could not update the profile.", ex);
            }
        });
        Button reset = UiKit.ghostButton("Reset", "fas-undo");
        reset.setOnAction(e -> {
            fillForm(fullName, email, phone, address);
            pickedPhoto = null;
            showPhoto(text(profile.get("profile_picture")));
        });

        VBox card = UiKit.card(UiKit.cardTitle("Personal details"), grid, note, new HBox(10, save, reset));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void fillForm(TextField fullName, TextField email, TextField phone, TextField address) {
        fullName.setText(text(profile.get("full_name")));
        email.setText(text(profile.get("email")));
        phone.setText(text(profile.get("phone")));
        address.setText(text(profile.get("address")));
    }
}
