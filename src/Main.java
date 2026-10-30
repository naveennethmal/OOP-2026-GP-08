import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Student.Dashboard;
import Technical_Officer.MainLayout;


public class Main {

    private static Scene scene;

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }


    public static void showLogin() {
        scene.setRoot(new Login(
                Main::showDashboard,
                Main::showAdmin,
                Main::showTechnicalOfficer,
                Main::showLecturer));
    }


    public static void showDashboard() {
        scene.setRoot(new Dashboard(Main::showLogin));
    }

    public static void showAdmin() {
        scene.setRoot(new Admin.App(Main::showLogin));
    }

    public static void showTechnicalOfficer() {
        scene.setRoot(new MainLayout(Main::showLogin));
    }

    public static void showLecturer() {
        scene.setRoot(new Lecturer.LecturerLayout(Main::showLogin));
    }

    public static class App extends Application {

        @Override
        public void start(Stage stage) {
            scene = new Scene(new Login(
                    Main::showDashboard,
                    Main::showAdmin,
                    Main::showTechnicalOfficer,
                    Main::showLecturer), 1400, 800);
            scene.getStylesheets().add(
                    getClass().getResource("/Resources/style.css").toExternalForm());

            stage.setTitle("TECLMS - Student Portal");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.show();
        }
    }
}
