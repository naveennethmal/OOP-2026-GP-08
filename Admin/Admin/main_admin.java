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


        VBox side = new VBox();
        side.setStyle("-fx-background-color: #1e293b; -fx-padding: 24px; -fx-spacing: 16px; -fx-pref-width: 280px;");

        Label logoIcon = new Label("\uD83C\uDF93");
        logoIcon.setStyle("-fx-font-size: 32px;");
        Label brand = new Label("TECLMS");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label sub = new Label("Admin Portal (UI Mode)");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        HBox branding = new HBox(14, logoIcon, new VBox(brand, sub));
        branding.setAlignment(Pos.CENTER_LEFT);
        side.getChildren().add(branding);

        Label menuCaption = new Label("MENU");
        menuCaption.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 10px 0 0 0;");
        side.getChildren().add(menuCaption);

        ToggleGroup g = new ToggleGroup();
        String[] tabs = {"👤  Users", "📘  Courses", "🔔  Notices", "🔑  Password requests", "📅  Timetables"};




