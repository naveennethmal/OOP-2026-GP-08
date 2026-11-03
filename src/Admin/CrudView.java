
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
import javafx.scene.paint.Color;

import java.util.*;

public abstract class CrudView extends BorderPane {

    public record Item(String key, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    @FunctionalInterface
    protected interface Task {
        void run() throws Exception;
    }

    protected final TableView<Map<String, Object>> table =
            new TableView<>();

    protected final Label msg = new Label();

    protected Map<String, Object> selected;

    private final TextField search = new TextField();
    private final ComboBox<String> roleFilter = new ComboBox<>();

    private ScrollPane formPane;

    protected abstract String title();

    protected abstract String[][] columns();

    protected abstract String selectSql();

    protected abstract Node form();

    protected abstract void save() throws Exception;

    protected abstract void delete(Map<String, Object> row)
            throws Exception;

    protected void create() throws Exception {
        save();
    }

    protected String searchColumn() {
        return null;
    }

    protected String searchPrompt() {
        return "Search...";
    }

    protected String[] filterOptions() {
        return new String[0];
    }

    protected String filterColumn() {
        return null;
    }

    protected String addButtonText() {
        return "+ Add";
    }

    protected void fill(Map<String, Object> row) {
    }

    protected void reset() {
    }

    // =====================================================
    // BUILD PAGE
    // =====================================================

    protected void build() {

        setPadding(new Insets(22));
        setStyle("-fx-background-color: #F3F6FC;");

        Label heading = new Label(title());

        heading.setStyle(
                "-fx-font-size: 23px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #172554;"
        );

        // Add button
        Button addBtn = new Button(addButtonText());

        addBtn.setStyle(
                "-fx-background-color: #2563EB;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 17px;" +
                        "-fx-background-radius: 7px;" +
                        "-fx-cursor: hand;"
        );

        addBtn.setMinHeight(38);
        addBtn.setOnAction(e -> openNewForm());

        // Search field
        search.setPromptText(searchPrompt());
        search.setPrefWidth(300);
        search.setMinWidth(180);
        search.setPrefHeight(38);

        search.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-prompt-text-fill: #64748B;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 7px;" +
                        "-fx-background-radius: 7px;"
        );

        search.textProperty().addListener(
                (observable, oldValue, newValue) -> refresh()
        );

        Button clearSearchBtn = new Button("Clear");

        clearSearchBtn.setStyle(
                "-fx-background-color: #E2E8F0;" +
                        "-fx-text-fill: #1E293B;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 13px;" +
                        "-fx-background-radius: 7px;" +
                        "-fx-cursor: hand;"
        );

        clearSearchBtn.setMinHeight(38);
        clearSearchBtn.setOnAction(e -> search.clear());

        HBox searchBox = new HBox(8, search, clearSearchBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Filter dropdown
        roleFilter.setPrefHeight(38);
        roleFilter.setMinWidth(175);
        roleFilter.setMaxWidth(220);
        roleFilter.setPromptText("Filter records");

        roleFilter.setStyle(
                "-fx-background-color: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 7px;" +
                        "-fx-background-radius: 7px;"
        );

        String[] options = filterOptions();

        roleFilter.getItems().clear();

        if (filterColumn() != null && options.length > 0) {


            roleFilter.getItems().addAll(options);

            roleFilter.setValue(options[0]);

            roleFilter.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item);
                    setTextFill(Color.web("#111827"));
                }
            });

            roleFilter.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(empty || item == null ? "" : item);
                    setTextFill(Color.web("#111827"));

                    setStyle(
                            empty
                                    ? "-fx-background-color: white;"
                                    : "-fx-background-color: white;" +
                                    "-fx-padding: 8px;"
                    );
                }
            });

            roleFilter.setOnAction(e -> refresh());
        }

        HBox filters = new HBox(9);
        filters.setAlignment(Pos.CENTER_RIGHT);

        if (searchColumn() != null) {
            filters.getChildren().add(searchBox);
        }

        if (filterColumn() != null && options.length > 0) {
            filters.getChildren().add(roleFilter);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                14,
                heading,
                spacer,
                filters,
                addBtn
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));

        setTop(header);

        // =================================================
        // TABLE
        // =================================================

        table.getColumns().clear();

        table.setPlaceholder(
                new Label("No matching records found.")
        );

        table.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #DCE4F0;" +
                        "-fx-border-radius: 9px;" +
                        "-fx-background-radius: 9px;" +
                        "-fx-font-size: 12px;"
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        table.setFixedCellSize(44);

        // Table header and row styling
        table.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();

            row.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: transparent transparent #E8EEF7 transparent;"
            );

            row.hoverProperty().addListener(
                    (obs, wasHovered, isHovered) -> {
                        if (!row.isEmpty() && !row.isSelected()) {
                            row.setStyle(
                                    isHovered
                                            ? "-fx-background-color: #F1F6FF;" +
                                            "-fx-border-color: transparent transparent #E8EEF7 transparent;"
                                            : "-fx-background-color: white;" +
                                            "-fx-border-color: transparent transparent #E8EEF7 transparent;"
                            );
                        }
                    }
            );

            row.selectedProperty().addListener(
                    (obs, wasSelected, isSelected) -> {
                        if (isSelected) {
                            row.setStyle(
                                    "-fx-background-color: #DBEAFE;" +
                                            "-fx-border-color: transparent transparent #BFDBFE transparent;"
                            );
                        } else {
                            row.setStyle(
                                    "-fx-background-color: white;" +
                                            "-fx-border-color: transparent transparent #E8EEF7 transparent;"
                            );
                        }
                    }
            );

            return row;
        });

        for (String[] c : columns()) {

            TableColumn<Map<String, Object>, String> col =
                    new TableColumn<>(c[0]);

            col.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            displayValue(data.getValue().get(c[1]))
                    )
            );

            col.setMinWidth(80);
            col.setPrefWidth(120);

            col.setStyle(
                    "-fx-alignment: CENTER-LEFT;" +
                            "-fx-font-size: 12px;"
            );

            table.getColumns().add(col);
        }

        // =================================================
        // ACTION BUTTONS
        // =================================================

        TableColumn<Map<String, Object>, Void> actions =
                new TableColumn<>("Actions");

        actions.setMinWidth(185);
        actions.setPrefWidth(205);
        actions.setSortable(false);
        actions.setReorderable(false);

        actions.setCellFactory(column -> new TableCell<>() {

            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            private final HBox buttons =
                    new HBox(6, viewBtn, editBtn, deleteBtn);

            {
                buttons.setAlignment(Pos.CENTER);

                viewBtn.setStyle(
                        "-fx-background-color: #DBEAFE;" +
                                "-fx-text-fill: #1D4ED8;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 5px;" +
                                "-fx-cursor: hand;"
                );

                editBtn.setStyle(
                        "-fx-background-color: #FEF3C7;" +
                                "-fx-text-fill: #92400E;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 5px;" +
                                "-fx-cursor: hand;"
                );

                deleteBtn.setStyle(
                        "-fx-background-color: #FEE2E2;" +
                                "-fx-text-fill: #B91C1C;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 5px;" +
                                "-fx-cursor: hand;"
                );

                viewBtn.setOnAction(e -> {
                    Map<String, Object> row = getCurrentRow();
                    if (row != null) {
                        showDetails(row);
                    }
                });

                editBtn.setOnAction(e -> {
                    Map<String, Object> row = getCurrentRow();

                    if (row != null) {
                        selected = row;
                        table.getSelectionModel().select(row);
                        fill(row);
                        msg.setText("");
                        showForm(true);
                    }
                });

                deleteBtn.setOnAction(e -> {
                    Map<String, Object> row = getCurrentRow();

                    if (row == null) {
                        return;
                    }

                    Alert confirm = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete this record?",
                            ButtonType.YES,
                            ButtonType.NO
                    );

                    confirm.setTitle("Confirm Delete");
                    confirm.setHeaderText("Delete Record");

                    if (confirm.showAndWait()
                            .orElse(ButtonType.NO) == ButtonType.YES) {
                        run(() -> delete(row));
                    }
                });
            }

            private Map<String, Object> getCurrentRow() {
                int index = getIndex();

                if (index < 0 ||
                        index >= getTableView().getItems().size()) {
                    return null;
                }

                return getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        table.getColumns().add(actions);

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldRow, row) -> {
                    if (row != null) {
                        selected = row;
                        msg.setText("");
                    }
                });

        // =================================================
        // FORM CARD
        // =================================================

        VBox formCard = new VBox(12, form());
        formCard.setPadding(new Insets(18));

        formCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-color: #DCE4F0;" +
                        "-fx-border-radius: 10px;"
        );

        Button saveBtn = new Button("Save");

        saveBtn.setStyle(
                "-fx-background-color: #2563EB;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 22px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-cursor: hand;"
        );

        saveBtn.setOnAction(e -> run(() -> {
            if (selected == null) {
                create();
            } else {
                save();
            }
        }));

        Button cancelBtn = new Button("Cancel");

        cancelBtn.setStyle(
                "-fx-background-color: #E2E8F0;" +
                        "-fx-text-fill: #1E293B;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 22px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-cursor: hand;"
        );

        cancelBtn.setOnAction(e -> {
            clear();
            msg.setText("");
            showForm(false);
        });

        HBox formButtons = new HBox(8, saveBtn, cancelBtn);
        formButtons.setAlignment(Pos.CENTER_LEFT);

        formCard.getChildren().addAll(formButtons, msg);

        msg.setWrapText(true);

        formPane = new ScrollPane(formCard);
        formPane.setFitToWidth(true);
        formPane.setPrefWidth(360);
        formPane.setMinWidth(300);

        formPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );

        setCenter(table);

        showForm(false);
        refresh();
    }

    // =====================================================
    // OPEN NEW FORM
    // =====================================================

    private void openNewForm() {
        clear();
        msg.setText("");
        showForm(true);
    }

    // =====================================================
    // SHOW / HIDE FORM
    // =====================================================

    private void showForm(boolean visible) {

        if (formPane == null) {
            return;
        }

        formPane.setVisible(visible);
        formPane.setManaged(visible);

        if (visible) {
            setLeft(formPane);
            BorderPane.setMargin(table, new Insets(0, 0, 0, 18));
        } else {
            setLeft(null);
            BorderPane.setMargin(table, Insets.EMPTY);
        }
    }

    // =====================================================
    // VIEW DETAILS
    // =====================================================

    private void showDetails(Map<String, Object> row) {

        StringBuilder details = new StringBuilder();

        for (Map.Entry<String, Object> entry : row.entrySet()) {

            String key = entry.getKey();
            String lowerKey = key.toLowerCase(Locale.ROOT);

            if (lowerKey.contains("password")
                    || lowerKey.contains("token")
                    || lowerKey.contains("secret")) {
                continue;
            }

            details.append(key.replace("_", " "))
                    .append(": ")
                    .append(displayValue(entry.getValue()))
                    .append("\n");
        }

        TextArea area = new TextArea(details.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(12);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Account Details");
        dialog.setHeaderText("Record Information");
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.showAndWait();
    }

    // =====================================================
    // DATABASE OPERATIONS
    // =====================================================

    protected void run(Task task) {

        try {
            task.run();

            refresh();
            clear();
            showForm(false);

            ok("Operation completed successfully.");

        } catch (IllegalArgumentException ex) {
            fail(ex.getMessage());

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Database error: " + ex.getMessage());
        }
    }

    // =====================================================
    // CLEAR FORM
    // =====================================================

    protected void clear() {
        selected = null;
        table.getSelectionModel().clearSelection();
        reset();
    }

    // =====================================================
    // REFRESH + SEARCH + FILTER
    // =====================================================

    protected void refresh() {

        try {
            List<Map<String, Object>> rows =
                    new ArrayList<>(Db.query(selectSql()));

            // Search across all returned fields
            String term = search.getText() == null
                    ? ""
                    : search.getText().trim().toLowerCase(Locale.ROOT);

            if (!term.isEmpty()) {
                rows.removeIf(row ->
                        row.values().stream()
                                .filter(Objects::nonNull)
                                .noneMatch(value ->
                                        value.toString()
                                                .toLowerCase(Locale.ROOT)
                                                .contains(term)
                                )
                );
            }

            // Apply selected filter
            String selectedFilter = roleFilter.getValue();
            String[] options = filterOptions();

            if (filterColumn() != null
                    && selectedFilter != null
                    && options.length > 0
                    && !selectedFilter.equals(options[0])) {

                rows.removeIf(row ->
                        !selectedFilter.equalsIgnoreCase(
                                displayValue(row.get(filterColumn()))
                        )
                );
            }

            table.setItems(
                    FXCollections.observableArrayList(rows)
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Cannot load data: " + ex.getMessage());
        }
    }

    // =====================================================
    // STATUS MESSAGES
    // =====================================================

    protected void ok(String text) {
        msg.setStyle(
                "-fx-text-fill: #15803D;" +
                        "-fx-font-weight: bold;"
        );
        msg.setText(text);
    }

    protected void fail(String text) {
        msg.setStyle(
                "-fx-text-fill: #B91C1C;" +
                        "-fx-font-weight: bold;"
        );
        msg.setText(text);
    }

    // =====================================================
    // FORM FIELD HELPER
    // =====================================================

    protected static VBox field(String label, Node node) {

        Label fieldLabel = new Label(label);

        fieldLabel.setStyle(
                "-fx-text-fill: #334155;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );

        VBox box = new VBox(5, fieldLabel, node);
        box.setFillWidth(true);

        return box;
    }

    // =====================================================
    // DROPDOWN ITEMS
    // =====================================================

    protected static ObservableList<Item> items(
            String sql,
            Object... params) {

        ObservableList<Item> result =
                FXCollections.observableArrayList();

        try {
            for (Map<String, Object> row : Db.query(sql, params)) {
                result.add(new Item(
                        String.valueOf(row.get("k")),
                        String.valueOf(row.get("l"))
                ));
            }
        } catch (Exception ex) {
            System.err.println("items(): " + ex.getMessage());
        }

        return result;
    }

    protected static void pick(
            ComboBox<Item> combo,
            Object key) {

        for (Item item : combo.getItems()) {
            if (item.key().equals(String.valueOf(key))) {
                combo.setValue(item);
                return;
            }
        }

        combo.setValue(null);
    }

    // =====================================================
    // VALIDATION HELPERS
    // =====================================================

    protected static String need(String value, String name) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required."
            );
        }

        return value.trim();
    }

    protected static String value(
            Map<String, Object> row,
            String key) {

        Object result = row.get(key);
        return result == null ? "" : result.toString();
    }

    private static String displayValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
