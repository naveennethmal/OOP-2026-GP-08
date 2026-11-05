package Student;

import Common.Session;
import Database.DBConnection;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MyProfile extends VBox {

    private final List<TextField> fields = new ArrayList<>();

    // Phone field
    private TextField phoneField;
    private TextField nicField;
    private Button editNicButton;

    // Profile picture
    private ImageView profileImageView;

    // Profile image container
    private StackPane profileImageBox;
    private Label profileDepartment;

    // Selected student ID
    private String studentId;
    private final Runnable onProfilePictureChanged;

    public MyProfile() {
        this(() -> {});
    }

    public MyProfile(Runnable onProfilePictureChanged) {
        super(18);
        this.onProfilePictureChanged = onProfilePictureChanged;

        getStyleClass().add("content");

        // =====================================================
        // PROFILE HEADER
        // =====================================================

        profileImageBox = createProfileImage();

        // =====================================================
        // PROFILE PICTURE BUTTON
        // =====================================================

        Button changePicture =
                new Button("Change Profile Picture");

        changePicture.getStyleClass().add(
                "primary-btn"
        );

        changePicture.setOnAction(
                e -> chooseProfilePicture()
        );

        Button removePicture =
                new Button("Remove Profile Picture");
        removePicture.getStyleClass().add("btn-ghost");
        removePicture.setOnAction(e -> removeProfilePicture());

        // =====================================================
        // FORM
        // =====================================================

        GridPane form = new GridPane();

        form.setHgap(24);
        form.setVgap(16);

        for (int i = 0; i < 2; i++) {

            ColumnConstraints cc =
                    new ColumnConstraints();

            cc.setPercentWidth(50);

            form.getColumnConstraints().add(cc);
        }

        String[] labels = {
                "Full Name",
                "Student ID",
                "Email",
                "Contact Number",
                "Address",
                "Department",
                "Batch",
                "Level",
                "Status",
                "NIC Number",
                "Birthday",
                "Days until next birthday"
        };

        for (int i = 0; i < labels.length; i++) {

            TextField tf =
                    new TextField();

            tf.setEditable(false);

            tf.getStyleClass().add(
                    "field"
            );

            // Contact Number field
            if (i == 3) {

                phoneField = tf;

                // Only contact number can be edited
                phoneField.setEditable(false);
            }

            if (i >= 10) {
                tf.setEditable(false);
            } else if (i == 9) {
                nicField = tf;
                nicField.setEditable(false);
            }

            fields.add(tf);

            form.add(
                    new VBox(
                            6,
                            Ui.text(
                                    labels[i],
                                    "field-label"
                            ),
                            tf
                    ),
                    i % 2,
                    i / 2
            );
        }

        // =====================================================
        // EDIT CONTACT BUTTON
        // =====================================================

        Button editContact =
                new Button("Edit Contact Number");

        editContact.getStyleClass().add(
                "primary-btn"
        );

        editContact.setOnAction(e -> {

            if (!phoneField.isEditable()) {

                phoneField.setEditable(true);

                phoneField.requestFocus();

                editContact.setText(
                        "💾  Save Contact Number"
                );

            } else {

                updateContactNumber();

                if (!phoneField.isEditable()) {

                    editContact.setText(
                            "✏  Edit Contact Number"
                    );
                }
            }
        });

        editNicButton = new Button("Edit NIC Number");
        editNicButton.getStyleClass().add("primary-btn");
        editNicButton.setOnAction(e -> {
            if (!nicField.isEditable()) {
                nicField.setEditable(true);
                nicField.requestFocus();
                editNicButton.setText("Save NIC Number");
            } else {
                updateNicNumber();
            }
        });

        VBox photoCard = new VBox(14);
        photoCard.getStyleClass().add("card");
        photoCard.setAlignment(Pos.TOP_CENTER);
        photoCard.setMinWidth(245);
        photoCard.setPrefWidth(270);
        photoCard.setMaxWidth(300);

        Label name = Ui.text(Session.getFullName() != null ? Session.getFullName() : "Student", "profile-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);
        profileDepartment = Ui.text("Department", "muted");
        Label registration = Ui.text("Student ID: " + (Session.getRegNo() != null ? Session.getRegNo() : "-"), "muted");
        photoCard.getChildren().addAll(
                profileImageBox,
                name,
                Ui.tag("Student", "blue"),
                profileDepartment,
                registration,
                changePicture,
                removePicture
        );
        changePicture.setMaxWidth(Double.MAX_VALUE);
        removePicture.setMaxWidth(Double.MAX_VALUE);

        Button resetContact = new Button("Reset");
        resetContact.getStyleClass().add("btn-ghost");
        resetContact.setOnAction(e -> loadProfile());
        HBox formActions = new HBox(10, editContact, resetContact);
        formActions.setAlignment(Pos.CENTER_LEFT);

        formActions.getChildren().add(editNicButton);
        VBox formBody = new VBox(18, form, Ui.text("Your contact number and NIC number can be updated here. Contact an administrator to change other details.", "muted"), formActions);
        VBox detailsCard = Ui.card("Personal details", formBody);
        detailsCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(detailsCard, Priority.ALWAYS);

        HBox profileLayout = new HBox(22, photoCard, detailsCard);
        profileLayout.setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(
                Ui.header("My Profile", "Update your personal details"),
                profileLayout
        );

        // =====================================================
        // LOAD DATABASE DATA
        // =====================================================

        loadProfile();
    }

    // =========================================================
    // CREATE PROFILE IMAGE
    // =========================================================

    private StackPane createProfileImage() {

        profileImageView =
                new ImageView();

        profileImageView.setFitWidth(100);
        profileImageView.setFitHeight(100);

        profileImageView.setPreserveRatio(true);

        // Circle image
        Circle clip =
                new Circle(
                        50,
                        50,
                        50
                );

        profileImageView.setClip(
                clip
        );

        profileImageBox =
                new StackPane(
                        profileImageView
                );

        profileImageBox.setPrefSize(
                100,
                100
        );

        profileImageBox.setMinSize(
                100,
                100
        );

        profileImageBox.setMaxSize(
                100,
                100
        );

        // Default avatar
        setDefaultAvatar();

        return profileImageBox;
    }

    // =========================================================
    // DEFAULT AVATAR
    // =========================================================

    private void setDefaultAvatar() {

        String fullName =
                Session.getFullName();

        String initials =
                getInitials(fullName);

        Label avatar =
                new Label(initials);

        avatar.getStyleClass().add("avatar");

        avatar.setPrefSize(
                100,
                100
        );

        profileImageBox
                .getChildren()
                .clear();

        profileImageBox
                .getChildren()
                .add(avatar);
    }

    // =========================================================
    // GET INITIALS
    // =========================================================

    private String getInitials(
            String name
    ) {

        if (name == null ||
                name.isBlank()) {

            return "ST";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                parts[0]
                        .substring(0, 1)
                        .toUpperCase()

                        +

                        parts[parts.length - 1]
                                .substring(0, 1)
                                .toUpperCase()
        );
    }

    // =========================================================
    // LOAD PROFILE FROM DATABASE
    // =========================================================

    private void loadProfile() {

        nicField.setEditable(false);
        editNicButton.setText("Edit NIC Number");

        studentId =
                Session.getRegNo();

        if (studentId == null ||
                studentId.isBlank()) {

            showAlert(
                    "No logged-in student found."
            );

            return;
        }

        try {
            ensureNicColumn();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error while preparing the NIC number field.");
            return;
        }

        String sql = """
                SELECT
                    u.full_name,
                    u.email,
                    u.phone,
                    u.address,
                    u.profile_picture,
                    ug.reg_no,
                    ug.batch,
                    ug.level,
                    ug.status,
                    ug.nic,
                    d.department_name
                FROM users u
                JOIN undergraduates ug
                    ON u.user_id = ug.user_id
                JOIN departments d
                    ON ug.department_id = d.department_id
                WHERE ug.reg_no = ?
                """;

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    studentId
            );

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                // ---------------- DATABASE VALUES ----------------

                String fullName =
                        result.getString(
                                "full_name"
                        );

                String email =
                        result.getString(
                                "email"
                        );

                String phone =
                        result.getString(
                                "phone"
                        );

                String address =
                        result.getString(
                                "address"
                        );

                String regNo =
                        result.getString(
                                "reg_no"
                        );

                String batch =
                        result.getString(
                                "batch"
                        );

                String level =
                        result.getString(
                                "level"
                        );

                String status =
                        result.getString(
                                "status"
                        );

                String nic =
                        result.getString(
                                "nic"
                        );

                String department =
                        result.getString(
                                "department_name"
                        );

                String profilePicture =
                        result.getString(
                                "profile_picture"
                        );

                // ---------------- SET UI VALUES ----------------

                fields.get(0).setText(
                        safe(fullName)
                );

                fields.get(1).setText(
                        safe(regNo)
                );

                fields.get(2).setText(
                        safe(email)
                );

                fields.get(3).setText(
                        safe(phone)
                );

                fields.get(4).setText(
                        safe(address)
                );

                fields.get(5).setText(
                        safe(department)
                );
                profileDepartment.setText(safe(department));

                fields.get(6).setText(
                        safe(batch)
                );

                fields.get(7).setText(
                        safe(level)
                );

                fields.get(8).setText(
                        safe(status)
                );

                fields.get(9).setText(safe(nic));
                updateBirthdayDisplay(nic);

                // Load profile picture
                loadProfilePicture(
                        profilePicture
                );

            } else {

                showAlert(
                        "Student profile not found."
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Database error while loading profile."
            );
        }
    }

    // =========================================================
    // LOAD PROFILE PICTURE
    // =========================================================

    private void loadProfilePicture(
            String picturePath
    ) {

        if (picturePath == null ||
                picturePath.isBlank()) {

            setDefaultAvatar();

            return;
        }

        try {

            Path path =
                    Paths.get(picturePath);

            if (!path.isAbsolute()) {

                path =
                        Paths.get(
                                System.getProperty(
                                        "user.dir"
                                )
                        ).resolve(
                                picturePath
                        );
            }

            if (!Files.exists(path)) {

                setDefaultAvatar();

                return;
            }

            Image image =
                    new Image(
                            path.toUri().toString(),
                            100,
                            100,
                            true,
                            true
                    );

            profileImageView.setImage(
                    image
            );

            profileImageBox
                    .getChildren()
                    .clear();

            profileImageBox
                    .getChildren()
                    .add(
                            profileImageView
                    );

        } catch (Exception e) {

            e.printStackTrace();

            setDefaultAvatar();
        }
    }

    // =========================================================
    // CHOOSE PROFILE PICTURE
    // =========================================================

    private void chooseProfilePicture() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Choose Profile Picture"
        );

        FileChooser.ExtensionFilter imageFilter =
                new FileChooser.ExtensionFilter(
                        "Image Files (*.png, *.jpg, *.jpeg)",
                        "*.png",
                        "*.jpg",
                        "*.jpeg"
                );

        fileChooser.getExtensionFilters()
                .add(imageFilter);

        File selectedFile =
                fileChooser.showOpenDialog(
                        getScene().getWindow()
                );

        if (selectedFile == null) {
            return;
        }

        try {

            // =================================================
            // CREATE PROFILE PICTURES FOLDER
            // =================================================

            Path folder =
                    Paths.get(
                            System.getProperty(
                                    "user.dir"
                            ),
                            "profile_pictures"
                    );

            if (!Files.exists(folder)) {

                Files.createDirectories(
                        folder
                );
            }

            // =================================================
            // GET FILE EXTENSION
            // =================================================

            String originalName =
                    selectedFile.getName();

            String extension =
                    "";

            int dot =
                    originalName.lastIndexOf(".");

            if (dot >= 0) {

                extension =
                        originalName.substring(
                                dot
                        ).toLowerCase();
            }

            // =================================================
            // CREATE NEW FILE NAME
            // =================================================

            int userId =
                    Session.getUserId();

            String newFileName =
                    "profile_"
                            + userId
                            + extension;

            Path destination =
                    folder.resolve(
                            newFileName
                    );

            // =================================================
            // COPY IMAGE
            // =================================================

            Files.copy(
                    selectedFile.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // Database path
            String databasePath =
                    "profile_pictures/"
                            + newFileName;

            // =================================================
            // UPDATE DATABASE
            // =================================================

            updateProfilePicture(
                    databasePath
            );

        } catch (IOException e) {

            e.printStackTrace();

            showAlert(
                    "Error while uploading profile picture."
            );
        }
    }

    // =========================================================
    // UPDATE PROFILE PICTURE DATABASE
    // =========================================================

    private void updateProfilePicture(
            String imagePath
    ) {

        String sql = """
                UPDATE users u
                JOIN undergraduates ug
                    ON u.user_id = ug.user_id
                SET u.profile_picture = ?
                WHERE ug.reg_no = ?
                """;

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    imagePath
            );

            statement.setString(
                    2,
                    studentId
            );

            int updated =
                    statement.executeUpdate();

            if (updated > 0) {

                // Display newly selected image
                loadProfilePicture(
                        imagePath
                );
                onProfilePictureChanged.run();

                showAlert(
                        "Profile picture updated successfully."
                );

            } else {

                showAlert(
                        "Profile picture update failed."
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Database error while saving profile picture."
            );
        }
    }

    private void removeProfilePicture() {
        if (studentId == null || studentId.isBlank()) {
            showAlert("No logged-in student found.");
            return;
        }

        String sql = """
                UPDATE users u
                JOIN undergraduates ug ON u.user_id = ug.user_id
                SET u.profile_picture = NULL
                WHERE ug.reg_no = ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            if (statement.executeUpdate() > 0) {
                setDefaultAvatar();
                onProfilePictureChanged.run();
                showAlert("Profile picture removed.");
            } else {
                showAlert("Profile picture could not be removed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error while removing profile picture.");
        }
    }

    // =========================================================
    // UPDATE CONTACT NUMBER
    // =========================================================

    private void updateContactNumber() {

        String phone =
                phoneField
                        .getText()
                        .trim();

        if (phone.isEmpty()) {

            showAlert(
                    "Contact number cannot be empty."
            );

            return;
        }

        // Basic phone validation
        if (!phone.matches(
                "[0-9+\\- ]{7,20}"
        )) {

            showAlert(
                    "Please enter a valid contact number."
            );

            return;
        }

        String sql = """
                UPDATE users u
                JOIN undergraduates ug
                    ON u.user_id = ug.user_id
                SET u.phone = ?
                WHERE ug.reg_no = ?
                """;

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    phone
            );

            statement.setString(
                    2,
                    studentId
            );

            int updated =
                    statement.executeUpdate();

            if (updated > 0) {

                phoneField.setEditable(
                        false
                );

                showAlert(
                        "Contact number updated successfully."
                );

            } else {

                showAlert(
                        "Contact number update failed."
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Database error while updating contact number."
            );
        }
    }

    private void updateNicNumber() {
        String nic = nicField.getText().trim().toUpperCase();
        LocalDate birthDate = nic.isEmpty() ? null : birthDateFromNic(nic);

        if (!nic.isEmpty() && birthDate == null) {
            showAlert("Please enter a valid Sri Lankan NIC number.");
            return;
        }

        String sql = """
                UPDATE undergraduates
                SET nic = ?
                WHERE reg_no = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (nic.isEmpty()) {
                statement.setNull(1, java.sql.Types.VARCHAR);
            } else {
                statement.setString(1, nic);
            }
            statement.setString(2, studentId);

            if (statement.executeUpdate() > 0) {
                nicField.setText(nic);
                nicField.setEditable(false);
                editNicButton.setText("Edit NIC Number");
                updateBirthdayDisplay(nic);
                showAlert("NIC number updated successfully.");
            } else {
                showAlert("NIC number update failed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error while updating NIC number.");
        }
    }

    private void updateBirthdayDisplay(String nic) {
        LocalDate birthDate = nic == null || nic.isBlank() ? null : birthDateFromNic(nic);
        fields.get(10).setText(birthDate == null ? "" : birthDate.toString());

        if (birthDate == null) {
            fields.get(11).clear();
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate nextBirthday = birthdayInYear(birthDate, today.getYear());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = birthdayInYear(birthDate, today.getYear() + 1);
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, nextBirthday);
        fields.get(11).setText(daysRemaining + (daysRemaining == 1 ? " day" : " days"));
    }

    private LocalDate birthdayInYear(LocalDate birthDate, int year) {
        if (birthDate.getMonthValue() == 2 && birthDate.getDayOfMonth() == 29
                && !java.time.Year.isLeap(year)) {
            return LocalDate.of(year, 3, 1);
        }
        return birthDate.withYear(year);
    }

    private LocalDate birthDateFromNic(String value) {
        String nic = value.trim().toUpperCase();
        try {
            int year;
            int dayOfYear;
            if (nic.matches("\\d{12}")) {
                year = Integer.parseInt(nic.substring(0, 4));
                dayOfYear = Integer.parseInt(nic.substring(4, 7));
            } else if (nic.matches("\\d{9}[VX]")) {
                int twoDigitYear = Integer.parseInt(nic.substring(0, 2));
                dayOfYear = Integer.parseInt(nic.substring(2, 5));
                int currentTwoDigitYear = LocalDate.now().getYear() % 100;
                year = twoDigitYear <= currentTwoDigitYear
                        ? 2000 + twoDigitYear
                        : 1900 + twoDigitYear;
            } else {
                return null;
            }

            if (dayOfYear > 500) {
                dayOfYear -= 500;
            }
            if (dayOfYear < 1 || dayOfYear > (java.time.Year.isLeap(year) ? 366 : 365)) {
                return null;
            }

            LocalDate birthDate = LocalDate.ofYearDay(year, dayOfYear);
            if (birthDate.isAfter(LocalDate.now()) && nic.matches("\\d{9}[VX]")) {
                birthDate = LocalDate.ofYearDay(year - 100, dayOfYear);
            }
            return birthDate.isAfter(LocalDate.now()) ? null : birthDate;
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    private void ensureNicColumn() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(
                     connection.getCatalog(), null, "undergraduates", "nic")) {
            if (columns.next()) {
                return;
            }
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "ALTER TABLE undergraduates ADD COLUMN nic VARCHAR(12) NULL")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    // =========================================================
    // ALERT
    // =========================================================

    private void showAlert(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                "TECLMS"
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}
