package Admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class main_admin extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // එකම ෆෝල්ඩර් එකේ නිසා කෙලින්ම App කියලා විතරක් ගන්නවා
        Admin adminApp = new App(() -> {
            System.out.println("Logging out from Admin Portal...");
            System.exit(0);
        });

        // JavaFX Scene එක සකස් කිරීම
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

    // ප්‍රධාන main method එක
    public static void main(String[] args) {
        RealLauncher.main(args);
    }
}

// JavaFX runtime error එක bypass කරන උපක්‍රමය
class RealLauncher {
    public static void main(String[] args) {
        Application.launch(main_admin.class, args);
    }
}
