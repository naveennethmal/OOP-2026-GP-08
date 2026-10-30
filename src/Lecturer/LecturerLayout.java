package Lecturer;

import Common.Session;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Lecturer portal: side bar on the left, the selected page on the right. Started from Main.showLecturer(). */
public class LecturerLayout extends BorderPane {

    private final StackPane content = new StackPane();
    private final Map<String, ToggleButton> menu = new LinkedHashMap<>();
    private final Circle avatar = new Circle(22, Color.web("#2563eb"));
    private final Label initials = new Label("L");
    private final Label nameLabel = new Label("Lecturer");

    public LecturerLayout(Runnable onLogout) {
        getStyleClass().add("app-root");

        // pages are created every time they are opened, so they always show fresh data
        Map<String, Supplier<Node>> pages = new LinkedHashMap<>();
        pages.put("Dashboard", () -> scroll(new HomeView(this::go)));
        pages.put("My profile", () -> scroll(new ProfileView(this::refreshUser)));
        pages.put("Course materials", () -> scroll(new MaterialsView()));
        pages.put("Upload marks", () -> scroll(new MarksView()));
        pages.put("CA & eligibility", () -> scroll(new EligibilityView()));
        pages.put("Attendance & medical", () -> scroll(new AttendanceView()));
        pages.put("Grades & GPA", () -> scroll(new GradesView()));
        pages.put("Student details", () -> scroll(new StudentsView()));
        pages.put("Notices", () -> scroll(new NoticesView()));

        String[] icons = {"🏠", "👤", "📚", "📝", "✅", "🗓", "🎓", "👥", "🔔"};
        setLeft(buildSidebar(pages, icons, onLogout));
        setCenter(content);
        refreshUser();
    }

    /** Opens a page by its menu name (used by the dashboard cards). */
    private void go(String page) {
        ToggleButton b = menu.get(page);
        if (b != null) b.setSelected(true);
    }

    private ScrollPane scroll(Node page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private VBox buildSidebar(Map<String, Supplier<Node>> pages, String[] icons, Runnable onLogout) {
        Label logoIcon = new Label("🎓");
        logoIcon.getStyleClass().add("logo-icon");
        Label title = new Label("TECLMS");
        title.getStyleClass().add("logo-title");
        Label sub = new Label("Lecturer Portal");
        sub.getStyleClass().add("logo-sub");
        HBox brand = new HBox(12, logoIcon, new VBox(0, title, sub));
        brand.setAlignment(Pos.CENTER_LEFT);

        Label caption = new Label("MENU");
        caption.getStyleClass().add("menu-caption");

        ToggleGroup group = new ToggleGroup();
        VBox nav = new VBox(4);
        int n = 0;
        for (String name : pages.keySet()) {
            ToggleButton b = new ToggleButton(icons[n++] + "  " + name);
            b.getStyleClass().add("menu-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setUserData(name);
            b.setToggleGroup(group);
            menu.put(name, b);
            nav.getChildren().add(b);
        }

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                if (oldT != null) oldT.setSelected(true);     // never leave nothing selected
                return;
            }
            if (oldT instanceof ToggleButton) {
                ((ToggleButton) oldT).getStyleClass().remove("menu-item-active");
            }
            if (newT instanceof ToggleButton) {
                ((ToggleButton) newT).getStyleClass().add("menu-item-active");
            }
            content.getChildren().setAll(pages.get((String) newT.getUserData()).get());
        });
        group.selectToggle(group.getToggles().get(0));

        // who is logged in + logout
        initials.getStyleClass().add("user-initials");
        StackPane picture = new StackPane(avatar, initials);
        picture.getStyleClass().add("user-avatar");
        nameLabel.getStyleClass().add("user-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label role = new Label("Lecturer");
        role.getStyleClass().add("user-role");
        VBox identity = new VBox(0, nameLabel, role);
        HBox.setHgrow(identity, Priority.ALWAYS);
        HBox user = new HBox(10, picture, identity);
        user.setAlignment(Pos.CENTER_LEFT);
        user.getStyleClass().add("user-box");

        SVGPath logoutIcon = new SVGPath();
        logoutIcon.setContent("M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 "
                + "10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6"
                + "-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0"
                + "-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z");
        logoutIcon.getStyleClass().add("logout-icon");
        logoutIcon.setScaleX(18.0 / 512);
        logoutIcon.setScaleY(18.0 / 512);

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

        VBox sidebar = new VBox(4, brand, caption, nav, spacer, user, logout);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    /** Reloads the name and picture in the side bar (called after the profile is saved). */
    private void refreshUser() {
        try {
            LecturerDAO.Profile p = LecturerDAO.profile();
            nameLabel.setText(p.fullName());
            LecturerUi.paintAvatar(avatar, initials, p.picture(), p.initials());
        } catch (Exception e) {
            System.err.println("Could not load the lecturer: " + e.getMessage());
        }
    }
}
