import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

public class CourseDetails extends VBox {
    public CourseDetails() {
        super(18);
        getStyleClass().add("content");

        // code, name, lecturer, credits, progress, color
        String[][] c = {
                {"ICT1212", "Java Programming", "Dr. A. Silva", "3", "0.75", "#2f80ff"},
                {"ICT1222", "Database Systems", "Ms. N. Fernando", "3", "0.60", "#22c07a"},
                {"ICT1232", "Software Engineering", "Mr. K. Jayawardena", "3", "0.45", "#7c5cff"},
                {"ICT1242", "Web Technologies", "Ms. S. Perera", "3", "0.30", "#f5a623"},
                {"ICT1252", "Computer Networks", "Dr. R. Wijesinghe", "3", "0.20", "#19c2d8"},
                {"ICT1261", "Data Structures & Algorithms", "Mr. T. Gunasekara", "3", "0.10", "#ef4b8a"}};

        Object[][] rows = new Object[c.length][];
        for (int i = 0; i < c.length; i++) {
            double p = Double.parseDouble(c[i][4]);
            ProgressBar bar = new ProgressBar(p);
            bar.setPrefWidth(130);
            bar.setStyle("-fx-accent: " + c[i][5] + ";");
            bar.getStyleClass().add("course-bar");
            HBox prog = new HBox(10, bar, Ui.text((int) (p * 100) + "%", "muted-small"));
            prog.setAlignment(Pos.CENTER_LEFT);
            rows[i] = new Object[]{c[i][0], c[i][1], c[i][2], c[i][3], prog, Ui.tag("Ongoing", "blue")};
        }

        getChildren().addAll(
                Ui.header("Course Details", "All courses you are enrolled in this semester"),
                Ui.card("Enrolled Courses", Ui.table(
                        new String[]{"Code", "Course", "Lecturer", "Credits", "Progress", "Status"}, rows)));
    }
}