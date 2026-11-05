
package Admin;

import Common.Session;
import Database.Db;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public class MyProfileView extends VBox {

    private final int userId = Session.getUserId();

    private final TextField username = new TextField();
    private final TextField fullName = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();

    private final PasswordField currentPassword = new PasswordField();
    private final PasswordField newPassword = new PasswordField();
    private final PasswordField confirmPassword = new PasswordField();

    private final Label avatar = new Label("A");
    private final Label nameLabel = new Label("Administrator");
    private final Label emailLabel = new Label();
    private final Label roleLabel = new Label("Admin");

    private final Map<String, String> original = new HashMap<>();

    public MyProfileView() {
        setPadding(new Insets(25));
        setSpacing(22);
        setStyle("-fx-background-color: #f4f7fb;");

        Label title = new Label("My Profile");
        title.setStyle("-fx-font-size: 27px; -fx-font-weight: bold;");

        Label subtitle = new Label("Manage your account and personal details");
        subtitle.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");

        VBox heading = new VBox(5, title, subtitle);

        VBox profileCard = card();
        profileCard.setAlignment(Pos.TOP_CENTER);
        profileCard.setPrefWidth(245);
        profileCard.setMinWidth(220);
        profileCard.setMaxWidth(245);

        avatar.setMinSize(90, 90);
        avatar.setMaxSize(90, 90);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 34px;" +
                        "-fx-font-weight: bold;"
        );

        nameLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: bold;");
        emailLabel.setStyle("-fx-text-fill: #64748b;");

        roleLabel.setStyle(
                "-fx-background-color: #dbeafe;" +
                        "-fx-text-fill: #1d4ed8;" +
                        "-fx-padding: 6 15 6 15;" +
                        "-fx-background-radius: 15;"
        );

        Label accountTitle = new Label("ACCOUNT OVERVIEW");
        accountTitle.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #94a3b8;"
        );

        profileCard.getChildren().addAll(
                avatar, nameLabel, emailLabel, roleLabel,
                new Separator(), accountTitle,
                infoLabel("Account ID", String.valueOf(userId)),
                infoLabel("Account Type", "Administrator"),
                infoLabel("Profile Status", "Active")
        );

        VBox personalCard = card();
        personalCard.getChildren().addAll(
                sectionTitle("Personal Information"),
                sectionSubtitle("Update your account and personal details"),
                fieldGrid(
                        new String[]{
                                "Username", "Full Name", "Email Address",
                                "Phone Number", "Address"
                        },
                        new TextField[]{
                                username, fullName, email, phone, address
                        },
                        2
                )
        );
