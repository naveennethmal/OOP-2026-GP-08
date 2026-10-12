package com.teclms.technician;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;

/** Username and password are shown but locked; everything else can be edited. */
public class ProfileView extends VBox {

    public ProfileView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox body = new HBox(18, buildProfileCard(), buildFormCard());
        body.setFillHeight(false);
        getChildren().addAll(UiKit.pageHeader("My profile", "Update your personal details"), body);
    }

    private VBox buildProfileCard() {
        Circle avatar = new Circle(48, Color.web("#2563eb"));
        Label initials = new Label("KP");
        initials.getStyleClass().add("avatar-text");
        StackPane picture = new StackPane(avatar, initials);

        Button change = UiKit.ghostButton("Change photo", "fas-camera");
        change.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) {
                avatar.setFill(new ImagePattern(new Image(file.toURI().toString())));
                initials.setVisible(false);
            }
        });

        Label name = new Label("Kasun Perera");
        name.getStyleClass().add("profile-name");
        Label dept = new Label("Department of ICT");
        dept.getStyleClass().add("hint");

        VBox card = UiKit.card(picture, name, UiKit.pill("Technical Officer", "info"), dept, change);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(280);
        card.setMinWidth(280);
        return card;
    }

    private VBox buildFormCard() {
        TextField fullName = new TextField("Kasun Perera");
        TextField email = new TextField("kasun.perera@fot.example.lk");
        TextField phone = new TextField("077 123 4567");
        TextField address = new TextField("No. 24, Wakwella Road, Galle");

        TextField username = new TextField("to_kperera");
        username.setDisable(true);
        PasswordField password = new PasswordField();
        password.setText("password");
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
        save.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Profile updated.").show());
        Button reset = UiKit.ghostButton("Reset", "fas-undo");
        reset.setOnAction(e -> {
            fullName.setText("Kasun Perera");
            email.setText("kasun.perera@fot.example.lk");
            phone.setText("077 123 4567");
            address.setText("No. 24, Wakwella Road, Galle");
        });
        HBox buttons = new HBox(10, save, reset);

        VBox card = UiKit.card(UiKit.cardTitle("Personal details"), grid, note, buttons);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
}
