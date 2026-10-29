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
        // ඩේටාබේස් එකට සම්බන්ධ නොවී ඔයාගේ ප්‍රොජෙක්ට් එකේ නියම ඇඩ්මින් UI එක (Layout) මෙතනින් සාදයි.
        BorderPane adminLayout = new BorderPane();

