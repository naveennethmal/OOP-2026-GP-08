package Lecturer;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.File;
import java.util.function.Function;

/** Small helpers so every lecturer screen looks like the rest of the project (uses Resources/style.css). */
final class LecturerUi {
    private LecturerUi() {}

    // ------------------------------------------------------------- layout

    static VBox page(Node... children) {
        VBox box = new VBox(18, children);
        box.getStyleClass().add("page");
        return box;
    }

    static HBox header(String title, String subtitle, Node... actions) {
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

    static VBox card(Node... children) {
        VBox c = new VBox(14, children);
        c.getStyleClass().add("card");
        return c;
    }

    static Label cardTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-title");
        return l;
    }

    static Label hint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hint");
        l.setWrapText(true);
        return l;
    }

    /** tone: blue, green, amber, red */
    static VBox stat(String label, String value, String emoji, String tone) {
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 20px;");
        StackPane badge = new StackPane(icon);
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

    static HBox statRow(Node... cards) {
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

    static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, input);
    }

    static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        return b;
    }

    static Button ghost(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-ghost");
        return b;
    }

    // ----------------------------------------------------------- messages

    /** Shows a green (ok) or red (error) message under a form. */
    static void say(Label label, String text, boolean ok) {
        label.setWrapText(true);
        label.getStyleClass().remove("form-error");
        if (ok) {
            label.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            label.setStyle("");
            label.getStyleClass().add("form-error");
        }
        label.setText(text);
    }

    static Label errorLabel(String message, Exception e) {
        System.err.println(message + " " + e.getMessage());
        Label l = new Label(message + " " + e.getMessage());
        l.setWrapText(true);
        l.getStyleClass().add("form-error");
        return l;
    }

    // -------------------------------------------------------------- pills

    static String tone(String status) {
        return switch (status) {
            case "Eligible", "Approved", "Present", "Yes", "Active" -> "ok";
            case "Pending", "Late" -> "warn";
            case "Not Eligible", "Rejected", "Absent", "No", "Suspended" -> "bad";
            default -> "muted";
        };
    }

    static Label pill(String text) {
        Label p = new Label(text);
        p.getStyleClass().addAll("pill", "pill-" + tone(text));
        return p;
    }

    // ------------------------------------------------------------- tables

    static <T> TableView<T> table(String placeholder, double height) {
        TableView<T> t = new TableView<>();
        t.getStyleClass().add("lecturer-table");
        t.setPlaceholder(new Label(placeholder));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPrefHeight(height);
        t.setMinHeight(height);
        return t;
    }

    static <T> TableColumn<T, String> col(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(value.apply(d.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    /** Same as col(), but the text is shown as a coloured pill (Eligible, Pending, ...). */
    static <T> TableColumn<T, String> pillCol(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = col(title, value, width);
        c.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null || item.isEmpty() ? null : pill(item));
            }
        });
        return c;
    }

    /** A drop-down that filters a table. "All..." (or nothing selected) means no filtering. */
    record Filter<T>(ComboBox<String> box, Function<T, String> value) {}

    /**
     * Search box + drop-down filters for a table. Give the returned list to the table and reload
     * the screen with data.setAll(...) on the list you passed in.
     */
    @SafeVarargs
    static <T> FilteredList<T> searchable(ObservableList<T> data, TextField search,
                                          Function<T, String> text, Filter<T>... filters) {
        FilteredList<T> shown = new FilteredList<>(data, x -> true);
        Runnable apply = () -> {
            String t = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            shown.setPredicate(x -> {
                if (!t.isEmpty() && !text.apply(x).toLowerCase().contains(t)) return false;
                for (Filter<T> f : filters) {
                    String v = f.box().getValue();
                    if (v != null && !v.startsWith("All") && !f.value().apply(x).equals(v)) return false;
                }
                return true;
            });
        };
        search.textProperty().addListener((o, a, b) -> apply.run());
        for (Filter<T> f : filters) f.box().valueProperty().addListener((o, a, b) -> apply.run());
        return shown;
    }

    static TextField searchField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefWidth(260);
        return f;
    }

    static ComboBox<String> statusBox(String... values) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.setValue(values[0]);
        box.setMinWidth(220);
        box.setPrefWidth(240);
        box.setMaxWidth(280);
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setStyle("-fx-text-fill: #1f2937; -fx-font-size: 14px; -fx-padding: 0 10 0 10;");
            }
        });
        return box;
    }

    // ------------------------------------------------------------- avatar

    /** Shows the profile picture inside the circle, or the initials when there is no picture. */
    static void paintAvatar(Circle circle, Label initialsLabel, String picture, String initials) {
        initialsLabel.setText(initials);
        if (picture != null && !picture.isBlank()) {
            File file = new File(picture);
            if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), picture);
            if (file.isFile()) {
                double size = circle.getRadius() * 2;
                Image image = new Image(file.toURI().toString(), size, size, true, true);
                if (!image.isError()) {
                    circle.setFill(new ImagePattern(image));
                    initialsLabel.setVisible(false);
                    return;
                }
            }
        }
        circle.setFill(Color.web("#2563eb"));
        initialsLabel.setVisible(true);
    }
}
