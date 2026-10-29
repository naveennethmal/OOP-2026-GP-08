package Admin;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class main_admin extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Database එකට කනෙක්ට් නොවී කෙලින්ම වැඩ කරන සරල Layout එකක් මෙතනින් හදනවා
        BorderPane mockAdminApp = new BorderPane();

        VBox side = new VBox();
        side.setStyle("-fx-background-color: #1e293b; -fx-padding: 20px; -fx-spacing: 15px; -fx-pref-width: 260px;");

        Label brand = new Label("TECLMS");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label sub = new Label("Admin Portal (Offline Mode)");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        side.getChildren().addAll(new VBox(5, brand, sub));

        ToggleGroup g = new ToggleGroup();
        String[][] menuItems = {
                {"👤  Users"}, {"📘  Courses"}, {"🔔  Notices"},
                {"🔑  Password requests"}, {"📅  Timetables"}
        };

        for (String[] item : menuItems) {
            ToggleButton b = new ToggleButton(item[0]);
            b.setToggleGroup(g);
            b.setStyle("-fx-text-fill: #334155; -fx-font-size: 15px; -fx-pref-width: 220px; -fx-alignment: center-left; -fx-padding: 10px;");
            side.getChildren().add(b);
        }

        mockAdminApp.setLeft(side);

        // මැද ප්‍රධාන පිටුවට සාදරයෙන් පිළිගන්නා පණිවිඩය
        VBox centerView = new VBox(10);
        centerView.setAlignment(Pos.CENTER);
        Label mainMsg = new Label("Welcome to TECLMS Admin Portal");
        mainMsg.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subMsg = new Label("Database එකක් නොමැතිව සාර්ථකව Run වී ඇත.");
        subMsg.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
        centerView.getChildren().addAll(mainMsg, subMsg);
        mockAdminApp.setCenter(centerView);

        Scene scene = new Scene(mockAdminApp, 1200, 750);

        primaryStage.setTitle("TECLMS - Admin Portal (Database Bypass)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        RealLauncher.main(args);
    }
}

class RealLauncher {
    public static void main(String[] args) {
        Application.launch(main_admin.class, args);
    }
}
