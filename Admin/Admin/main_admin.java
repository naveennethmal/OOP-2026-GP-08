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

        for (String tabName : tabs) {
            ToggleButton b = new ToggleButton(tabName);
            b.setToggleGroup(g);
            b.setStyle("-fx-text-fill: #e2e8f0; -fx-background-color: transparent; -fx-font-size: 14px; -fx-pref-width: 240px; -fx-alignment: center-left; -fx-padding: 10px 12px; -fx-cursor: hand;");
            side.getChildren().add(b);
        }

        if (!side.getChildren().isEmpty() && side.getChildren().get(2) instanceof ToggleButton) {
            ((ToggleButton) side.getChildren().get(2)).setSelected(true);
        }

        adminLayout.setLeft(side);


        VBox centerView = new VBox(15);
        centerView.setAlignment(Pos.CENTER);
        centerView.setStyle("-fx-background-color: #f8fafc;");

        Label mainMsg = new Label("Welcome to TECLMS Admin Portal");
        mainMsg.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subMsg = new Label("Database not connect withou run UI");
        subMsg.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        Label hintMsg = new Label("💡 ඉහත මෙනු අයිතමයන් සහ Layout එක ඔයාගේ original project එකට අනුකූල වේ.");
        hintMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #0284c7; -fx-background-color: #e0f2fe; -fx-padding: 8px 16px; -fx-background-radius: 5px;");

        centerView.getChildren().addAll(mainMsg, subMsg, hintMsg);
        adminLayout.setCenter(centerView);

        Scene scene = new Scene(adminLayout, 1200, 750);

        primaryStage.setTitle("TECLMS - Admin Portal (UI Offline Preview)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        RealLauncher.main(args);
    }
}

class RealLauncher {
    public static void main(String[] args) {


