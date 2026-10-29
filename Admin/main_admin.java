package Admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class main_admin extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        App adminApp = new App(() -> {
            System.out.println("Logging out from Admin Portal...");
            System.exit(0);
        });

        Scene scene = new Scene(adminApp, 1200, 750);

        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS file not found, loading layout without styles.");
        }

        primaryStage.setTitle("TECLMS - Admin Portal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
