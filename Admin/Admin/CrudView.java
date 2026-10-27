package Admin;

import Database.Db;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final TextField search = new TextField();
    protected Map<String, Object> selected;

    protected abstract String title();
    protected abstract String[][] columns();          // {header, sqlColumnLabel}
    protected abstract String selectSql();
    protected abstract Node form();
    protected abstract void save() throws Exception;  // throw IllegalArgumentException for validation messages
    protected void create() throws Exception { save(); }
    protected abstract void delete(Map<String, Object> row) throws Exception;
    protected String searchColumn() { return null; }
    protected String searchPrompt() { return searchColumn(); }
    protected void fill(Map<String, Object> row) {}
    protected void reset() {}

    /** Call at the end of the subclass constructor. */
    protected void build() {
        setPadding(new Insets(24));
        table.getStyleClass().add("admin-table");
        Label h = new Label(title()); h.getStyleClass().add("page-title");
        for (String[] c : columns()) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(c[0]);
            col.setCellValueFactory(d -> new SimpleStringProperty(displayValue(d.getValue().get(c[1]))));
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
        newBtn.setOnAction(e -> {
            if (selected != null) {
                clear();
                msg.setText("Enter the new record details, then click New.");
            } else {
                run(() -> { create(); ok("Created."); });
            }
        });
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
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(h, spacer);
        if (searchColumn() != null) {
            search.setPromptText(searchPrompt());
            search.setPrefWidth(325);
            search.setMinWidth(260);
            search.setMaxWidth(325);
            search.setPrefHeight(44);
            search.getStyleClass().add("admin-search");
            search.textProperty().addListener((o, oldValue, newValue) -> refresh());
            header.getChildren().add(search);
        }
        header.setMaxWidth(Double.MAX_VALUE);
        header.setPadding(new Insets(0, 0, 18, 0));
        setTop(header); setLeft(sp); setCenter(table);
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
        try {
            var rows = Db.query(selectSql());
            String term = search.getText().trim().toLowerCase();
            if (!term.isEmpty() && searchColumn() != null) {
                rows.removeIf(row -> !displayValue(row.get(searchColumn())).toLowerCase().contains(term));
            }
            table.setItems(FXCollections.observableArrayList(rows));
        }
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

    private static String displayValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
