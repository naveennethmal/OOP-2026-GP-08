import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Main does NOT extend Application, so no separate Launcher class is needed.
public class Main {

    private static Scene scene;

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    // Login screen ekata yanna
    public static void showLogin() {
        scene.setRoot(new Login(Main::showDashboard));
    }

    // Dashboard ekata yanna
    public static void showDashboard() {
        scene.setRoot(new Dashboard());
    }

    public static class App extends Application {

        @Override
        public void start(Stage stage) {
            scene = new Scene(new Login(Main::showDashboard), 1400, 800);
            scene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm());

            stage.setTitle("TECLMS - Student Portal");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.show();
        }
    }
}