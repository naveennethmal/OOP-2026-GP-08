
package Admin;

import Database.Db;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Consumer;

public class DashboardView extends VBox {

    private final Consumer<String> onNavigate;
    private final Label usersCount = new Label("Loading...");
    private final Label coursesCount = new Label("Loading...");
    private final Label noticesCount = new Label("Loading...");
    private final Label timetableCount = new Label("Loading...");
    private final Label undergraduatesCount = new Label("Loading...");
    private final Label lecturersCount = new Label("Loading...");
    private final Label officersCount = new Label("Loading...");
    private final Label adminsCount = new Label("Loading...");

    private final Label manageUsersCount = new Label("Loading...");
    private final Label manageCoursesCount = new Label("Loading...");
    private final Label manageNoticesCount = new Label("Loading...");
    private final Label manageTimetablesCount = new Label("Loading...");

    private final Map<String, VBox> recentContainers = new HashMap<>();

    public DashboardView(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setPadding(new Insets(25));
        setSpacing(20);
        setStyle("-fx-background-color:#F4F7FC;");

        Label heading = title("Dashboard", 28);
        Label subtitle = new Label("Welcome to TECLMS Admin Portal");
        subtitle.setStyle("-fx-text-fill:#64748B;");

        Button refresh = button("↻ Refresh Data", "#2563EB");
        refresh.setOnAction(e -> loadDashboardData());

        VBox headingBox = new VBox(6, heading, subtitle);
        HBox header = new HBox(15, headingBox, refresh);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);

