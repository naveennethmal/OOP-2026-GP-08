import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LecturerDashboard extends BorderPane {

    private final Lecturer lecturer;
    private final Stage stage;
    private final StackPane content = new StackPane();

    private static final String MENU_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 14;";

    public LecturerDashboard(Lecturer lecturer, Stage stage) {
        this.lecturer = lecturer;
        this.stage = stage;
        setLeft(buildSidebar());
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_LEFT);
        setCenter(content);
        navigate("Dashboard");

    }

    private VBox buildSidebar() {
        Label brand = new Label("TECLMS");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("Lecturer Portal");
        sub.setStyle("-fx-text-fill: #9db4e0;");

        VBox box = new VBox(6, brand, sub);
        box.setPadding(new Insets(20));
        box.setPrefWidth(240);
        box.setStyle("-fx-background-color: #0b1f5c;");

        Region gap = new Region();
        gap.setPrefHeight(20);
        box.getChildren().add(gap);

        String[] items = {
                "Dashboard", "My Profile", "Course Materials", "Upload Marks",
                "CA & Eligibility", "Attendance & Medical", "Grades & GPA",
                "Student Details", "Notices"
        };
        for (String name : items) {
            Button b = new Button(name);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setStyle(MENU_STYLE);
            b.setOnAction(e -> navigate(name));
            box.getChildren().add(b);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = new Button("Logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setStyle(MENU_STYLE);
        logout.setOnAction(e -> {
            stage.close();
            new LecturerLogin().start(new Stage());
        });

        box.getChildren().addAll(spacer, logout);
        return box;
    }

    private void navigate(String page) {
        Node view = switch (page) {
            case "Dashboard" -> homePage();
            case "My Profile" -> new LecturerProfilePage(lecturer);
            case "Notices" -> new LecturerNoticesPage();
            case "Student Details" -> new LecturerStudentsPage();
            case "Upload Marks" -> new LecturerMarksPage(lecturer);
            case "CA & Eligibility" -> new LecturerCAPage(lecturer);
            case "Attendance & Medical" -> new LecturerAttendancePage(lecturer);
            case "Grades & GPA" -> new LecturerGradesPage(lecturer);
            default -> placeholder(page);
        };
        content.getChildren().setAll(view);
    }

    private VBox homePage() {
        Label welcome = new Label("Welcome back, " + lecturer.getFullName() + "!");
        welcome.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label info = new Label("Employee No: " + lecturer.getEmployeeNo()
                + "\nEmail: " + lecturer.getEmail());
        return new VBox(12, welcome, info);
    }

    private VBox placeholder(String title) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label soon = new Label("Coming soon...");
        return new VBox(10, t, soon);
    }
}