package Student;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

public final class Ui {
    private Ui() {}

    public static Label text(String v, String... styles) {
        Label l = new Label(v);
        l.getStyleClass().addAll(styles);
        return l;
    }

    public static Label tag(String v, String color) {
        return text(v, "tag", "tag-" + color);
    }

    public static VBox header(String title, String sub) {
        return new VBox(4, text(title, "page-title"), text(sub, "page-sub"));
    }

    public static VBox card(String title, Node body) {
        VBox c = new VBox(16, text(title, "card-title"), body);
        c.getStyleClass().add("card");
        return c;
    }

    /** items: {title, value, color}  colors: blue, green, red, purple, yellow */
    public static HBox stats(String[][] items) {
        HBox row = new HBox(16);
        for (String[] s : items) {
            VBox b = new VBox(6, text(s[0], "stat-title"), text(s[1], "stat-value"));
            b.getStyleClass().addAll("stat-card", "card-" + s[2]);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            row.getChildren().add(b);
        }
        return row;
    }

    public static GridPane table(String[] head, Object[][] rows) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(6);
        for (int c = 0; c < head.length; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / head.length);
            g.getColumnConstraints().add(cc);
            Label h = text(head[c], "table-head");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setWrapText(true);
            g.add(h, c, 0);
        }
        for (int r = 0; r < rows.length; r++)
            for (int c = 0; c < head.length; c++) {
                Object o = rows[r][c];
                if (o instanceof Node) {
                    g.add((Node) o, c, r + 1);
                } else {
                    Label cell = text(String.valueOf(o), "table-cell");
                    cell.setWrapText(true);
                    cell.setMaxWidth(Double.MAX_VALUE);
                    g.add(cell, c, r + 1);
                }
            }
        return g;
    }

    public static StackPane avatar(String initials, double size) {
        Circle c = new Circle(size / 2);
        c.getStyleClass().add("avatar-circle");
        Label l = text(initials, "avatar-text");
        return new StackPane(c, l);
    }
}