        GridPane stats = new GridPane();
        stats.setHgap(15);
        stats.setVgap(15);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            c.setHgrow(Priority.ALWAYS);
            stats.getColumnConstraints().add(c);
        }

        stats.add(createStatCard("👥", "Total Users", usersCount, "#E8F0FF", "#2563EB"), 0, 0);
        stats.add(createStatCard("📚", "Total Courses", coursesCount, "#E3F9ED", "#059669"), 1, 0);
        stats.add(createStatCard("🔔", "Total Notices", noticesCount, "#FFF2DE", "#D97706"), 2, 0);
        stats.add(createStatCard("📅", "Timetables", timetableCount, "#F0E8FF", "#7C3AED"), 3, 0);
        stats.add(createStatCard("🎓", "Undergraduates", undergraduatesCount, "#E8F0FF", "#2563EB"), 0, 1);
        stats.add(createStatCard("👨‍🏫", "Lecturers", lecturersCount, "#E3F9ED", "#059669"), 1, 1);
        stats.add(createStatCard("🛠", "Technical Officers", officersCount, "#FFF2DE", "#D97706"), 2, 1);
        stats.add(createStatCard("🛡", "Admins", adminsCount, "#F0E8FF", "#7C3AED"), 3, 1);

        Label quickTitle = title("Quick Access", 19);
        Label quickSubtitle = new Label("Access modules and view recent database records");
        quickSubtitle.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");

        GridPane quickGrid = new GridPane();
        quickGrid.setHgap(15);
        quickGrid.setVgap(15);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            c.setHgrow(Priority.ALWAYS);
            quickGrid.getColumnConstraints().add(c);
        }

        quickGrid.add(createQuickCard("👥", "Manage Users", "Registered system users",
                "Users", usersCount, manageUsersCount, "#2563EB"), 0, 0);
        quickGrid.add(createQuickCard("📘", "Manage Courses", "Available learning courses",
                "Courses", coursesCount, manageCoursesCount, "#059669"), 1, 0);
        quickGrid.add(createQuickCard("🔔", "Manage Notices", "Published system notices",
                "Notices", noticesCount, manageNoticesCount, "#D97706"), 0, 1);
        quickGrid.add(createQuickCard("📅", "Manage Timetables", "Class schedule records",
                "Timetables", timetableCount, manageTimetablesCount, "#7C3AED"), 1, 1);

        VBox overview = new VBox(16);
        overview.setPadding(new Insets(20));
        overview.setStyle(cardStyle());
        overview.getChildren().addAll(
                createOverviewRow("👥", "User Management", "Manage registered users"),
                createOverviewRow("📚", "Course Management", "Organize learning courses"),
                createOverviewRow("🔔", "Notice Board", "Share announcements with users"),
                createOverviewRow("📅", "Timetable Management", "Organize class schedules")
        );

        VBox content = new VBox(22, header, stats,
                new VBox(8, quickTitle, quickSubtitle, quickGrid),
                title("System Overview", 19), overview);
        content.setPadding(new Insets(0, 0, 15, 0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:#F4F7FC;-fx-background-color:#F4F7FC;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        loadDashboardData();
    }

    private String cardStyle() {
        return "-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E2E8F0;-fx-border-radius:14;";
    }

    private Label title(String text, int size) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size:" + size +
                "px;-fx-font-weight:bold;-fx-text-fill:#172554;");
        return label;
    }

    private Button button(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color +
                ";-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:10 15;");
        return b;
    }

    private Node createStatCard(String icon, String text, Label count,
                                String bg, String accent) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:22px;-fx-background-color:" + bg +
                ";-fx-background-radius:10;-fx-padding:10;");

        Label name = new Label(text);
        name.setWrapText(true);
        name.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        count.setStyle("-fx-font-size:27px;-fx-font-weight:bold;-fx-text-fill:" + accent + ";");

        Label description = new Label("Database records");
        description.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");

        HBox top = new HBox(10, iconLabel, name);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(15, top, count, description);
        card.setPadding(new Insets(17));
        card.setMinHeight(145);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(cardStyle());
        return card;
    }

    private Node createQuickCard(String icon, String title, String description,
                                 String page, Label sourceCount, Label quickCount,
                                 String accent) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:25px;-fx-text-fill:" + accent + ";");

        Label name = title(title, 15);
        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");

        quickCount.setText(sourceCount.getText());
        quickCount.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + accent + ";");

        HBox countRow = new HBox(5, new Label("Total records:"), quickCount);
        countRow.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(7, name, desc, countRow);
        HBox top = new HBox(12, iconLabel, details);
        top.setAlignment(Pos.CENTER_LEFT);

        Label recentTitle = new Label("Recent Records");
        recentTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#334155;");

        VBox recent = new VBox(8);
        recentContainers.put(page, recent);

        Button viewAll = new Button("View all " + title.replace("Manage ", "") + " →");
        viewAll.setMaxWidth(Double.MAX_VALUE);
        viewAll.setAlignment(Pos.CENTER_LEFT);
        viewAll.setStyle("-fx-background-color:transparent;-fx-text-fill:" + accent +
                ";-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;");
        viewAll.setOnAction(e -> navigateTo(page));

        VBox card = new VBox(12, top, recentTitle, recent, new Separator(), viewAll);
        card.setPadding(new Insets(18));
        card.setMinHeight(260);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(cardStyle());
        return card;
    }

    private String getTableName(String page) {
        switch (page) {
            case "Users": return "users";
            case "Courses": return "courses";
            case "Notices": return "notices";
            case "Timetables": return "time_table";
            default: throw new IllegalArgumentException("Unknown page: " + page);
        }
    }

    private List<Map<String, Object>> loadRecentRecords(String table) {
        if (!table.matches("users|courses|notices|time_table"))
            return Collections.emptyList();

        try {
            return Db.query("SELECT * FROM " + table + " ORDER BY id DESC LIMIT 5");
        } catch (Exception e) {
            try {
                return Db.query("SELECT * FROM " + table + " LIMIT 5");
            } catch (Exception ex) {
                ex.printStackTrace();
                return Collections.emptyList();
            }
        }
    }

    private void updateRecentRecords(String page) {
        VBox container = recentContainers.get(page);
        if (container == null) return;

        container.getChildren().clear();
        List<Map<String, Object>> records = loadRecentRecords(getTableName(page));

        if (records.isEmpty()) {
            Label empty = new Label("No records available");
            empty.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");
            container.getChildren().add(empty);
            return;
        }

        int index = 1;
        for (Map<String, Object> record : records) {
            Label row = new Label(index++ + ". " + formatRecord(record, page));
            row.setWrapText(true);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;-fx-padding:3 0;");
            container.getChildren().add(row);
        }
    }

    private String formatRecord(Map<String, Object> record, String page) {
        String[] columns;
        switch (page) {
            case "Users": columns = new String[]{"name", "full_name", "username", "user_name", "email"}; break;
            case "Courses": columns = new String[]{"course_name", "name", "title", "course_code"}; break;
            case "Notices": columns = new String[]{"title", "notice_title", "subject", "message"}; break;
            case "Timetables": columns = new String[]{"course_name", "day", "date", "start_time", "time", "title"}; break;
            default: columns = new String[0];
        }

        StringBuilder result = new StringBuilder();
        for (String column : columns) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                Object value = entry.getValue();
                if (entry.getKey().equalsIgnoreCase(column) && value != null &&
                        !value.toString().trim().isEmpty()) {
                    if (result.length() > 0) result.append(" | ");
                    result.append(value.toString().trim());
                    break;
                }
            }
            if (result.length() >= 65) break;
        }

        if (result.length() == 0) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String key = entry.getKey().toLowerCase();
                Object value = entry.getValue();
                if (key.contains("password") || key.contains("token") ||
                        key.contains("secret") || value == null) continue;
                if (!value.toString().trim().isEmpty()) {
                    if (result.length() > 0) result.append(" | ");
                    result.append(value);
                    if (result.length() >= 65) break;
                }
            }
        }

        String text = result.toString().trim();
        if (text.isEmpty()) return "Record available";
        return text.length() > 75 ? text.substring(0, 72) + "..." : text;
    }

    private void navigateTo(String page) {
        if (onNavigate != null) onNavigate.accept(page);
    }

    private Node createOverviewRow(String icon, String title, String description) {
        Label iconLabel = new Label(icon);
        iconLabel.setMinWidth(40);
        iconLabel.setStyle("-fx-font-size:21px;");

        Label name = title(title, 13);
        Label desc = new Label(description);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");

        HBox row = new HBox(12, iconLabel, new VBox(5, name, desc));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String getCount(String table) {
        if (!table.matches("users|courses|notices|time_table|undergraduates|lecturers|technical_officers"))
            return "N/A";

        try {
            List<Map<String, Object>> rows =
                    Db.query("SELECT COUNT(*) AS total FROM " + table);
            return rows.isEmpty() ? "0" : String.valueOf(rows.get(0).get("total"));
        } catch (Exception e) {
            System.err.println("Failed to load count for " + table);
            e.printStackTrace();
            return "N/A";
        }
    }

    private String getAdminCount() {
        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT COUNT(*) AS total FROM users WHERE LOWER(role) = 'admin'");
            return rows.isEmpty() ? "0" : String.valueOf(rows.get(0).get("total"));
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private void loadDashboardData() {
        usersCount.setText(getCount("users"));
        coursesCount.setText(getCount("courses"));
        noticesCount.setText(getCount("notices"));
        timetableCount.setText(getCount("time_table"));
        undergraduatesCount.setText(getCount("undergraduates"));
        lecturersCount.setText(getCount("lecturers"));
        officersCount.setText(getCount("technical_officers"));
        adminsCount.setText(getAdminCount());

        manageUsersCount.setText(usersCount.getText());
        manageCoursesCount.setText(coursesCount.getText());
        manageNoticesCount.setText(noticesCount.getText());
        manageTimetablesCount.setText(timetableCount.getText());

        updateRecentRecords("Users");
        updateRecentRecords("Courses");
        updateRecentRecords("Notices");
        updateRecentRecords("Timetables");
    }
}
