package Student;

import Common.Session;
import Database.DBConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Types;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MedicalDetails extends VBox {

    private final String regNo = Session.getRegNo();

    private final Label bannerTitle = new Label();
    private final Label bannerSubtitle = new Label();

    private final Label totalLabel = new Label("0");
    private final Label pendingLabel = new Label("0");
    private final Label approvedLabel = new Label("0");

    private final VBox historyCard = new VBox(12);

    public MedicalDetails() {
        super(18);
        getStyleClass().add("content");

        if (regNo == null || regNo.isBlank()) {
            getChildren().add(
                    Ui.header(
                            "Medical Details",
                            "Please login to view your medical details"
                    )
            );
            return;
        }

        getChildren().addAll(
                Ui.header(
                        "Medical Details",
                        "Your health information and medical submissions"
                ),
                createBanner(),
                createSummary(),
                historyCard
        );

        ensureMedicalColumns();
        loadMedicalDetails();
    }

    private void ensureMedicalColumns() {
        try (Connection connection = DBConnection.getConnection();
             java.sql.Statement statement = connection.createStatement()) {
            addColumnIfMissing(statement,
                    "ALTER TABLE medical ADD COLUMN submission_group_id VARCHAR(36) NULL");
            addColumnIfMissing(statement,
                    "ALTER TABLE medical ADD COLUMN medical_start_date DATE NULL");
            addColumnIfMissing(statement,
                    "ALTER TABLE medical ADD COLUMN medical_end_date DATE NULL");
            statement.executeUpdate("ALTER TABLE medical MODIFY COLUMN tech_id INT NULL");
        } catch (SQLException ex) {
            historyCard.getChildren().add(
                    new Label("Database error: Cannot prepare medical submissions. " + ex.getMessage()));
        }
    }

    private static void addColumnIfMissing(java.sql.Statement statement, String sql)
            throws SQLException {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 1060) throw ex;
        }
    }

    // Medical status banner
    private HBox createBanner() {
        bannerTitle.getStyleClass().add("banner-title");
        bannerSubtitle.getStyleClass().add("banner-sub");

        HBox banner = new HBox(
                16,
                Ui.text("✚", "banner-icon"),
                new VBox(4, bannerTitle, bannerSubtitle)
        );

        banner.getStyleClass().add("banner-ok");
        return banner;
    }

    // Summary cards
    private VBox createSummary() {
        return Ui.card(
                "Medical Summary",
                new HBox(
                        30,
                        summaryItem("Total Submissions", totalLabel),
                        summaryItem("Pending", pendingLabel),
                        summaryItem("Approved", approvedLabel)
                )
        );
    }

    private VBox summaryItem(String title, Label value) {
        Label heading = new Label(title);
        heading.getStyleClass().add("field-label");
        value.getStyleClass().add("table-cell");

        return new VBox(8, heading, value);
    }

    // Load medical submissions, grouping selected subjects together
    private void loadMedicalDetails() {
        historyCard.getChildren().clear();

        String sql = """
                SELECT
                    COALESCE(
                        m.submission_group_id,
                        CONCAT('old-', m.medical_id)
                    ) AS submission_key,
                    MIN(COALESCE(m.medical_start_date, m.medical_date)) AS medical_start_date,
                    MAX(COALESCE(m.medical_end_date, m.medical_date)) AS medical_end_date,
                    GROUP_CONCAT(
                        DISTINCT CONCAT(
                            COALESCE(c.course_name, m.course_code),
                            ' (',
                            m.course_code,
                            ')'
                        )
                        ORDER BY c.course_name
                        SEPARATOR ', '
                    ) AS course_names,
                    m.reason,
                    m.document,
                    m.approval_status
                FROM medical m
                LEFT JOIN courses c
                    ON m.course_code = c.course_code
                WHERE m.reg_no = ?
                GROUP BY
                    COALESCE(
                        m.submission_group_id,
                        CONCAT('old-', m.medical_id)
                    ),
                    m.reason,
                    m.document,
                    m.approval_status
                ORDER BY MIN(COALESCE(m.medical_start_date, m.medical_date)) DESC
                """;

        List<Object[]> rows = new ArrayList<>();

        int total = 0;
        int pending = 0;
        int approved = 0;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, regNo);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    total++;

                    String status = rs.getString("approval_status");

                    if (status == null || status.isBlank()) {
                        status = "Pending";
                    }

                    if (status.equalsIgnoreCase("Pending")) {
                        pending++;
                    } else if (status.equalsIgnoreCase("Approved")) {
                        approved++;
                    }

                    Date startDate = rs.getDate("medical_start_date");
                    Date endDate = rs.getDate("medical_end_date");
                    String dateText = startDate == null
                            ? "-"
                            : startDate.equals(endDate)
                            ? startDate.toString()
                            : startDate + " to " + endDate;

                    String courseNames = rs.getString("course_names");
                    String reason = rs.getString("reason");
                    String submissionKey = rs.getString("submission_key");
                    Button delete = new Button("Delete");
                    delete.getStyleClass().add("danger-btn");
                    delete.setOnAction(event -> deleteSubmission(submissionKey));

                    rows.add(new Object[]{
                            dateText,
                            courseNames == null ? "-" : courseNames,
                            reason == null ? "-" : reason,
                            Ui.tag(status, getStatusColor(status)),
                            status.equalsIgnoreCase("Pending") ? delete : new Label("-")
                    });
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

            bannerTitle.setText("Database Error");
            bannerSubtitle.setText(
                    "Unable to load medical submissions."
            );

            historyCard.getChildren().add(
                    new Label("Database error: " + e.getMessage())
            );
            return;
        }

        totalLabel.setText(String.valueOf(total));
        pendingLabel.setText(String.valueOf(pending));
        approvedLabel.setText(String.valueOf(approved));

        if (pending > 0) {
            bannerTitle.setText("Medical Submission Pending");
            bannerSubtitle.setText(
                    "You have " + pending
                            + " submission(s) awaiting review."
            );
        } else {
            bannerTitle.setText("Medical Records");
            bannerSubtitle.setText(
                    "Your medical submissions are up to date."
            );
        }

        Button submitButton = new Button("＋ Submit New Medical");
        submitButton.getStyleClass().add("primary-btn");
        submitButton.setOnAction(event -> openSubmitForm());

        historyCard.getChildren().addAll(
                new Label("Submitted Medicals"),
                Ui.table(
                        new String[]{
                                "Date",
                                "Course / Subjects",
                                "Reason",
                                "Status",
                                "Actions"
                        },
                        rows.toArray(new Object[0][])
                ),
                submitButton
        );
    }

    private void deleteSubmission(String submissionKey) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this pending medical submission?",
                ButtonType.CANCEL,
                ButtonType.OK);
        confirmation.setHeaderText("Delete medical submission");
        confirmation.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            String groupId = submissionKey.startsWith("old-")
                    ? null : submissionKey;
            Integer medicalId = null;
            if (groupId == null) {
                try {
                    medicalId = Integer.valueOf(submissionKey.substring(4));
                } catch (NumberFormatException ex) {
                    showError("Invalid medical submission.");
                    return;
                }
            }
            String sql = """
                    DELETE FROM medical
                    WHERE reg_no = ?
                      AND approval_status = 'Pending'
                      AND (
                          submission_group_id = ?
                          OR (submission_group_id IS NULL AND medical_id = ?)
                      )
                    """;
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, regNo);
                if (groupId == null) statement.setNull(2, Types.VARCHAR);
                else statement.setString(2, groupId);
                if (medicalId == null) statement.setNull(3, Types.INTEGER);
                else statement.setInt(3, medicalId);
                statement.executeUpdate();
                loadMedicalDetails();
            } catch (SQLException ex) {
                showError("Unable to delete the medical submission.\n\n" + ex.getMessage());
            }
        });
    }

    private String getStatusColor(String status) {
        if (status == null) {
            return "yellow";
        }

        return switch (status.toLowerCase()) {
            case "approved" -> "green";
            case "rejected" -> "red";
            default -> "yellow";
        };
    }

    // Open medical form
    private void openSubmitForm() {
        ListView<CheckBox> courseList = new ListView<>();
        courseList.setPrefHeight(150);
        courseList.setMinHeight(100);
        courseList.setMaxWidth(Double.MAX_VALUE);

        loadCoursesForMultipleSelection(courseList);

        if (courseList.getItems().isEmpty()) {
            showWarning(
                    "No enrolled subjects were found for your account."
            );
            return;
        }

        DatePicker startDate =
                new DatePicker(LocalDate.now());
        startDate.setMaxWidth(Double.MAX_VALUE);
        DatePicker endDate =
                new DatePicker(LocalDate.now());
        endDate.setMaxWidth(Double.MAX_VALUE);

        TextArea reasonField = new TextArea();
        reasonField.setPromptText(
                "Enter your medical reason..."
        );
        reasonField.setPrefRowCount(3);
        reasonField.setWrapText(true);

        TextField documentField = new TextField();
        documentField.setPromptText(
                "Optional medical certificate"
        );
        documentField.setEditable(false);

        File[] selectedFile = {null};

        Button chooseFileButton =
                new Button("Choose Medical Certificate");

        chooseFileButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Medical Certificate");

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PDF and Images",
                            "*.pdf", "*.png", "*.jpg", "*.jpeg"
                    )
            );

            File file = chooser.showOpenDialog(
                    getScene() == null
                            ? null
                            : getScene().getWindow()
            );

            if (file != null) {
                selectedFile[0] = file;
                documentField.setText(file.getAbsolutePath());
            }
        });

        Label instruction = new Label(
                "Tick all subjects related to this medical submission:"
        );
        instruction.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(12));

        ColumnConstraints labelColumn =
                new ColumnConstraints();
        labelColumn.setMinWidth(130);

        ColumnConstraints inputColumn =
                new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().addAll(
                labelColumn,
                inputColumn
        );

        form.add(new Label("Course / Subjects:"), 0, 0);
        form.add(
                new VBox(6, instruction, courseList),
                1,
                0
        );

        form.add(new Label("Start Date:"), 0, 1);
        form.add(startDate, 1, 1);

        form.add(new Label("End Date:"), 0, 2);
        form.add(endDate, 1, 2);

        form.add(new Label("Reason:"), 0, 3);
        form.add(reasonField, 1, 3);

        form.add(new Label("Certificate:"), 0, 4);
        form.add(
                new VBox(8, documentField, chooseFileButton),
                1,
                4
        );

        ButtonType submitType = new ButtonType(
                "Submit",
                ButtonBar.ButtonData.OK_DONE
        );

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Submit New Medical");
        dialog.setHeaderText(
                "Select one or more subjects for your medical"
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.CANCEL,
                submitType
        );

        dialog.getDialogPane().setContent(form);

        Button submitButton = (Button) dialog.getDialogPane()
                .lookupButton(submitType);

        submitButton.addEventFilter(
                javafx.event.ActionEvent.ACTION,
                event -> {
                    boolean selected = false;

                    for (CheckBox checkBox : courseList.getItems()) {
                        if (checkBox.isSelected()) {
                            selected = true;
                            break;
                        }
                    }

                    if (!selected) {
                        showWarning(
                                "Please select at least one subject."
                        );
                        event.consume();

                    } else if (startDate.getValue() == null
                            || endDate.getValue() == null) {
                        showWarning(
                                "Please select the start and end dates."
                        );
                        event.consume();

                    } else if (startDate.getValue().isAfter(endDate.getValue())) {
                        showWarning("End date cannot be before the start date.");
                        event.consume();
                    } else if (endDate.getValue().isAfter(LocalDate.now())) {
                        showWarning(
                                "Medical end date cannot be in the future."
                        );
                        event.consume();

                    } else if (reasonField.getText().isBlank()) {
                        showWarning(
                                "Please enter the medical reason."
                        );
                        event.consume();
                    }
                }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitType) {
                List<CourseOption> selectedCourses =
                        getSelectedCourses(courseList);

                saveMedical(
                        selectedCourses,
                        startDate.getValue(),
                        endDate.getValue(),
                        reasonField.getText().trim(),
                        selectedFile[0]
                );
            }
        });
    }

    // Load subjects matching the student's department, level and semester
    private void loadCoursesForMultipleSelection(
            ListView<CheckBox> courseList
    ) {
        String sql = """
                SELECT DISTINCT c.course_code, c.course_name
                FROM undergraduates ug
                JOIN courses c
                  ON c.department_id = ug.department_id
                 AND c.level = ug.level
                 AND c.semester = ug.semester
                WHERE ug.reg_no = ?
                ORDER BY c.course_name
                """;

        ObservableList<CheckBox> subjects =
                FXCollections.observableArrayList();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, regNo);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("course_code");
                    String name = rs.getString("course_name");

                    CheckBox checkBox = new CheckBox(
                            name + " (" + code + ")"
                    );

                    checkBox.setUserData(
                            new CourseOption(code, name)
                    );

                    subjects.add(checkBox);
                }
            }

            courseList.setItems(subjects);

        } catch (SQLException e) {
            e.printStackTrace();

            showError(
                    "Unable to load your subjects.\n"
                            + e.getMessage()
            );
        }
    }

    // Collect selected subjects
    private List<CourseOption> getSelectedCourses(
            ListView<CheckBox> courseList
    ) {
        List<CourseOption> selectedCourses =
                new ArrayList<>();

        for (CheckBox checkBox : courseList.getItems()) {
            if (checkBox.isSelected()) {
                selectedCourses.add(
                        (CourseOption) checkBox.getUserData()
                );
            }
        }

        return selectedCourses;
    }

    // Save all selected subjects under one submission group
    private void saveMedical(
            List<CourseOption> selectedCourses,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            File document
    ) {
        if (selectedCourses.isEmpty()) {
            showWarning("Please select at least one subject.");
            return;
        }

        String submissionGroupId = UUID.randomUUID().toString();

        String sql = """
                INSERT INTO medical
                    (submission_group_id, reg_no, tech_id,
                     course_code, medical_date, medical_start_date,
                     medical_end_date, reason,
                     document, approval_status)
                VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, 'Pending')
                """;

        String documentPath = document == null
                ? null
                : document.getAbsolutePath();

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                for (CourseOption course : selectedCourses) {
                    statement.setString(1, submissionGroupId);
                    statement.setString(2, regNo);
                    statement.setString(3, course.getCode());
                    statement.setDate(4, Date.valueOf(startDate));
                    statement.setDate(5, Date.valueOf(startDate));
                    statement.setDate(6, Date.valueOf(endDate));
                    statement.setString(7, reason);

                    if (documentPath == null) {
                        statement.setNull(8, Types.VARCHAR);
                    } else {
                        statement.setString(8, documentPath);
                    }

                    statement.addBatch();
                }

                statement.executeBatch();
                connection.commit();

                showInformation(
                        "Medical submitted successfully for "
                                + selectedCourses.size()
                                + " subjects."
                );

                loadMedicalDetails();

            } catch (SQLException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();

            showError(
                    "Unable to save medical submission.\n\n"
                            + e.getMessage()
            );
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Medical Details");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Medical Details");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Medical Details - Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Course data
    private static class CourseOption {
        private final String code;
        private final String name;

        CourseOption(String code, String name) {
            this.code = code;
            this.name = name;
        }

        String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return name + " (" + code + ")";
        }
    }
}