package Student;

import Common.Session;
import Common.AcademicResults;
import Database.DBConnection;
import Database.Db;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.util.stream.IntStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Dashboard extends BorderPane {

    private final ScrollPane scroll = new ScrollPane();
    private final Map<String, Button> menuButtons = new LinkedHashMap<>();
    private final Runnable onLogout;
    private HBox sidebarProfileCard;

    public Dashboard(Runnable onLogout) {
        this.onLogout = onLogout;

        getStyleClass().add("root-pane");

        setLeft(buildSidebar());

        scroll.setContent(buildContent());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox right = new VBox(scroll);

        right.getStyleClass().add("main-area");

        setCenter(right);

    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    private void navigate(String title) {

        menuButtons.values().forEach(
                b -> b.getStyleClass().remove("menu-item-active")
        );

        Button active = menuButtons.get(title);

        if (active != null) {
            active.getStyleClass().add("menu-item-active");
        }

        Node page = switch (title) {

            case "My Profile" ->
                    new MyProfile(this::refreshSidebarProfileImage);

            case "Attendance" ->
                    new Attendance();

            case "Medical Details" ->
                    new MedicalDetails();

            case "Course Details" ->
                    new CourseDetails();

            case "Grades & GPA" ->
                    new GradesGpa();

            case "Timetable" ->
                    new Timetable();

            case "Notices" ->
                    new Notices();

            default ->
                    buildContent();
        };

        scroll.setContent(page);
        scroll.setVvalue(0);
    }

    private void makeClickable(Node n, String target) {

        n.setCursor(Cursor.HAND);

        n.setOnMouseClicked(
                e -> navigate(target)
        );
    }

    // ============================================================
    // SIDEBAR
    // ============================================================

    private VBox buildSidebar() {

        Label logoIcon = new Label("🎓");
        logoIcon.getStyleClass().add("logo-icon");

        VBox logoText = new VBox(
                text("TECLMS", "logo-title"),
                text("Student Portal", "logo-sub")
        );

        HBox logo = new HBox(
                12,
                logoIcon,
                logoText
        );

        logo.setAlignment(Pos.CENTER_LEFT);

        VBox menu = new VBox(
                6,
                menuItem("🏠", "Dashboard"),
                menuItem("👤", "My Profile"),
                menuItem("✅", "Attendance"),
                menuItem("➕", "Medical Details"),
                menuItem("📘", "Course Details"),
                menuItem("📊", "Grades & GPA"),
                menuItem("📅", "Timetable"),
                menuItem("🔔", "Notices")
        );

        Region spacer = new Region();

        VBox.setVgrow(
                spacer,
                Priority.ALWAYS
        );

        Button logout = logoutButton();

        logout.setOnAction(e -> {

            Session.clear();

            onLogout.run();
        });

        String fullName = Session.getFullName();
        if (fullName == null || fullName.isBlank()) fullName = "Student";
        Label profileName = text(fullName, "user-name");
        profileName.setMaxWidth(Double.MAX_VALUE);
        profileName.setTextOverrun(OverrunStyle.ELLIPSIS);
        VBox profileDetails = new VBox(4, profileName, text("Student", "user-role"));
        HBox.setHgrow(profileDetails, Priority.ALWAYS);
        sidebarProfileCard = new HBox(10, createSidebarProfileImage(fullName), profileDetails);
        sidebarProfileCard.setAlignment(Pos.CENTER_LEFT);
        sidebarProfileCard.setMaxWidth(Double.MAX_VALUE);
        sidebarProfileCard.getStyleClass().add("user-box");
        makeClickable(sidebarProfileCard, "My Profile");

        VBox sidebar = new VBox(
                18,
                logo,
                text("MENU", "menu-caption"),
                menu,
                spacer,
                sidebarProfileCard,
                logout
        );

        sidebar.getStyleClass().add(
                "sidebar"
        );

        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(270);

        return sidebar;
    }

    // ============================================================
// SIDEBAR PROFILE IMAGE
// ============================================================

    private StackPane createSidebarProfileImage(String fullName) {

        double size = 44;

        StackPane box = new StackPane();

        Circle circle = new Circle(size / 2);
        circle.setStyle("-fx-fill: #2563eb;");

        Label initials = text(getInitials(fullName), "user-initials");
        box.getChildren().addAll(circle, initials);
        box.getStyleClass().add("user-avatar");
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);

        // --------------------------------------------------------
        // Get logged-in student's profile picture
        // --------------------------------------------------------

        String regNo = Session.getRegNo();

        if (regNo == null || regNo.isBlank()) {
            return box;
        }

        String sql = """
            SELECT u.profile_picture
            FROM users u
            JOIN undergraduates ug
                ON u.user_id = ug.user_id
            WHERE ug.reg_no = ?
            """;

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    regNo
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                if (result.next()) {

                    String picturePath =
                            result.getString(
                                    "profile_picture"
                            );

                    if (picturePath != null &&
                            !picturePath.isBlank()) {

                        Path path =
                                Paths.get(
                                        System.getProperty(
                                                "user.dir"
                                        ),
                                        picturePath
                                );

                        if (Files.exists(path)) {

                            Image image =
                                    new Image(
                                            path.toUri().toString(),
                                            size,
                                            size,
                                            true,
                                            true
                                    );

                            if (!image.isError()) {

                                ImageView imageView =
                                        new ImageView(image);

                                imageView.setFitWidth(size);
                                imageView.setFitHeight(size);

                                imageView.setPreserveRatio(true);
                                imageView.setSmooth(true);

                                // Make image circular
                                Circle clip =
                                        new Circle(
                                                size / 2,
                                                size / 2,
                                                size / 2
                                        );

                                imageView.setClip(clip);

                                // Remove initials
                                box.getChildren().clear();

                                // Add image
                                box.getChildren().add(
                                        imageView
                                );
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return box;
    }

    private void refreshSidebarProfileImage() {
        if (sidebarProfileCard != null) {
            sidebarProfileCard.getChildren().set(0,
                    createSidebarProfileImage(Session.getFullName()));
        }
    }

    // ============================================================
    // MENU ITEM
    // ============================================================

    private Button menuItem(
            String icon,
            String title
    ) {

        Button b = new Button(
                icon + "   " + title
        );

        b.setMaxWidth(
                Double.MAX_VALUE
        );

        b.setAlignment(
                Pos.CENTER_LEFT
        );

        b.getStyleClass().add(
                "menu-item"
        );

        if (title.equals("Dashboard")) {

            b.getStyleClass().add(
                    "menu-item-active"
            );
        }

        b.setOnAction(
                e -> navigate(title)
        );

        menuButtons.put(
                title,
                b
        );

        return b;
    }

    // ============================================================
    // LOGOUT BUTTON
    // ============================================================

    private Button logoutButton() {

        SVGPath icon = new SVGPath();

        icon.setContent(
                "M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 "
                        + "10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6"
                        + "-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0"
                        + "-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z"
        );

        icon.getStyleClass().add(
                "logout-icon"
        );

        icon.setScaleX(
                20.0 / 512
        );

        icon.setScaleY(
                20.0 / 512
        );

        Button b = new Button(
                "Logout",
                new Group(icon)
        );

        b.setGraphicTextGap(16);

        b.setMaxWidth(
                Double.MAX_VALUE
        );

        b.setAlignment(
                Pos.CENTER_LEFT
        );

        b.getStyleClass().add(
                "logout-btn"
        );

        return b;
    }

    // ============================================================
    // TOP BAR
    // ============================================================

    private HBox buildTopBar() {

        Label bell = new Label("🔔");

        bell.getStyleClass().add(
                "bell"
        );

        StackPane bellPane = new StackPane(bell);

        makeClickable(
                bellPane,
                "Notices"
        );

        String fullName = Session.getFullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = "Student";
        }

        HBox userCard = new HBox(
                16,
                createSidebarProfileImage(fullName),
                new VBox(
                        4,
                        text(fullName, "user-name"),
                        text("Student", "user-role")
                )
        );
        userCard.setAlignment(Pos.CENTER_LEFT);
        userCard.getStyleClass().add("top-profile");
        makeClickable(userCard, "My Profile");

        HBox bar = new HBox(
                20,
                bellPane,
                userCard
        );

        bar.setAlignment(Pos.TOP_RIGHT);
        bar.setMinHeight(94);
        bar.setPrefHeight(94);
        bar.setMaxWidth(Double.MAX_VALUE);

        bar.getStyleClass().add(
                "top-bar"
        );

        return bar;
    }

    // ============================================================
    // MAIN CONTENT
    // ============================================================

    private VBox buildContent() {
        VBox content = new VBox(18, buildDashboardHeader(), buildWelcome(), databaseSummary());

        content.getStyleClass().add(
                "content"
        );

        return content;
    }

    private VBox buildDashboardHeader() {
        Label heading = text("Dashboard", "page-title");
        Label subtitle = text("Overview of your courses, attendance and academic progress.", "page-subtitle");
        Separator divider = new Separator();
        divider.setMaxWidth(Double.MAX_VALUE);
        VBox header = new VBox(5, heading, subtitle, divider);
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private VBox databaseSummary() {
        String regNo = Session.getRegNo();
        if (regNo == null || regNo.isBlank()) {
            return new VBox(
                    12,
                    text("Please login to view your dashboard.", "err"));
        }

        try {
            int courses = number(
                    "SELECT COUNT(*) total FROM enrollments WHERE reg_no = ?",
                    regNo);

            int notices = number(
                    "SELECT COUNT(*) total FROM notices");

            String attendance = decimal(
                    "SELECT COALESCE(ROUND("
                            + "100 * SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END)"
                            + " / NULLIF(COUNT(*), 0), 0), 0) percentage "
                            + "FROM attendance a "
                            + "JOIN (SELECT DISTINCT reg_no, course_code FROM enrollments) e "
                            + "ON e.reg_no = a.reg_no AND e.course_code = a.course_code "
                            + "WHERE a.reg_no = ?",
                    regNo) + "%";

            var completedGrades = AcademicResults.beforeCurrentTerm(regNo);
            String cgpa = AcademicResults.hasGpaResults(completedGrades)
                    ? String.format("%.2f", AcademicResults.gpa(completedGrades))
                    : "Pending";

            HBox stats = new HBox(
                    16,
                    statCard("📘", "My Courses", String.valueOf(courses),
                            "View Courses →", "blue", "Course Details"),
                    statCard("✅", "Attendance", attendance,
                            "View Details →", "green", "Attendance"),
                    statCard("📊", "CGPA", cgpa,
                            "View Grades →", "purple", "Grades & GPA"),
                    statCard("🔔", "Notices", String.valueOf(notices),
                            "View Notices →", "yellow", "Notices"));

            stats.getChildren().forEach(node ->
                    HBox.setHgrow(node, Priority.ALWAYS));

            return new VBox(18, stats, calendarCard());
        } catch (Exception ex) {
            return new VBox(
                    12,
                    text("Unable to load dashboard data: " + ex.getMessage(), "err"));
        }
    }

    private HBox statCard(String icon, String title, String value,
                          String link, String color, String target) {
        StackPane iconBox = new StackPane(text(icon, "stat-icon"));
        iconBox.getStyleClass().addAll("stat-icon-box", "icon-" + color);

        VBox info = new VBox(
                6,
                text(title, "stat-title"),
                text(value, "stat-value"),
                text(link, "stat-link"));

        HBox card = new HBox(16, iconBox, info);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().addAll("stat-card", "card-" + color);
        makeClickable(card, target);
        return card;
    }

    private VBox calendarCard() {
        GridPane calendar = new GridPane();
        calendar.setHgap(10);
        calendar.setVgap(10);
        calendar.setMaxWidth(Double.MAX_VALUE);

        YearMonth current = YearMonth.now();
        ComboBox<String> monthSelector = new ComboBox<>();
        monthSelector.getItems().addAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthSelector.setValue(current.getMonth().name().charAt(0)
                + current.getMonth().name().substring(1).toLowerCase());
        monthSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        });
        monthSelector.setEditable(false);
        monthSelector.setPromptText("Select month");
        monthSelector.setPrefWidth(150);
        monthSelector.setMinWidth(150);
        monthSelector.setPrefHeight(45);
        monthSelector.getStyleClass().add("calendar-selector");
        monthSelector.getSelectionModel().select(monthSelector.getValue());

        ComboBox<Integer> yearSelector = new ComboBox<>();
        IntStream.rangeClosed(current.getYear() - 5, current.getYear() + 5)
                .forEach(yearSelector.getItems()::add);
        yearSelector.getSelectionModel().select(current.getYear());
        yearSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
                setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        });
        yearSelector.setEditable(false);
        yearSelector.setPromptText("Select year");
        yearSelector.setPrefWidth(125);
        yearSelector.setMinWidth(125);
        yearSelector.setPrefHeight(45);
        yearSelector.getStyleClass().add("calendar-selector");
        yearSelector.setValue(current.getYear());

        Runnable refreshCalendar = () -> {
            String selectedMonth = monthSelector.getValue();
            Integer selectedYear = yearSelector.getValue();
            int monthNumber = monthSelector.getItems().indexOf(selectedMonth) + 1;
            if (selectedMonth == null || selectedYear == null || monthNumber < 1) return;
            populateCalendar(calendar, YearMonth.of(selectedYear, monthNumber));
        };
        monthSelector.setOnAction(event -> refreshCalendar.run());
        yearSelector.setOnAction(event -> refreshCalendar.run());

        VBox monthField = new VBox(
                6,
                text("Month", "calendar-selector-label"),
                monthSelector);
        VBox yearField = new VBox(
                6,
                text("Year", "calendar-selector-label"),
                yearSelector);
        HBox selectors = new HBox(16, monthField, yearField);
        selectors.setAlignment(Pos.BOTTOM_LEFT);
        selectors.getStyleClass().add("calendar-selectors");

        populateCalendar(calendar, current);

        VBox body = new VBox(12, selectors, calendar);
        body.getStyleClass().add("calendar-body");
        VBox result = new VBox(16, text("📅  Calendar", "card-title"), body);
        result.getStyleClass().add("card");
        return result;
    }

    private void populateCalendar(GridPane calendar, YearMonth month) {
        calendar.getChildren().clear();
        calendar.getColumnConstraints().clear();

        LocalDate firstDay = month.atDay(1);
        LocalDate today = LocalDate.now();
        String[] dayNames = {"Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday", "Sunday"};
        for (int column = 0; column < dayNames.length; column++) {
            Label day = text(dayNames[column], "calendar-day-header");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setAlignment(Pos.CENTER);
            calendar.add(day, column, 0);
            GridPane.setHgrow(day, Priority.ALWAYS);
        }

        int startColumn = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int dayNumber = 1; dayNumber <= month.lengthOfMonth(); dayNumber++) {
            LocalDate date = month.atDay(dayNumber);
            int position = startColumn + dayNumber - 1;
            int column = position % 7;
            int row = position / 7 + 1;

            Label day = text(String.valueOf(dayNumber), "calendar-day");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setMinHeight(54);
            day.setAlignment(Pos.CENTER);
            if (date.equals(today)) {
                day.getStyleClass().add("calendar-day-today");
            }
            calendar.add(day, column, row);
            GridPane.setHgrow(day, Priority.ALWAYS);
        }

        for (int column = 0; column < 7; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / 7);
            calendar.getColumnConstraints().add(constraints);
        }
    }

    private static int number(String sql, Object... parameters) throws Exception {
        List<Map<String, Object>> rows = Db.query(sql, parameters);
        return rows.isEmpty() ? 0 : ((Number) rows.get(0).get("total")).intValue();
    }

    private static String decimal(String sql, Object... parameters) throws Exception {
        List<Map<String, Object>> rows = Db.query(sql, parameters);
        if (rows.isEmpty() || rows.get(0).values().iterator().next() == null) {
            return "0.00";
        }
        return String.format("%.2f",
                ((Number) rows.get(0).values().iterator().next()).doubleValue());
    }

    // ============================================================
    // WELCOME
    // ============================================================

    private HBox buildWelcome() {

        String fullName =
                Session.getFullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = "Student";
        }

        VBox greet = new VBox(
                4,
                text(
                        "Welcome back, " + fullName + "!",
                        "welcome-title"
                ),
                text(
                        "Keep going, your future is bright! 🚀",
                        "welcome-sub"
                )
        );

        greet.setAlignment(
                Pos.CENTER_LEFT
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Label quote = text(
                "“Small steps every day\nlead to big results.”",
                "quote-text"
        );

        HBox quoteBox = new HBox(
                20,
                quote,
                text("🎓", "quote-icon")
        );

        quoteBox.setAlignment(
                Pos.CENTER
        );

        quoteBox.getStyleClass().add(
                "quote-box"
        );

        HBox box = new HBox(
                20,
                greet,
                spacer,
                quoteBox
        );

        box.setAlignment(
                Pos.CENTER_LEFT
        );

        box.getStyleClass().add(
                "welcome"
        );

        return box;
    }

    // ============================================================
    // GET INITIALS
    // ============================================================

    private String getInitials(
            String name
    ) {

        if (name == null ||
                name.isBlank()) {

            return "ST";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                parts[0]
                        .substring(0, 1)
                        .toUpperCase()

                        +

                        parts[parts.length - 1]
                                .substring(0, 1)
                                .toUpperCase()
        );
    }

    // ============================================================
    // TEXT HELPER
    // ============================================================

    private Label text(
            String value,
            String... styles
    ) {

        Label l = new Label(
                value
        );

        l.getStyleClass().addAll(
                styles
        );

        return l;
    }
}
