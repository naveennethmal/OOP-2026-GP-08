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

        BorderPane adminLayout = new BorderPane();


        VBox side = new VBox();side.setStyle("-fx-background-color: #1e293b; -fx-padding: 24px; -fx-spacing: 16px; -fx-pref-width: 280px;");
