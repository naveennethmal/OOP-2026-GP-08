package com.teclms.technician;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // Taskbar eka ain kala screen eke visible area eka
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Screen ekata fit wenawa, max 1280x780
        double width = Math.min(1280, screen.getWidth() * 0.92);
        double height = Math.min(780, screen.getHeight() * 0.90);

        Scene scene = new Scene(new MainLayout(), width, height);
        scene.getStylesheets().add(MainApp.class.getResource("styles.css").toExternalForm());

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