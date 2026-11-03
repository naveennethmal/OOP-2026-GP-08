package Technical_Officer;

import Common.Session;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static Technical_Officer.TechnicianDAO.text;

/** Side bar (menu, user box, logout) and the content area. */
public class MainLayout extends BorderPane {

    private final StackPane content = new StackPane();
    private final Circle avatar = new Circle(22, Color.web("#2563eb"));
    private final Label initials = new Label("TO");
    private final Label userName = new Label("Technical Officer");

    public MainLayout(Runnable onLogout) {
        getStyleClass().add("app-root");

        // Pages are created each time they are opened, so they always show the latest data.
        Map<String, Supplier<Node>> pages = new LinkedHashMap<>();
        pages.put("Dashboard", () -> scroll(new DashboardView()));
        pages.put("My profile", () -> scroll(new ProfileView(this::refreshUserBox)));
        pages.put("Attendance", () -> scroll(new AttendanceView()));
        pages.put("Medicals", () -> scroll(new MedicalView()));
        pages.put("Notices", () -> scroll(new NoticesView()));
        pages.put("Timetable", () -> scroll(new TimetableView()));

        setLeft(buildSidebar(pages, onLogout));
        setCenter(content);
        refreshUserBox();
    }

    private ScrollPane scroll(Node page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private VBox buildSidebar(Map<String, Supplier<Node>> pages, Runnable onLogout) {
        // brand
        Label logoIcon = new Label("🎓");
        logoIcon.getStyleClass().add("logo-icon");
        Label title = new Label("TECLMS");
        title.getStyleClass().add("logo-title");
        Label sub = new Label("Technical Officer Portal");
        sub.getStyleClass().add("logo-sub");
        HBox brand = new HBox(12, logoIcon, new VBox(0, title, sub));
        brand.setAlignment(Pos.CENTER_LEFT);

        Label menu = new Label("MENU");
        menu.getStyleClass().add("menu-caption");

        // navigation: one toggle button per page
        Map<String, String> emojis = Map.of("Dashboard", "🏠", "My profile", "👤", "Attendance", "✅",
                "Medicals", "➕", "Notices", "🔔", "Timetable", "📅");
        ToggleGroup group = new ToggleGroup();
        VBox nav = new VBox(4);
        for (String pageName : pages.keySet()) {
            ToggleButton b = new ToggleButton(emojis.get(pageName) + "  " + pageName);
            b.getStyleClass().add("menu-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setUserData(pageName);
            b.setToggleGroup(group);
            nav.getChildren().add(b);
        }
        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);                       // never leave nothing selected
                return;
            }
            if (oldT != null) oldT.getToggleGroup().getToggles().forEach(
                    t -> ((ToggleButton) t).getStyleClass().remove("menu-item-active"));
            ((ToggleButton) newT).getStyleClass().add("menu-item-active");
            content.getChildren().setAll(pages.get((String) newT.getUserData()).get());
        });
        group.selectToggle(group.getToggles().get(0));

        // user box
        initials.getStyleClass().add("user-initials");
        StackPane avatarPane = new StackPane(avatar, initials);
        avatarPane.getStyleClass().add("user-avatar");
        userName.getStyleClass().add("user-name");
        userName.setMaxWidth(Double.MAX_VALUE);
        userName.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label role = new Label("Technical Officer");
        role.getStyleClass().add("user-role");
        VBox identity = new VBox(0, userName, role);
        HBox.setHgrow(identity, Priority.ALWAYS);
        HBox user = new HBox(10, avatarPane, identity);
        user.setAlignment(Pos.CENTER_LEFT);
        user.setMaxWidth(Double.MAX_VALUE);
        user.getStyleClass().add("user-box");

        // logout
        SVGPath logoutIcon = new SVGPath();
        logoutIcon.setContent("M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 "
                + "10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6"
                + "-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0"
                + "-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z");
        logoutIcon.getStyleClass().add("logout-icon");
        logoutIcon.setScaleX(20.0 / 512);
        logoutIcon.setScaleY(20.0 / 512);
        Button logout = new Button("Logout", new Group(logoutIcon));
        logout.setGraphicTextGap(16);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.getStyleClass().add("logout-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            Session.clear();
            onLogout.run();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(4, brand, menu, nav, spacer, user, logout);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    /** Loads the name and photo of the logged-in officer into the user box (also called after a profile save). */
    private void refreshUserBox() {
        try {
            Map<String, Object> profile = TechnicianDAO.profile();
            userName.setText(text(profile.get("full_name")));
            UiKit.setAvatar(avatar, initials, text(profile.get("full_name")), text(profile.get("profile_picture")));
        } catch (Exception e) {
            System.err.println("Could not load the user: " + e.getMessage());
        }
    }
}
