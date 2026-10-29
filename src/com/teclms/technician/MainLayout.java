package com.teclms.technician;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;

/** Side bar and content area */
public class MainLayout extends BorderPane {

    private final StackPane content = new StackPane();

    public MainLayout() {
        getStyleClass().add("app-root");

        Map<String, Node> pages = new LinkedHashMap<>();
        pages.put("Dashboard",  scroll(new DashboardView()));
        pages.put("Attendance", scroll(new AttendanceView()));
        pages.put("Medicals",   scroll(new MedicalView()));
        pages.put("Notices",    scroll(new NoticesView()));
        pages.put("Timetable",  scroll(new TimetableView()));
        pages.put("My profile", scroll(new ProfileView()));

        setLeft(buildSidebar(pages));
        setCenter(content);
    }

    private ScrollPane scroll(Node page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private VBox buildSidebar(Map<String, Node> pages) {
        // Brand
        StackPane logo = new StackPane(new FontIcon("fas-graduation-cap"));
        logo.getStyleClass().add("brand-logo");
        Label title = new Label("TECLMS");
        title.getStyleClass().add("brand-title");
        Label sub = new Label("Technical Officer");
        sub.getStyleClass().add("brand-sub");
        HBox brand = new HBox(12, logo, new VBox(0, title, sub));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new javafx.geometry.Insets(0, 0, 8, 6));

        Label menu = new Label("MENU");
        menu.getStyleClass().add("menu-label");

        // Navigation
        String[][] items = {
                {"Dashboard",  "fas-th-large"},
                {"Attendance", "fas-clipboard-check"},
                {"Medicals",   "fas-notes-medical"},
                {"Notices",    "fas-bell"},
                {"Timetable",  "far-calendar-alt"},
                {"My profile", "fas-user-circle"}
        };

        ToggleGroup group = new ToggleGroup();
        VBox nav = new VBox(4);
        for (String[] item : items) {
            ToggleButton b = new ToggleButton(item[0], new FontIcon(item[1]));
            b.getStyleClass().add("nav-btn");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setUserData(item[0]);
            b.setToggleGroup(group);
            nav.getChildren().add(b);
        }

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);           // never leave nothing selected
            } else {
                content.getChildren().setAll(pages.get((String) newT.getUserData()));
            }
        });
        group.selectToggle(group.getToggles().get(0));

        // User box + logout

        Label initials = new Label("KP");
        initials.getStyleClass().add("user-initials");
        StackPane avatar = new StackPane(new Circle(18), initials);
        avatar.getStyleClass().add("user-avatar");
        Label name = new Label("Kasun Perera");
        name.getStyleClass().add("user-name");
        Label role = new Label("Technical Officer");
        role.getStyleClass().add("user-role");
        HBox user = new HBox(10, avatar, new VBox(0, name, role));
        user.setAlignment(Pos.CENTER_LEFT);
        user.getStyleClass().add("user-box");

        Button logout = new Button("Logout", new FontIcon("fas-sign-out-alt"));
        logout.getStyleClass().add("nav-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> Platform.exit());   // replace with your login screen later

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(4, brand, menu, nav, spacer, user, logout);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }
}
