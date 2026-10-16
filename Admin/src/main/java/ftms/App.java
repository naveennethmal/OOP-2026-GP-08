package ftms;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.function.Supplier;

public class App extends Application {
    private final BorderPane root = new BorderPane();

    @Override public void start(Stage stage) {
        VBox side = new VBox();
        side.getStyleClass().add("sidebar");
        Label brand = new Label("FTMS"); brand.getStyleClass().add("brand");
        Label sub = new Label("Admin console"); sub.getStyleClass().add("brand-sub");
        side.getChildren().addAll(brand, sub);

        ToggleGroup g = new ToggleGroup();
        nav(side, g, "Users", UsersView::new);
        nav(side, g, "Courses", CoursesView::new);
        nav(side, g, "Notices", NoticesView::new);
        nav(side, g, "Timetables", TimetableView::new);
        ((ToggleButton) side.getChildren().get(2)).fire();

        root.setLeft(side);
        Scene scene = new Scene(root, 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("Faculty of Technology Management System - Admin");
        stage.setScene(scene);
        stage.show();
    }

    private void nav(VBox side, ToggleGroup g, String text, Supplier<Node> view) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("nav");
        b.setToggleGroup(g);
        b.setOnAction(e -> { if (!b.isSelected()) b.setSelected(true); root.setCenter(view.get()); });
        side.getChildren().add(b);
    }

    public static void main(String[] args) { launch(args); }
}
