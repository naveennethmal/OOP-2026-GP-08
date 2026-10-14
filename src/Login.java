import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login extends HBox {

    public Login(Runnable onSuccess) {

        getStyleClass().add("login-root");

        // ---------------- Left branding panel ----------------
        Label logoIcon = new Label("🎓");
        logoIcon.getStyleClass().add("logo-icon");

        HBox logo = new HBox(
                14,
                logoIcon,
                new VBox(
                        Ui.text("TECLMS", "logo-title"),
                        Ui.text("Student Portal", "logo-sub")
                )
        );

        logo.setAlignment(Pos.CENTER_LEFT);

        Label tagline = Ui.text(
                "Learn. Grow.\nSucceed.",
                "login-tagline"
        );

        Label tagSub = Ui.text(
                "Access your courses, attendance, grades and\nnotices all in one place.",
                "login-tag-sub"
        );

        VBox left = new VBox(
                28,
                logo,
                tagline,
                tagSub
        );

        left.getStyleClass().add("login-left");
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPrefWidth(540);
        left.setMinWidth(420);

        // ---------------- Right form card ----------------

        TextField id = new TextField();
        id.setPromptText("e.g. IT2024/001");
        id.getStyleClass().add("field");

        PasswordField pw = new PasswordField();
        pw.setPromptText("Enter your password");
        pw.getStyleClass().add("field");

        CheckBox remember = new CheckBox("Remember me");
        remember.getStyleClass().add("login-check");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label forgot = Ui.text(
                "Forgot password?",
                "link"
        );

        forgot.setOnMouseClicked(e -> {
            // TODO: forgot password page
        });

        HBox options = new HBox(
                remember,
                sp,
                forgot
        );

        options.setAlignment(Pos.CENTER_LEFT);

        // Error message
        Label error = Ui.text(
                "",
                "login-error"
        );

        error.setVisible(false);
        error.setManaged(false);

        // Login button
        Button login = new Button("Sign In");

        login.getStyleClass().add("primary-btn");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);

        // ---------------- LOGIN ATTEMPT ----------------

        Runnable attempt = () -> {

            String studentId = id.getText().trim();
            String password = pw.getText();

            // Empty fields
            if (studentId.isEmpty() || password.isEmpty()) {

                showError(
                        error,
                        "Please enter your Student ID and password."
                );

                return;
            }

            // Database authentication
            String sql = """
                    SELECT
                        u.user_id,
                        u.full_name,
                        u.role,
                        ug.reg_no,
                        ug.status
                    FROM users u
                    JOIN undergraduates ug
                        ON u.user_id = ug.user_id
                    WHERE ug.reg_no = ?
                      AND u.password = ?
                      AND u.role = 'Undergraduate'
                    """;

            try (
                    Connection connection =
                            DBConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setString(1, studentId);
                statement.setString(2, password);

                ResultSet result = statement.executeQuery();

                if (result.next()) {

                    // Login successful
                    int userId =
                            result.getInt("user_id");

                    String fullName =
                            result.getString("full_name");

                    String regNo =
                            result.getString("reg_no");

                    String status =
                            result.getString("status");

                    // Check student status
                    if (!status.equalsIgnoreCase("Active")) {

                        showError(
                                error,
                                "Your student account is not active."
                        );

                        return;
                    }

                    System.out.println(
                            "LOGIN SUCCESSFUL"
                    );

                    System.out.println(
                            "User ID: " + userId
                    );

                    System.out.println(
                            "Student ID: " + regNo
                    );

                    System.out.println(
                            "Student Name: " + fullName
                    );

                    // Save logged-in student
                    Session.setUser(userId,regNo,fullName);

                    // Go to Dashboard
                    onSuccess.run();

                } else {

                    showError(
                            error,
                            "Invalid Student ID or password."
                    );

                    pw.clear();
                }

            } catch (SQLException e) {

                e.printStackTrace();

                showError(
                        error,
                        "Database connection error. Please try again."
                );
            }
        };

        login.setOnAction(e -> attempt.run());

        id.setOnAction(e -> pw.requestFocus());

        pw.setOnAction(e -> attempt.run());

        // ---------------- Form ----------------

        VBox form = new VBox(
                18,

                Ui.text(
                        "Welcome back 👋",
                        "page-title"
                ),

                Ui.text(
                        "Sign in to continue to your student portal",
                        "page-sub"
                ),

                new VBox(
                        6,
                        Ui.text(
                                "Student ID",
                                "field-label"
                        ),
                        id
                ),

                new VBox(
                        6,
                        Ui.text(
                                "Password",
                                "field-label"
                        ),
                        pw
                ),

                options,
                error,
                login,

                Ui.text(
                        "Need help? Contact the IT support desk.",
                        "muted-small"
                )
        );

        form.getStyleClass().add("login-card");
        form.setMaxWidth(420);

        StackPane right = new StackPane(form);

        right.getStyleClass().add("login-right");

        HBox.setHgrow(
                right,
                Priority.ALWAYS
        );

        getChildren().addAll(
                left,
                right
        );
    }

    private void showError(
            Label label,
            String message
    ) {

        label.setText(
                "⚠  " + message
        );

        label.setVisible(true);
        label.setManaged(true);
    }
}

