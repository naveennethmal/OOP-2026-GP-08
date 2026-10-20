import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class LecturerStudentsPage extends VBox {

    private final TableView<Undergraduate> table = new TableView<>();
    private FilteredList<Undergraduate> filtered;
    private final TextField search = new TextField();
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final Label count = new Label();

    public LecturerStudentsPage() {
        setSpacing(15);

        Label title = new Label("Student Details");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        search.setPromptText("Search by name or reg no...");
        search.setPrefWidth(300);
        batchBox.setPromptText("All batches");

        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            search.clear();
            batchBox.setValue(null);
        });

        HBox filters = new HBox(10, search, batchBox, clear, count);
        filters.setPadding(new Insets(0, 0, 5, 0));

        table.getColumns().addAll(
                col("Reg No", "regNo", 110),
                col("Name", "fullName", 190),
                col("Batch", "batch", 70),
                col("Status", "status", 90),
                col("Email", "email", 230),
                col("Phone", "phone", 110),
                col("Department", "department", 200));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No students found"));
        VBox.setVgrow(table, Priority.ALWAYS);

        try {
            UndergraduateDAO dao = new UndergraduateDAO();
            filtered = new FilteredList<>(
                    FXCollections.observableArrayList(dao.getAll()), s -> true);
            table.setItems(filtered);
            batchBox.getItems().addAll(dao.getBatches());

            search.textProperty().addListener((o, a, b) -> applyFilter());
            batchBox.valueProperty().addListener((o, a, b) -> applyFilter());
            applyFilter();
        } catch (Exception ex) {
            count.setStyle("-fx-text-fill: red;");
            count.setText("Could not load students: " + ex.getMessage());
        }

        getChildren().addAll(title, filters, table);
    }

    private void applyFilter() {
        String text = search.getText() == null ? "" : search.getText().toLowerCase().trim();
        String batch = batchBox.getValue();
        filtered.setPredicate(s ->
                (text.isEmpty()
                        || s.getFullName().toLowerCase().contains(text)
                        || s.getRegNo().toLowerCase().contains(text))
                        && (batch == null || s.getBatch().equals(batch)));
        count.setText(filtered.size() + " students");
    }

    private TableColumn<Undergraduate, String> col(String header, String property, double width) {
        TableColumn<Undergraduate, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}