//button
        Button saveButton = button("Save Changes", "#2563eb");
        Button discardButton = button("Discard Changes", "#e2e8f0");
        discardButton.setStyle(
                "-fx-background-color: #e2e8f0;" +
                        "-fx-text-fill: #334155;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18 10 18;" +
                        "-fx-background-radius: 7;" +
                        "-fx-cursor: hand;"
        );

        HBox profileActions = new HBox(10, discardButton, saveButton);
        profileActions.setAlignment(Pos.CENTER_RIGHT);
        personalCard.getChildren().add(profileActions);

        VBox passwordCard = card();
        passwordCard.getChildren().addAll(
                sectionTitle("Security Settings"),
                sectionSubtitle("Change your account password"),
                fieldGrid(
                        new String[]{
                                "Current Password", "New Password", "Confirm Password"
                        },
                        new TextField[]{
                                currentPassword, newPassword, confirmPassword
                        },
                        3
                )
        );

        Button passwordButton = button("Update Password", "#0f766e");
        HBox passwordActions = new HBox(passwordButton);
        passwordActions.setAlignment(Pos.CENTER_RIGHT);
        passwordCard.getChildren().add(passwordActions);

        VBox right = new VBox(20, personalCard, passwordCard);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox content = new HBox(22, profileCard, right);
        content.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(heading, content);

        saveButton.setOnAction(e -> saveChanges());
        discardButton.setOnAction(e -> discardChanges());
        passwordButton.setOnAction(e -> updatePassword());

        fullName.textProperty().addListener((o, a, b) -> updatePreview());
        email.textProperty().addListener((o, a, b) -> updatePreview());

        loadProfile();
    }

    private VBox card() {
        VBox box = new VBox(17);
        box.setPadding(new Insets(23));
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;"
        );
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return label;
    }

    private Label sectionSubtitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        return label;
    }

    private Label infoLabel(String title, String value) {
        Label label = new Label(title + "\n" + value);
        label.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #64748b;" +
                        "-fx-line-spacing: 5px;"
        );
        return label;
    }

    private GridPane fieldGrid(String[] titles, TextField[] fields, int columns) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(16);

        for (int i = 0; i < fields.length; i++) {
            int col = i % columns;
            int row = i / columns;

            Label label = new Label(titles[i]);
            label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

            fields[i].setPromptText("Enter " + titles[i].toLowerCase());
            fields[i].setPrefHeight(40);
            fields[i].setMaxWidth(Double.MAX_VALUE);
            fields[i].setStyle(
                    "-fx-background-color: #f8fafc;" +
                            "-fx-border-color: #dbe3ef;" +
                            "-fx-border-radius: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 9;"
            );

            VBox group = new VBox(7, label, fields[i]);
            grid.add(group, col, row);
            GridPane.setHgrow(group, Priority.ALWAYS);
        }

        for (int i = 0; i < columns; i++) {
            grid.getColumnConstraints().add(
                    new ColumnConstraints(
                            0, 10000, Double.MAX_VALUE,
                            Priority.ALWAYS, HPos.LEFT, true
                    )
            );
        }

        return grid;
    }

    private Button button(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 18 10 18;" +
                        "-fx-background-radius: 7;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    private void loadProfile() {
        try {
            if (userId <= 0) {
                showMessage("Error", "Please log in again.");
                return;
            }

            List<Map<String, Object>> rows = Db.query(
                    "SELECT user_id, user_name, full_name, email, phone, address, role " +
                            "FROM users WHERE user_id = ?", userId
            );

            if (rows.isEmpty()) {
                showMessage("Error", "Profile not found.");
                return;
            }

            Map<String, Object> row = rows.get(0);

            username.setText(value(row, "user_name"));
            fullName.setText(value(row, "full_name"));
            email.setText(value(row, "email"));
            phone.setText(value(row, "phone"));
            address.setText(value(row, "address"));
            roleLabel.setText(value(row, "role"));

            saveOriginalValues();
            updatePreview();

        } catch (Exception e) {
            showMessage("Error", e.getMessage());
        }
    }

    private String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private void saveOriginalValues() {
        original.put("username", username.getText().trim());
        original.put("fullName", fullName.getText().trim());
        original.put("email", email.getText().trim());
        original.put("phone", phone.getText().trim());
        original.put("address", address.getText().trim());
    }

    private void updatePreview() {
        String name = fullName.getText().trim();

        nameLabel.setText(name.isEmpty() ? "Administrator" : name);
        emailLabel.setText(email.getText().trim());
        avatar.setText(name.isEmpty() ? "A" : name.substring(0, 1).toUpperCase());
    }

    private void saveChanges() {
        try {
            String user = username.getText().trim();
            String name = fullName.getText().trim();
            String mail = email.getText().trim();
            String phoneNo = phone.getText().trim();
            String home = address.getText().trim();

            if (user.isEmpty() || name.isEmpty() || mail.isEmpty()) {
                showMessage("Validation", "Username, name and email are required.");
                return;
            }

            if (!user.matches("[A-Za-z0-9_.-]{3,30}")) {
                showMessage(
                        "Validation",
                        "Username must be 3-30 characters and may contain letters, numbers, dot, underscore or hyphen."
                );
                return;
            }

            if (!mail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                showMessage("Validation", "Enter a valid email address.");
                return;
            }

            boolean changed =
                    !user.equals(original.get("username")) ||
                            !name.equals(original.get("fullName")) ||
                            !mail.equals(original.get("email")) ||
                            !phoneNo.equals(original.get("phone")) ||
                            !home.equals(original.get("address"));

            if (!changed) {
                showMessage("Info", "No changes to save.");
                return;
            }

            List<Map<String, Object>> duplicates = Db.query(
                    "SELECT user_id FROM users " +
                            "WHERE (user_name = ? OR email = ?) AND user_id <> ?",
                    user, mail, userId
            );

            if (!duplicates.isEmpty()) {
                showMessage(
                        "Validation",
                        "Username or email is already used by another account."
                );
                return;
            }

            int result = Db.update(
                    "UPDATE users SET user_name=?, full_name=?, email=?, phone=?, address=? " +
                            "WHERE user_id=?",
                    user, name, mail, phoneNo, home, userId
            );

            if (result > 0) {
                saveOriginalValues();
                updatePreview();
                showMessage("Success", "Profile updated successfully.");
            } else {
                showMessage("Info", "No changes were saved.");
            }

        } catch (Exception e) {
            showMessage("Error", e.getMessage());
        }
    }

    private void discardChanges() {
        username.setText(original.getOrDefault("username", ""));
        fullName.setText(original.getOrDefault("fullName", ""));
        email.setText(original.getOrDefault("email", ""));
        phone.setText(original.getOrDefault("phone", ""));
        address.setText(original.getOrDefault("address", ""));
        updatePreview();
    }

    private void updatePassword() {
        String current = currentPassword.getText();
        String next = newPassword.getText();
        String confirm = confirmPassword.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showMessage("Validation", "Fill in all password fields.");
            return;
        }

        if (next.length() < 8) {
            showMessage("Validation", "Password must have at least 8 characters.");
            return;
        }

        if (!next.equals(confirm)) {
            showMessage("Validation", "Passwords do not match.");
            return;
        }

        if (current.equals(next)) {
            showMessage("Validation", "Choose a different new password.");
            return;
        }

        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT password FROM users WHERE user_id = ?", userId
            );

            if (rows.isEmpty() ||
                    !current.equals(String.valueOf(rows.get(0).get("password")))) {
                showMessage("Error", "Current password is incorrect.");
                return;
            }

            int result = Db.update(
                    "UPDATE users SET password = ? WHERE user_id = ?",
                    next, userId
            );

            if (result > 0) {
                currentPassword.clear();
                newPassword.clear();
                confirmPassword.clear();
                showMessage("Success", "Password updated successfully.");
            } else {
                showMessage("Error", "Password was not updated.");
            }

        } catch (Exception e) {
            showMessage("Error", e.getMessage());
        }
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }
}
//commit check this project