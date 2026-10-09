import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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

    private final Label dateLabel = new Label();
    private final Label timeLabel = new Label();
    private final ScrollPane scroll = new ScrollPane();
    private final Map<String, Button> menuButtons = new LinkedHashMap<>();

    public Dashboard() {

        getStyleClass().add("root-pane");

        setLeft(buildSidebar());

        scroll.setContent(buildContent());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox right = new VBox(
                buildTopBar(),
                scroll
        );

        right.getStyleClass().add("main-area");

        setCenter(right);

        startClock();
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
                    new MyProfile();

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

        // Logged-in student's name
        String fullName = Session.getFullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = "Student";
        }

        HBox userCard = new HBox(
                12,
                createSidebarProfileImage(fullName),
                new VBox(
                        2,
                        text(
                                fullName,
                                "user-name"
                        ),
                        text(
                                "Student",
                                "user-role"
                        )
                )
        );

        userCard.setAlignment(
                Pos.CENTER_LEFT
        );

        userCard.getStyleClass().add(
                "sidebar-user"
        );

        Button logout = logoutButton();

        logout.setOnAction(e -> {

            Session.clear();

            Main.showLogin();
        });

        VBox sidebar = new VBox(
                18,
                logo,
                text("MENU", "menu-caption"),
                menu,
                spacer,
                userCard,
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

        Circle circle = new Circle(
                size / 2
        );

        circle.getStyleClass().add(
                "avatar-circle"
        );

        // --------------------------------------------------------
        // Default initials
        // --------------------------------------------------------

        Label initials = text(
                getInitials(fullName),
                "avatar-text"
        );

        initials.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: white;"
        );

        box.getChildren().addAll(
                circle,
                initials
        );

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

        TextField search = new TextField();

        search.setPromptText(
                "🔍  Search anything..."
        );

        search.getStyleClass().add(
                "search"
        );

        HBox.setHgrow(
                search,
                Priority.ALWAYS
        );

        Label bell = new Label("🔔");

        bell.getStyleClass().add(
                "bell"
        );

        Label badge = text(
                "3",
                "badge"
        );

        StackPane bellPane = new StackPane(
                bell,
                badge
        );

        StackPane.setAlignment(
                badge,
                Pos.TOP_RIGHT
        );

        makeClickable(
                bellPane,
                "Notices"
        );

        VBox dateTime = new VBox(
                4,
                dateLabel,
                timeLabel
        );

        dateLabel.getStyleClass().add(
                "date-text"
        );

        timeLabel.getStyleClass().add(
                "time-text"
        );

        // Logged-in student's initials
        String fullName = Session.getFullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = "Student";
        }

        StackPane me = avatar(
                getInitials(fullName),
                42
        );

        makeClickable(
                me,
                "My Profile"
        );

        HBox bar = new HBox(
                20,
                search,
                bellPane,
                dateTime,
                me
        );

        bar.setAlignment(
                Pos.CENTER
        );

        bar.getStyleClass().add(
                "top-bar"
        );

        return bar;
    }

    // ============================================================
    // CLOCK
    // ============================================================

    private void startClock() {

        Timeline t = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        e -> {

                            LocalDateTime now =
                                    LocalDateTime.now();

                            dateLabel.setText(
                                    "📅  " +
                                            now.format(
                                                    DateTimeFormatter.ofPattern(
                                                            "EEEE, dd MMMM yyyy"
                                                    )
                                            )
                            );

                            timeLabel.setText(
                                    "🕙  " +
                                            now.format(
                                                    DateTimeFormatter.ofPattern(
                                                            "hh:mm a"
                                                    )
                                            )
                            );
                        }
                )
        );

        t.setCycleCount(
                Timeline.INDEFINITE
        );

        t.play();

        t.getKeyFrames()
                .get(0)
                .getOnFinished()
                .handle(null);
    }

    // ============================================================
    // MAIN CONTENT
    // ============================================================

    private VBox buildContent() {

        VBox content = new VBox(
                18,
                buildWelcome(),
                buildStatCards(),
                buildColumns()
        );

        content.getStyleClass().add(
                "content"
        );

        return content;
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
                avatar(
                        getInitials(fullName),
                        90
                ),
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
    // STAT CARDS
    // ============================================================

    private HBox buildStatCards() {

        HBox row = new HBox(
                16,
                statCard(
                        "📘",
                        "My Courses",
                        "6",
                        "View Courses →",
                        "blue",
                        "Course Details"
                ),
                statCard(
                        "📅",
                        "Attendance",
                        "92%",
                        "View Details →",
                        "green",
                        "Attendance"
                ),
                statCard(
                        "➕",
                        "Medical Details",
                        "All Clear",
                        "View Details →",
                        "red",
                        "Medical Details"
                ),
                statCard(
                        "A+",
                        "GPA",
                        "3.75",
                        "View Grades →",
                        "purple",
                        "Grades & GPA"
                ),
                statCard(
                        "🔔",
                        "Notices",
                        "3",
                        "View All →",
                        "yellow",
                        "Notices"
                )
        );

        row.getChildren().forEach(
                n -> HBox.setHgrow(
                        n,
                        Priority.ALWAYS
                )
        );

        return row;
    }

    private HBox statCard(
            String icon,
            String title,
            String value,
            String link,
            String color,
            String target
    ) {

        Label ic = text(
                icon,
                "stat-icon"
        );

        StackPane iconBox = new StackPane(
                ic
        );

        iconBox.getStyleClass().addAll(
                "stat-icon-box",
                "icon-" + color
        );

        VBox info = new VBox(
                6,
                text(title, "stat-title"),
                text(value, "stat-value"),
                text(link, "stat-link")
        );

        HBox card = new HBox(
                16,
                iconBox,
                info
        );

        card.setAlignment(
                Pos.CENTER_LEFT
        );

        card.setMaxWidth(
                Double.MAX_VALUE
        );

        card.getStyleClass().addAll(
                "stat-card",
                "card-" + color
        );

        makeClickable(
                card,
                target
        );

        return card;
    }

    // ============================================================
    // COLUMNS
    // ============================================================

    private HBox buildColumns() {

        VBox left = new VBox(
                18,
                attendanceCard(),
                coursesCard()
        );

        VBox mid = new VBox(
                18,
                noticesCard(),
                timetableCard()
        );

        VBox right = new VBox(
                18,
                profileCard(),
                quickLinksCard()
        );

        HBox row = new HBox(
                18,
                left,
                mid,
                right
        );

        HBox.setHgrow(
                left,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                mid,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                right,
                Priority.ALWAYS
        );

        left.setPrefWidth(500);
        mid.setPrefWidth(500);
        right.setPrefWidth(380);

        return row;
    }

    // ============================================================
    // ATTENDANCE
    // ============================================================

    private VBox attendanceCard() {

        double size = 140;
        double r = 58;

        Circle track = new Circle(
                size / 2,
                size / 2,
                r,
                Color.TRANSPARENT
        );

        track.setStroke(
                Color.web("#e5e9f2")
        );

        track.setStrokeWidth(14);

        Arc arc = new Arc(
                size / 2,
                size / 2,
                r,
                r,
                90,
                -360 * 0.92
        );

        arc.setType(
                ArcType.OPEN
        );

        arc.setFill(
                Color.TRANSPARENT
        );

        arc.setStroke(
                Color.web("#22c07a")
        );

        arc.setStrokeWidth(14);

        arc.setStrokeLineCap(
                javafx.scene.shape.StrokeLineCap.ROUND
        );

        Pane ring = new Pane(
                track,
                arc
        );

        ring.setPrefSize(
                size,
                size
        );

        ring.setMinSize(
                size,
                size
        );

        VBox center = new VBox(
                text("92%", "donut-value"),
                text("Present", "muted")
        );

        center.setAlignment(
                Pos.CENTER
        );

        center.setPrefSize(
                size,
                size
        );

        StackPane donut = new StackPane(
                ring,
                center
        );

        VBox legend = new VBox(
                0,
                legendRow(
                        "#22c07a",
                        "Present",
                        "46"
                ),
                legendRow(
                        "#ef4b5f",
                        "Absent",
                        "3"
                ),
                legendRow(
                        "#f5a623",
                        "Late",
                        "1"
                )
        );

        legend.setAlignment(
                Pos.CENTER
        );

        HBox.setHgrow(
                legend,
                Priority.ALWAYS
        );

        HBox body = new HBox(
                24,
                donut,
                legend
        );

        body.setAlignment(
                Pos.CENTER_LEFT
        );

        return card(
                "📅",
                "Attendance Overview",
                "View All →",
                "Attendance",
                body
        );
    }

    private HBox legendRow(
            String color,
            String name,
            String count
    ) {

        Circle dot = new Circle(
                6,
                Color.web(color)
        );

        Region sp = new Region();

        HBox.setHgrow(
                sp,
                Priority.ALWAYS
        );

        HBox row = new HBox(
                12,
                dot,
                text(name, "row-text"),
                sp,
                text(count, "row-text")
        );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        row.getStyleClass().add(
                "legend-row"
        );

        return row;
    }

    // ============================================================
    // COURSES
    // ============================================================

    private VBox coursesCard() {

        String[][] courses = {

                {
                        "Java Programming",
                        "ICT1212",
                        "0.75",
                        "#2f80ff"
                },

                {
                        "Database Systems",
                        "ICT1222",
                        "0.60",
                        "#22c07a"
                },

                {
                        "Software Engineering",
                        "ICT1232",
                        "0.45",
                        "#7c5cff"
                },

                {
                        "Web Technologies",
                        "ICT1242",
                        "0.30",
                        "#f5a623"
                },

                {
                        "Computer Networks",
                        "ICT1252",
                        "0.20",
                        "#19c2d8"
                },

                {
                        "Data Structures & Algorithms",
                        "ICT1261",
                        "0.10",
                        "#ef4b8a"
                }
        };

        VBox list = new VBox(14);

        for (String[] c : courses) {

            double p =
                    Double.parseDouble(c[2]);

            ProgressBar bar =
                    new ProgressBar(p);

            bar.setPrefWidth(150);

            bar.setStyle(
                    "-fx-accent: " + c[3] + ";"
            );

            bar.getStyleClass().add(
                    "course-bar"
            );

            Label dot = text(
                    "●",
                    "course-dot"
            );

            dot.setStyle(
                    "-fx-text-fill: " + c[3] + ";"
            );

            VBox names = new VBox(
                    1,
                    text(c[0], "course-name"),
                    text(c[1], "muted-small")
            );

            Region sp = new Region();

            HBox.setHgrow(
                    sp,
                    Priority.ALWAYS
            );

            HBox row = new HBox(
                    12,
                    dot,
                    names,
                    sp,
                    bar,
                    text(
                            (int) (p * 100) + "%",
                            "muted-small"
                    ),
                    text("›", "chevron")
            );

            row.setAlignment(
                    Pos.CENTER_LEFT
            );

            makeClickable(
                    row,
                    "Course Details"
            );

            list.getChildren().add(
                    row
            );
        }

        return card(
                "📘",
                "My Courses",
                "View All →",
                "Course Details",
                list
        );
    }

    // ============================================================
    // NOTICES
    // ============================================================

    private VBox noticesCard() {

        VBox list = new VBox(
                12,
                notice(
                        "Mid-semester examination schedule released",
                        "2026-09-25",
                        true
                ),
                new Separator(),
                notice(
                        "Lab maintenance on Friday",
                        "2026-09-24",
                        false
                ),
                new Separator(),
                notice(
                        "Medical submission deadline",
                        "2026-09-20",
                        false
                )
        );

        return card(
                "🔔",
                "Recent Notices",
                "View All →",
                "Notices",
                list
        );
    }

    private HBox notice(
            String title,
            String date,
            boolean isNew
    ) {

        VBox t = new VBox(
                4,
                text(title, "notice-title"),
                text(date, "muted-small")
        );

        Region sp = new Region();

        HBox.setHgrow(
                sp,
                Priority.ALWAYS
        );

        HBox row = new HBox(
                t,
                sp
        );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        if (isNew) {

            row.getChildren().add(
                    text(
                            "New",
                            "tag-new"
                    )
            );
        }

        makeClickable(
                row,
                "Notices"
        );

        return row;
    }

    // ============================================================
    // TIMETABLE
    // ============================================================

    private VBox timetableCard() {

        String[][] rows = {

                {
                        "10:00 AM - 11:00 AM",
                        "Java Programming",
                        "Lab 1"
                },

                {
                        "02:00 PM - 03:00 PM",
                        "Database Systems",
                        "Lab 2"
                },

                {
                        "03:00 PM - 04:00 PM",
                        "Web Technologies",
                        "Lab 3"
                },

                {
                        "04:00 PM - 05:00 PM",
                        "Computer Networks",
                        "Room 101"
                }
        };

        GridPane grid = new GridPane();

        grid.setHgap(16);
        grid.setVgap(14);

        ColumnConstraints c1 =
                new ColumnConstraints();

        c1.setPercentWidth(38);

        ColumnConstraints c2 =
                new ColumnConstraints();

        c2.setPercentWidth(40);

        ColumnConstraints c3 =
                new ColumnConstraints();

        c3.setPercentWidth(22);

        grid.getColumnConstraints().addAll(
                c1,
                c2,
                c3
        );

        String[] head = {
                "Time",
                "Subject",
                "Room"
        };

        for (int i = 0; i < 3; i++) {

            Label h = text(
                    head[i],
                    "table-head"
            );

            h.setMaxWidth(
                    Double.MAX_VALUE
            );

            grid.add(
                    h,
                    i,
                    0
            );
        }

        for (int r = 0; r < rows.length; r++) {

            for (int c = 0; c < 3; c++) {

                grid.add(
                        text(
                                rows[r][c],
                                "table-cell"
                        ),
                        c,
                        r + 1
                );
            }
        }

        return card(
                "📅",
                "Timetable",
                "View Full Timetable →",
                "Timetable",
                grid
        );
    }

    // ============================================================
    // PROFILE CARD
    // ============================================================

    private VBox profileCard() {

        String fullName =
                Session.getFullName();

        String regNo =
                Session.getRegNo();

        if (fullName == null || fullName.isBlank()) {
            fullName = "Student";
        }

        if (regNo == null || regNo.isBlank()) {
            regNo = "-";
        }

        HBox head = new HBox(
                16,
                avatar(
                        getInitials(fullName),
                        72
                ),
                new VBox(
                        2,
                        text(
                                fullName,
                                "profile-name"
                        ),
                        text(
                                "Student",
                                "muted"
                        )
                )
        );

        head.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox info = new VBox(
                12,
                head,

                profileRow(
                        "🪪",
                        "Student ID",
                        regNo
                ),

                profileRow(
                        "✉",
                        "Email",
                        "View My Profile →"
                ),

                profileRow(
                        "📞",
                        "Phone",
                        "View My Profile →"
                ),

                profileRow(
                        "📍",
                        "Address",
                        "View My Profile →"
                )
        );

        return card(
                "👤",
                "My Profile",
                "Edit",
                "My Profile",
                info
        );
    }

    private HBox profileRow(
            String icon,
            String key,
            String value
    ) {

        Label k = text(
                icon + "  " + key,
                "profile-key"
        );

        k.setMinWidth(110);

        HBox row = new HBox(
                8,
                k,
                text(
                        value,
                        "profile-value"
                )
        );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        return row;
    }

    // ============================================================
    // QUICK LINKS
    // ============================================================

    private VBox quickLinksCard() {

        GridPane grid = new GridPane();

        grid.setHgap(12);
        grid.setVgap(12);

        String[][] links = {

                {
                        "📅",
                        "Attendance",
                        "Attendance"
                },

                {
                        "➕",
                        "Medical Details",
                        "Medical Details"
                },

                {
                        "📘",
                        "Course Details",
                        "Course Details"
                },

                {
                        "📊",
                        "Grades & GPA",
                        "Grades & GPA"
                },

                {
                        "A+",
                        "Timetable",
                        "Timetable"
                },

                {
                        "📅",
                        "Timetable",
                        "Timetable"
                },

                {
                        "🔔",
                        "Notices",
                        "Notices"
                }
        };

        int[][] pos = {
                {0, 0, 3},
                {3, 0, 3},
                {0, 1, 2},
                {2, 1, 2},
                {4, 1, 2},
                {0, 2, 3},
                {3, 2, 3}
        };

        for (int i = 0; i < links.length; i++) {

            Button b = new Button(
                    links[i][0]
                            + "\n"
                            + links[i][1]
            );

            b.getStyleClass().add(
                    "quick-btn"
            );

            b.setMaxWidth(
                    Double.MAX_VALUE
            );

            String target =
                    links[i][2];

            b.setOnAction(
                    e -> navigate(target)
            );

            GridPane.setHgrow(
                    b,
                    Priority.ALWAYS
            );

            grid.add(
                    b,
                    pos[i][0],
                    pos[i][1],
                    pos[i][2],
                    1
            );
        }

        for (int i = 0; i < 6; i++) {

            ColumnConstraints cc =
                    new ColumnConstraints();

            cc.setPercentWidth(
                    100.0 / 6
            );

            grid.getColumnConstraints()
                    .add(cc);
        }

        return card(
                "🔗",
                "Quick Links",
                null,
                null,
                grid
        );
    }

    // ============================================================
    // CARD
    // ============================================================

    private VBox card(
            String icon,
            String title,
            String link,
            String target,
            Node body
    ) {

        Region sp = new Region();

        HBox.setHgrow(
                sp,
                Priority.ALWAYS
        );

        HBox header = new HBox(
                10,
                text(icon, "card-icon"),
                text(title, "card-title"),
                sp
        );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        if (link != null) {

            Label l = text(
                    link,
                    "link"
            );

            if (target != null) {

                makeClickable(
                        l,
                        target
                );
            }

            header.getChildren().add(
                    l
            );
        }

        VBox card = new VBox(
                16,
                header,
                body
        );

        card.getStyleClass().add(
                "card"
        );

        return card;
    }

    // ============================================================
    // AVATAR
    // ============================================================

    private StackPane avatar(
            String initials,
            double size
    ) {

        Circle c = new Circle(
                size / 2
        );

        c.getStyleClass().add(
                "avatar-circle"
        );

        Label l = text(
                initials,
                "avatar-text"
        );

        l.setStyle(
                "-fx-font-size: "
                        + (size * 0.34)
                        + "px;"
        );

        return new StackPane(
                c,
                l
        );
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