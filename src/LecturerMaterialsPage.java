import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.*;
import java.util.Optional;
import java.util.Set;

public class LecturerMaterialsPage extends VBox {

    private static final Path FOLDER = Paths.get("materials");
    private static final long MAX_SIZE = 20L * 1024 * 1024;   // 20 MB
    private static final Set<String> ALLOWED = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "png", "jpg", "jpeg");

    private final Lecturer lecturer;
    private final MaterialDAO dao = new MaterialDAO();
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final TableView<Material> table = new TableView<>();
    private final TextField titleField = new TextField();
    private final TextArea descArea = new TextArea();
    private final Label fileLabel = new Label("No file chosen");
    private final Label message = new Label();
    private File chosenFile;     // new file picked in the form (null = none)
    private Material selected;   // row being edited (null = adding a new one)

    public LecturerMaterialsPage(Lecturer lecturer) {
        this.lecturer = lecturer;
        setSpacing(10);

        Label title = new Label("Course Materials");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(320);

        table.getColumns().addAll(
                this.<Material>col("Title", "title", 230),
                this.<Material>col("File", "originalName", 220),
                this.<Material>col("Uploaded", "uploadedDate", 110),
                this.<Material>col("Description", "description", 320));
        table.setPlaceholder(new Label("Select a course"));
        table.setPrefHeight(230);
        VBox.setVgrow(table, Priority.ALWAYS);

        titleField.setPromptText("Title (e.g. Week 3 - Normalization slides)");
        descArea.setPromptText("Description (optional)");
        descArea.setPrefRowCount(3);

        Button choose = new Button("Choose File...");
        choose.setOnAction(e -> chooseFile());
        HBox fileRow = new HBox(10, choose, fileLabel);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        Button add = new Button("Add Material");
        Button update = new Button("Update Selected");
        Button delete = new Button("Delete Selected");
        Button open = new Button("Open File");
        Button clear = new Button("Clear Form");
        add.setOnAction(e -> addMaterial());
        update.setOnAction(e -> updateMaterial());
        delete.setOnAction(e -> deleteMaterial());
        open.setOnAction(e -> openFile());
        clear.setOnAction(e -> clearForm());
        HBox buttons = new HBox(10, add, update, delete, open, clear);

        Label hint = new Label("Select a row in the table to edit, delete or open it.");
        hint.setStyle("-fx-text-fill: #666;");

        VBox form = new VBox(8, new Label("Title"), titleField,
                new Label("Description"), descArea, new Label("File"), fileRow, buttons, hint);
        form.setPadding(new Insets(15));
        form.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        form.setMaxWidth(720);

        getChildren().addAll(title, courseBox, table, form, message);

        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    new MarkDAO().getCourses(lecturer.getLecturerId())));
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }

        courseBox.valueProperty().addListener((o, a, c) -> { clearForm(); loadMaterials(); });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, m) -> onRowSelected(m));
    }

    private void loadMaterials() {
        CourseItem c = courseBox.getValue();
        if (c == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            table.setItems(FXCollections.observableArrayList(dao.getByCourse(c.getCode())));
            table.setPlaceholder(new Label("No materials added for this course yet"));
        } catch (Exception ex) {
            showError("Could not load materials: " + ex.getMessage());
        }
    }

    private void onRowSelected(Material m) {
        selected = m;
        if (m == null) return;
        titleField.setText(m.getTitle());
        descArea.setText(m.getDescription());
        chosenFile = null;
        fileLabel.setText("Current file: " + m.getOriginalName());
        message.setText("");
    }

    private void clearForm() {
        selected = null;
        chosenFile = null;
        titleField.clear();
        descArea.clear();
        fileLabel.setText("No file chosen");
        table.getSelectionModel().clearSelection();
    }

    private void chooseFile() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;
        chosenFile = f;
        fileLabel.setText("New file: " + f.getName());
    }

    // Returns an error message, or null when the file is acceptable
    private String fileError(File f) {
        String name = f.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        if (!ALLOWED.contains(ext)) return "File type not allowed (use pdf, docx, pptx, xlsx, txt, zip, png, jpg)";
        if (f.length() > MAX_SIZE) return "File is too large (maximum 20 MB)";
        return null;
    }

    // Copies the file into the materials folder and returns the stored name
    private String storeFile(File f, String courseCode) throws Exception {
        Files.createDirectories(FOLDER);
        String safe = f.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        String stored = courseCode + "_" + System.currentTimeMillis() + "_" + safe;
        Files.copy(f.toPath(), FOLDER.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        return stored;
    }

    private void addMaterial() {
        CourseItem c = courseBox.getValue();
        String t = titleField.getText().trim();
        if (c == null) { showError("Please select a course first"); return; }
        if (t.isEmpty()) { showError("Title cannot be empty"); return; }
        if (t.length() > 150) { showError("Title is too long (maximum 150 characters)"); return; }
        if (chosenFile == null) { showError("Please choose a file"); return; }
        String err = fileError(chosenFile);
        if (err != null) { showError(err); return; }

        try {
            String stored = storeFile(chosenFile, c.getCode());
            dao.add(new Material(0, c.getCode(), t, descArea.getText().trim(),
                    stored, chosenFile.getName(), ""), lecturer.getLecturerId());
            clearForm();
            loadMaterials();
            showOk("Material added successfully");
        } catch (Exception ex) {
            showError("Could not add material: " + ex.getMessage());
        }
    }

    private void updateMaterial() {
        if (selected == null) { showError("Select a material from the table first"); return; }
        String t = titleField.getText().trim();
        if (t.isEmpty()) { showError("Title cannot be empty"); return; }
        if (t.length() > 150) { showError("Title is too long (maximum 150 characters)"); return; }

        try {
            String stored = selected.getStoredName();
            String original = selected.getOriginalName();
            boolean replaced = false;

            if (chosenFile != null) {
                String err = fileError(chosenFile);
                if (err != null) { showError(err); return; }
                stored = storeFile(chosenFile, selected.getCourseCode());
                original = chosenFile.getName();
                replaced = true;
            }

            dao.update(new Material(selected.getId(), selected.getCourseCode(), t,
                    descArea.getText().trim(), stored, original, selected.getUploadedDate()));

            if (replaced) {   // remove the old file only after the database was updated
                Files.deleteIfExists(FOLDER.resolve(selected.getStoredName()));
            }
            clearForm();
            loadMaterials();
            showOk("Material updated successfully");
        } catch (Exception ex) {
            showError("Could not update material: " + ex.getMessage());
        }
    }

    private void deleteMaterial() {
        if (selected == null) { showError("Select a material from the table first"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getTitle() + "\"? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            dao.delete(selected.getId());
            Files.deleteIfExists(FOLDER.resolve(selected.getStoredName()));
            clearForm();
            loadMaterials();
            showOk("Material deleted");
        } catch (Exception ex) {
            showError("Could not delete material: " + ex.getMessage());
        }
    }

    private void openFile() {
        if (selected == null) { showError("Select a material from the table first"); return; }
        File f = FOLDER.resolve(selected.getStoredName()).toFile();
        if (!f.exists()) { showError("File not found on this computer: " + selected.getOriginalName()); return; }
        if (!Desktop.isDesktopSupported()) { showError("Opening files is not supported here"); return; }

        new Thread(() -> {
            try {
                Desktop.getDesktop().open(f);
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Could not open file: " + ex.getMessage()));
            }
        }).start();
    }

    private void showOk(String text) {
        message.setStyle("-fx-text-fill: green;");
        message.setText(text);
    }

    private void showError(String text) {
        message.setStyle("-fx-text-fill: red;");
        message.setText(text);
    }

    private <S> TableColumn<S, String> col(String header, String property, double width) {
        TableColumn<S, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}