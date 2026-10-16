package ftms;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/** Abstract base: every admin page = form card + table. Subclasses supply the specifics (abstraction, inheritance, polymorphism). */
public abstract class CrudView extends BorderPane {
    public record Item(String key, String label) { @Override public String toString() { return label; } }
    @FunctionalInterface protected interface Task { void run() throws Exception; }

    protected final TableView<Map<String, Object>> table = new TableView<>();
    protected final Label msg = new Label();
    protected Map<String, Object> selected;

    protected abstract String title();
    protected abstract String[][] columns();          // {header, sqlColumnLabel}
    protected abstract String selectSql();
    protected abstract Node form();
    protected abstract void save() throws Exception;  // throw IllegalArgumentException for validation messages
    protected abstract void delete(Map<String, Object> row) throws Exception;
    protected void fill(Map<String, Object> row) {}
    protected void reset() {}

    /** Call at the end of the subclass constructor. */
    protected void build() {
        setPadding(new Insets(24));
        Label h = new Label(title()); h.getStyleClass().add("page-title");
        for (String[] c : columns()) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(c[0]);
            col.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get(c[1]))));
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, row) -> {
            if (row != null) { selected = row; msg.setText(""); fill(row); }
        });

        Button saveBtn = new Button("Save"); saveBtn.getStyleClass().add("primary");
        Button newBtn = new Button("New"); newBtn.getStyleClass().add("ghost");
        Button delBtn = new Button("Delete"); delBtn.getStyleClass().add("danger");
        saveBtn.setOnAction(e -> run(() -> { save(); ok("Saved."); }));
        newBtn.setOnAction(e -> { clear(); msg.setText(""); });
        delBtn.setOnAction(e -> {
            if (selected == null) { fail("Select a row to delete."); return; }
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete the selected record?", ButtonType.YES, ButtonType.NO);
            if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) run(() -> { delete(selected); ok("Deleted."); });
        });

        VBox card = new VBox(12, form(), new HBox(8, saveBtn, newBtn, delBtn), msg);
        card.getStyleClass().add("card");
        msg.setWrapText(true);
        ScrollPane sp = new ScrollPane(card); sp.setFitToWidth(true); sp.setStyle("-fx-background-color:transparent;");
        sp.setPrefWidth(360);
        setTop(h); setLeft(sp); setCenter(table);
        BorderPane.setMargin(table, new Insets(0, 0, 0, 18));
        refresh();
    }

    protected void run(Task t) {
        try { t.run(); refresh(); clear(); }
        catch (IllegalArgumentException ex) { fail(ex.getMessage()); }
        catch (Exception ex) { fail("Database error: " + ex.getMessage()); }
    }

    protected void clear() { selected = null; table.getSelectionModel().clearSelection(); reset(); }

    protected void refresh() {
        try { table.setItems(FXCollections.observableArrayList(Db.query(selectSql()))); }
        catch (Exception e) { fail("Cannot load data: " + e.getMessage()); }
    }

    protected void ok(String s) { msg.getStyleClass().setAll("label", "ok"); msg.setText(s); }
    protected void fail(String s) { msg.getStyleClass().setAll("label", "err"); msg.setText(s); }

    protected static VBox field(String label, Node n) {
        Label l = new Label(label); l.getStyleClass().add("field-label");
        return new VBox(4, l, n);
    }

    /** Query must return columns k (key) and l (label). */
    protected static ObservableList<Item> items(String sql, Object... p) {
        ObservableList<Item> out = FXCollections.observableArrayList();
        try { for (Map<String, Object> r : Db.query(sql, p)) out.add(new Item(r.get("k").toString(), r.get("l").toString())); }
        catch (Exception e) { System.err.println("items(): " + e.getMessage()); }
        return out;
    }

    protected static void pick(ComboBox<Item> cb, Object key) {
        for (Item i : cb.getItems()) if (i.key().equals(String.valueOf(key))) { cb.setValue(i); return; }
        cb.setValue(null);
    }

    protected static String need(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " is required.");
        return v.trim();
    }
}
