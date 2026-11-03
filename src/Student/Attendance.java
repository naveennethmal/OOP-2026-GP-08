package Student;
import Common.Session;
import Database.DBConnection;

import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class Attendance extends VBox {

    public Attendance() {

        super(18);
        getStyleClass().add("content");

        String regNo = Session.getRegNo();

        // Check logged-in student
        if (regNo == null || regNo.isBlank()) {

            getChildren().add(
                    Ui.header(
                            "Attendance",
                            "Please login to view your attendance"
                    )
            );

            return;
        }

        // SQL query to load attendance from database.....
        String sql = """
                SELECT
                    c.course_name,
                    SUM(
                        CASE
                            WHEN a.status IN ('Present', 'Late', 'Medical')
                            THEN 1 ELSE 0
                        END
                    ) AS present_count,
                    SUM(
                        CASE
                            WHEN a.status = 'Absent'
                            THEN 1 ELSE 0
                        END
                    ) AS absent_count
                FROM attendance a
                JOIN (
                    SELECT DISTINCT reg_no, course_code
                    FROM enrollments
                ) e
                    ON e.reg_no = a.reg_no
                    AND e.course_code = a.course_code
                JOIN courses c ON a.course_code = c.course_code
                WHERE a.reg_no = ?
                GROUP BY c.course_code, c.course_name
                ORDER BY c.course_name
                """;

        List<Object[]> data = new ArrayList<>();

        int totalPresent = 0;
        int totalAbsent = 0;

        // Load data from database
        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, regNo);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    String courseName =
                            rs.getString("course_name");

                    int present =
                            rs.getInt("present_count");

                    int absent =
                            rs.getInt("absent_count");

                    int total = present + absent;

                    int percentage = total == 0
                            ? 0
                            : present * 100 / total;

                    totalPresent += present;
                    totalAbsent += absent;

                    String color = percentage >= 90
                            ? "green"
                            : percentage >= 80
                            ? "yellow"
                            : "red";

                    data.add(new Object[]{
                            courseName,
                            present,
                            absent,
                            total,
                            Ui.tag(
                                    percentage + "%",
                                    color
                            )
                    });
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            Alert alert = new Alert(
                    Alert.AlertType.ERROR
            );

            alert.setTitle("Database Error");
            alert.setHeaderText(
                    "Could not load attendance details"
            );

            alert.setContentText(
                    "Please check your database connection.\n"
                            + e.getMessage()
            );

            alert.showAndWait();

            getChildren().setAll(
                    Ui.header(
                            "Attendance",
                            "Unable to load attendance details"
                    )
            );

            return;
        }

        // Calculate overall attendance
        int totalClasses = totalPresent + totalAbsent;

        int overallPercentage = totalClasses == 0
                ? 0
                : totalPresent * 100 / totalClasses;

        // Convert list into table rows
        Object[][] rows = data.toArray(
                new Object[0][]
        );

        // Display attendance page
        getChildren().addAll(

                Ui.header(
                        "Attendance",
                        "Your attendance summary for this semester"
                ),

                Ui.stats(new String[][]{

                        {
                                "Total Classes",
                                String.valueOf(totalClasses),
                                "blue"
                        },

                        {
                                "Present",
                                String.valueOf(totalPresent),
                                "green"
                        },

                        {
                                "Absent",
                                String.valueOf(totalAbsent),
                                "red"
                        },

                        {
                                "Attendance",
                                overallPercentage + "%",
                                "purple"
                        }
                }),

                Ui.card(
                        "Course-wise Attendance",

                        Ui.table(
                                new String[]{
                                        "Course",
                                        "Present",
                                        "Absent",
                                        "Total",
                                        "Percentage"
                                },

                                rows
                        )
                )
        );
    }
}
