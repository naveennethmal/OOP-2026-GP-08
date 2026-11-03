package Technical_Officer;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

import java.io.File;
import java.util.function.Function;

public final class UiKit {

    private UiKit() {}

    // ---------- layout pieces ----------

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

    public static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, input);
    }

    // ---------- buttons and pills ----------

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

    /** tone: ok / warn / bad / info / purple / muted */
    public static Label pill(String text, String tone) {
        Label p = new Label(text);
        p.getStyleClass().addAll("pill", "pill-" + tone);
        return p;
    }

    // Maps a status text to a pill colour.
    public static String toneFor(String status) {
        return switch (status) {
            case "Eligible", "Approved", "Present" -> "ok";
            case "Eligible (medical)", "Medical" -> "info";
            case "Pending", "Medical pending" -> "warn";
            case "Rejected", "Not eligible", "Absent" -> "bad";
            default -> "muted";
        };
    }

    // ---------- tables ----------

    public static <S> TableColumn<S, String> column(String title, Function<S, String> getter) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new SimpleStringProperty(getter.apply(d.getValue())));
        return col;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> pillCells() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : pill(item, toneFor(item)));
            }
        };
    }

    // ---------- avatar ----------

    public static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public static void setAvatar(Circle circle, Label initials, String name, String picturePath) {
        initials.setText(initialsOf(name));
        circle.setFill(Color.web("#2563eb"));
        boolean hasPhoto = false;
        if (picturePath != null && !picturePath.isBlank() && new File(picturePath).isFile()) {
            double size = circle.getRadius() * 2;
            Image image = new Image(new File(picturePath).toURI().toString(), size, size, true, true);
            if (!image.isError()) {
                circle.setFill(new ImagePattern(image));
                hasPhoto = true;
            }
        }
        initials.setVisible(!hasPhoto);
    }

    // ---------- messages ----------

    public static void info(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    public static void warn(String message) {
        new Alert(Alert.AlertType.WARNING, message).show();
    }

    public static void showError(String message, Exception e) {
        System.err.println(message + " " + e.getMessage());
        new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage()).show();
    }


    public static Label errorLabel(String message, Exception e) {
        System.err.println(message + " " + e.getMessage());
        Label l = new Label(message + " " + e.getMessage());
        l.setWrapText(true);
        l.getStyleClass().add("form-error");
        return l;
    }

    public static void showLoadError(Pane page, Exception e) {
        page.getChildren().add(errorLabel("Could not load data from the database.", e));
    }
}
