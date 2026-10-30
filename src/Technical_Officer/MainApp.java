package Technical_Officer;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.lang.reflect.Constructor;

import Admin.App;
import Student.Dashboard;

public class MainApp extends Application {
    private static Scene scene;

    private static void showLogin() {
        try {
            Class<?> loginClass = Class.forName("Login");
            Constructor<?> constructor = loginClass.getConstructor(
                    Runnable.class, Runnable.class, Runnable.class);
            Object login = constructor.newInstance(
                    (Runnable) () -> scene.setRoot(new Dashboard(MainApp::showLogin)),
                    (Runnable) () -> scene.setRoot(new App(MainApp::showLogin)),
                    (Runnable) () -> scene.setRoot(new MainLayout(MainApp::showLogin)));
            scene.setRoot((Parent) login);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not open the login screen.", e);
        }
    }

    @Override
    public void start(Stage stage) {
        // Taskbar eka ain kala screen eke visible area eka
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Screen ekata fit wenawa, max 1280x780
        double width = Math.min(1280, screen.getWidth() * 0.92);
        double height = Math.min(780, screen.getHeight() * 0.90);

        scene = new Scene(new StackPane(), width, height);
        scene.getStylesheets().add(
                MainApp.class.getResource("/Resources/style.css").toExternalForm());
        showLogin();

        stage.setTitle("TECLMS - Technical Officer");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setResizable(true);
        stage.setScene(scene);

        // Screen eke mediyata
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
