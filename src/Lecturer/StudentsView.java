package Lecturer;

import Lecturer.LecturerDAO.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import static Lecturer.LecturerUi.*;

public class StudentsView extends VBox {

    public StudentsView() {
        getStyleClass().add("page");
        setSpacing(18);
        getChildren().add(header("Student details", "Undergraduate records (read only)"));

        try {
            ObservableList<Student> data = FXCollections.observableArrayList(LecturerDAO.students());

            TextField search = searchField("Search by name or reg no...");
            search.setPrefWidth(260);

            ComboBox<String> batch = new ComboBox<>();
            batch.getItems().add("All batches");
            batch.getItems().addAll(LecturerDAO.batches());
            batch.setValue("All batches");
            batch.setMinWidth(220);
            batch.setPrefWidth(220);
            batch.setMaxWidth(280);

            ComboBox<String> status = statusBox("All", "Active", "Inactive", "Graduated", "Suspended");
            status.setValue("All");
            status.setMinWidth(220);
            status.setPrefWidth(220);
            status.setMaxWidth(280);

            FilteredList<Student> shown = new FilteredList<>(data, s -> true);

            Runnable updateFilter = () -> {
                String searchText = search.getText() == null ? "" : search.getText().toLowerCase().trim();
                String batchVal = batch.getValue();
                String statusVal = status.getValue();

                shown.setPredicate(s -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            s.name().toLowerCase().contains(searchText) ||
                            s.regNo().toLowerCase().contains(searchText);

                    boolean matchesBatch = batchVal == null || batchVal.equals("All batches") ||
                            s.batch().equalsIgnoreCase(batchVal);

                    boolean matchesStatus = statusVal == null || statusVal.equals("All") ||
                            s.status().equalsIgnoreCase(statusVal);

                    return matchesSearch && matchesBatch && matchesStatus;
                });
            };

            search.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
            batch.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
            status.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

            TableView<Student> table = table("No students found", 480);
            table.getColumns().add(col("Reg No", Student::regNo, 100));
            table.getColumns().add(col("Name", Student::name, 190));
            table.getColumns().add(col("Batch", Student::batch, 70));
            table.getColumns().add(col("Level", Student::level, 60));
            table.getColumns().add(col("Sem", Student::semester, 50));
            table.getColumns().add(pillCol("Status", Student::status, 90));
            table.getColumns().add(col("Email", Student::email, 220));
            table.getColumns().add(col("Phone", Student::phone, 110));
            table.getColumns().add(col("Address", Student::address, 160));
            table.setItems(shown);

            VBox searchBox = new VBox(4, new Label("Search"), search);
            VBox batchBox = new VBox(4, new Label("Batch"), batch);
            VBox statusBox = new VBox(4, new Label("Status"), status);

            HBox filters = new HBox(15, searchBox, batchBox, statusBox);
            filters.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

            getChildren().addAll(filters, card(table));
        } catch (Exception e) {
            getChildren().add(errorLabel("Could not load students.", e));
        }
    }
}