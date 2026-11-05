package Technical_Officer;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;

import java.sql.SQLException;


public final class UiKit {

    private UiKit() {}

    static Label icon(String name) {
        Label icon = new Label();
        icon.setUserData(name);
        icon.getStyleClass().add("icon");
        return icon;
    }

    public static HBox pageHeader(String title, String subtitle, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(12, new VBox(2, t, s), spacer);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(actions);
        return box;
    }

    public static VBox card(Node... children) {
        VBox c = new VBox(14, children);
        c.getStyleClass().add("card");
        return c;
    }

    public static Label cardTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-title");
        return l;
    }

    /** tone: blue, green, amber, red */
    public static VBox statCard(String label, String value, String icon, String tone) {
        StackPane badge = new StackPane(icon(icon));
        badge.getStyleClass().addAll("stat-icon", "tone-" + tone);
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        HBox row = new HBox(14, badge, new VBox(2, v, l));
        row.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(row);
        card.getStyleClass().add("card");
        return card;
    }

    public static HBox statRow(Node... cards) {
        HBox row = new HBox(16, cards);
        for (Node c : cards) {
            HBox.setHgrow(c, Priority.ALWAYS);
            if (c instanceof Region r) {
                r.setPrefWidth(10);
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    public static Button primaryButton(String text, String icon) {
        Button b = new Button(text, icon(icon));
        b.getStyleClass().add("btn-primary");
        return b;
    }

    public static Button ghostButton(String text, String icon) {
        Button b = new Button(text, icon(icon));
        b.getStyleClass().add("btn-ghost");
        return b;
    }

//    tone:ok/warm/bad/purple
    public static Label pill(String text, String tone) {
        Label p = new Label(text);
        p.getStyleClass().addAll("pill", "pill-" + tone);
        return p;
    }

    public static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, input);
    }

    //Maps a status text to a pill colour.
    public static String toneFor(String status) {
        return switch (status) {
            case "Eligible", "Approved", "Present" -> "ok";
            case "Eligible (medical)", "Medical" -> "info";
            case "Pending", "Medical pending" -> "warn";
            case "Rejected", "Not eligible", "Absent" -> "bad";
            default -> "muted";
        };
    }

    // Table cell factory that shows the cell text as a coloured pill.
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> pillCells() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : pill(item, toneFor(item)));
            }
        };
    }

    /** Red message label for a database error (used when a page cannot load its data). */
    public static Label errorLabel(String message, SQLException e) {
        System.err.println(message + " " + e.getMessage());
        Label l = new Label(message + " " + e.getMessage());
        l.setWrapText(true);
        l.getStyleClass().add("form-error");
        return l;
    }

    /** Adds the database error message to a page. */
    public static void showLoadError(Pane page, SQLException e) {
        page.getChildren().add(errorLabel("Could not load data from the database.", e));
    }

    /** Pop-up for a database error caused by a button click. */
    public static void showError(String message, SQLException e) {
        System.err.println(message + " " + e.getMessage());
        new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage()).show();
    }
}
