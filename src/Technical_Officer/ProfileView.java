package Technical_Officer;

import Technical_Officer.ProfileDAO.TechnicianProfile;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;

/** Username and password are shown but locked; everything else can be edited. */
public class ProfileView extends VBox {

    private TechnicianProfile profile;
    private String pickedPhoto;                       // new photo chosen but not saved yet
    private final Runnable onProfileChange;

    private final Circle avatar = new Circle(48, Color.web("#2563eb"));
    private final Label initials = new Label();
    private final Label nameLabel = new Label();

    public ProfileView() {
        this(() -> {});
    }

    public ProfileView(Runnable onProfileChange) {
        this.onProfileChange = onProfileChange;
        getStyleClass().add("page");
        setSpacing(20);
        getChildren().add(UiKit.pageHeader("My profile", "Update your personal details"));

        try {
            profile = ProfileDAO.get();
            HBox body = new HBox(18, buildProfileCard(), buildFormCard());
            body.setFillHeight(false);
            getChildren().add(body);
        } catch (SQLException e) {
            UiKit.showLoadError(this, e);
        }
    }

    private void showPhoto(String path) {
        initials.setText(profile.initials());
        if (path != null && !path.isBlank()) {
            File file = new File(path);
            if (file.isFile()) {
                avatar.setFill(new ImagePattern(new Image(file.toURI().toString())));
                initials.setVisible(false);
                return;
            }
        }
        avatar.setFill(Color.web("#2563eb"));
        initials.setVisible(true);
    }

    private VBox buildProfileCard() {
        initials.getStyleClass().add("avatar-text");
        StackPane picture = new StackPane(avatar, initials);
        showPhoto(profile.picture());

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
                ProfileDAO.removePicture();
                profile = ProfileDAO.get();
                pickedPhoto = null;
                showPhoto(profile.picture());
                onProfileChange.run();
            } catch (SQLException ex) {
                UiKit.showError("Could not remove the profile photo.", ex);
            }
        });
        HBox photoActions = new HBox(8, change, remove);

        nameLabel.setText(profile.fullName());
        nameLabel.getStyleClass().add("profile-name");
        Label dept = new Label(profile.department());
        dept.getStyleClass().add("hint");

        VBox card = UiKit.card(picture, nameLabel, UiKit.pill("Technical Officer", "info"), dept, photoActions);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(280);
        card.setMinWidth(280);
        return card;
    }

    private VBox buildFormCard() {
        TextField fullName = new TextField(profile.fullName());
        TextField email = new TextField(profile.email());
        TextField phone = new TextField(profile.phone());
        TextField address = new TextField(profile.address());

        TextField username = new TextField(profile.username());
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
                new Alert(Alert.AlertType.WARNING, "Full name and email cannot be empty.").show();
                return;
            }

            try {
                ProfileDAO.save(fullName.getText().trim(), email.getText().trim(),
                        phone.getText().trim(), address.getText().trim(), pickedPhoto);
                profile = ProfileDAO.get();
                pickedPhoto = null;
                nameLabel.setText(profile.fullName());
                initials.setText(profile.initials());
                showPhoto(profile.picture());
                onProfileChange.run();
                new Alert(Alert.AlertType.INFORMATION, "Profile updated.").show();
            } catch (SQLException ex) {
                UiKit.showError("Could not update the profile.", ex);
            }
        });
        Button reset = UiKit.ghostButton("Reset", "fas-undo");
        reset.setOnAction(e -> {
            fullName.setText(profile.fullName());
            email.setText(profile.email());
            phone.setText(profile.phone());
            address.setText(profile.address());
            pickedPhoto = null;
            showPhoto(profile.picture());
        });
        HBox buttons = new HBox(10, save, reset);

        VBox card = UiKit.card(UiKit.cardTitle("Personal details"), grid, note, buttons);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
}
