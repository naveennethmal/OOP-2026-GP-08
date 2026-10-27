package Admin;

import Common.Session;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import java.util.function.Supplier;

public class App extends BorderPane {

    private final Runnable onLogout;

    public App(Runnable onLogout) {
        this.onLogout = onLogout;
        VBox side = new VBox();
        side.getStyleClass().add("sidebar");
        Label logoIcon = new Label("\uD83C\uDF93");
        logoIcon.getStyleClass().add("logo-icon");
        Label brand = new Label("TECLMS"); brand.getStyleClass().add("brand");
        Label sub = new Label("Admin Portal"); sub.getStyleClass().add("brand-sub");
        HBox branding = new HBox(14, logoIcon, new VBox(brand, sub));
        branding.setAlignment(Pos.CENTER_LEFT);
        side.getChildren().add(branding);
        Label menu = new Label("MENU");
        menu.getStyleClass().add("menu-caption");
        side.getChildren().add(menu);

        ToggleGroup g = new ToggleGroup();
        nav(side, g, "👤", "Users", UsersView::new);
        nav(side, g, "📘", "Courses", CoursesView::new);
        nav(side, g, "🔔", "Notices", NoticesView::new);
        nav(side, g, "🔑", "Password requests", PasswordRequestsView::new);
        nav(side, g, "📅", "Timetables", TimetableView::new);
        ((ToggleButton) side.getChildren().get(2)).fire();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        SVGPath icon = new SVGPath();
        icon.setContent(
                "M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 "
                        + "10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6"
                        + "-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0"
                        + "-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z"
        );
        icon.getStyleClass().add("logout-icon");
        icon.setScaleX(20.0 / 512);
        icon.setScaleY(20.0 / 512);

        Button logout = new Button("Logout", new Group(icon));
        logout.setGraphicTextGap(16);
        logout.getStyleClass().add("logout-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setOnAction(event -> {
            Session.clear();
            onLogout.run();
        });

        side.getChildren().addAll(spacer, logout);
        setLeft(side);
    }

    private void nav(VBox side, ToggleGroup g, String icon, String text, Supplier<Node> view) {
        ToggleButton b = new ToggleButton(icon + "  " + text);
        b.getStyleClass().add("nav");
        b.setToggleGroup(g);
        b.setOnAction(e -> { if (!b.isSelected()) b.setSelected(true); setCenter(view.get()); });
        side.getChildren().add(b);
    }
}
