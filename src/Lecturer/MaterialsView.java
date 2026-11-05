package Lecturer;

import Lecturer.LecturerDAO.Course;
import Lecturer.LecturerDAO.Material;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static Lecturer.LecturerUi.*;

/**
 * Add, change, delete and open the materials of the lecturer's courses.
 * The file is copied to the "materials" folder and the path is saved in course_materials.file_path,
 * which is the same column the student portal opens. Type "Link" saves a web address instead.
 */
public class MaterialsView extends VBox {

    private static final Path FOLDER = Paths.get("materials");
    private static final long MAX_SIZE = 20L * 1024 * 1024;                 // 20 MB
    private static final Set<String> ALLOWED = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "png", "jpg", "jpeg");

    private final ComboBox<Course> courseBox = new ComboBox<>();
    private final TableView<Material> table = table("Select a course", 260);
    private final TextField titleField = new TextField();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField linkField = new TextField();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker untilPicker = new DatePicker();
    private final Label fileLabel = new Label("No file chosen");
    private final Button chooseBtn = ghost("Choose file...");
    private final Label message = new Label();

    private File chosenFile;          // new file picked in the form
    private Material selected;        // row being edited (null = adding a new one)

    public MaterialsView() {
        getStyleClass().add("page");
        setSpacing(18);
        getChildren().add(header("Course materials", "Add, change or remove the materials of your courses"));

        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(360);

        table.getStyleClass().add("materials-table");
        table.getColumns().add(col("Title", Material::title, 240));
        table.getColumns().add(col("Type", Material::type, 110));
        table.getColumns().add(col("Uploaded", Material::date, 100));
        table.getColumns().add(col("From", Material::availableFrom, 100));
        table.getColumns().add(col("Until", MaterialsView::untilText, 110));
        table.getColumns().add(col("Status", MaterialsView::availability, 150));
        table.getColumns().add(col("File / link", Material::filePath, 260));

        typeBox.getItems().addAll("Lecture Notes", "Slides", "Tutorial", "Assignment", "Past Paper", "Other", "Link");
        typeBox.setValue("Lecture Notes");
        titleField.setPromptText("Title (e.g. Week 3 - Normalization slides)");
        linkField.setPromptText("https://...");

        // Only the calendar is used (so no typing mistakes); "Clear dates" empties both pickers.
        fromPicker.setEditable(false);
        untilPicker.setEditable(false);
        fromPicker.setPromptText("Optional");
        untilPicker.setPromptText("Optional");
        untilPicker.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));     // an end date cannot be in the past
            }
        });
        Button clearDates = ghost("Clear dates");
        clearDates.setOnAction(e -> { fromPicker.setValue(null); untilPicker.setValue(null); });
        HBox dateRow = new HBox(14, field("Available from", fromPicker),
                field("Available until (removed after this day)", untilPicker), clearDates);
        dateRow.setAlignment(Pos.BOTTOM_LEFT);

        HBox fileRow = new HBox(10, chooseBtn, fileLabel);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        VBox fileBox = field("File", fileRow);
        VBox linkBox = field("Web link", linkField);
        typeBox.valueProperty().addListener((o, a, b) -> showFileOrLink(fileBox, linkBox));
        showFileOrLink(fileBox, linkBox);

        chooseBtn.setOnAction(e -> chooseFile());

        Button add = primary("Add material");
        Button update = ghost("Update selected");
        Button delete = ghost("Delete selected");
        Button open = ghost("Open");
        Button clear = ghost("Clear form");
        add.setOnAction(e -> save(false));
        update.setOnAction(e -> save(true));
        delete.setOnAction(e -> delete());
        open.setOnAction(e -> open());
        clear.setOnAction(e -> clearForm());

        VBox form = card(cardTitle("Material"), field("Title", titleField), field("Type", typeBox),
                fileBox, linkBox, dateRow,
                hint("No dates: always available. Only a from date: available from that day and never removed. "
                        + "Only an end date: available from today and removed automatically after the end date."),
                new HBox(10, add, update, delete, open, clear),
                hint("Select a row in the table to change, delete or open it."), message);
        form.setMaxWidth(760);

        getChildren().addAll(field("Course", courseBox), card(table), form);

        try {
            courseBox.setItems(FXCollections.observableArrayList(LecturerDAO.courses()));
        } catch (Exception e) {
            say(message, "Could not load your courses: " + e.getMessage(), false);
        }
        courseBox.valueProperty().addListener((o, a, c) -> { clearForm(); load(); });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, m) -> pick(m));
    }

    private void showFileOrLink(VBox fileBox, VBox linkBox) {
        boolean link = "Link".equals(typeBox.getValue());
        fileBox.setVisible(!link);
        fileBox.setManaged(!link);
        linkBox.setVisible(link);
        linkBox.setManaged(link);
    }

    private void load() {
        Course c = courseBox.getValue();
        if (c == null) {
            table.getItems().clear();
            return;
        }
        try {
            int removed = LecturerDAO.purgeExpiredMaterials();      // materials past their end date go first
            table.setItems(FXCollections.observableArrayList(LecturerDAO.materials(c.code())));
            table.setPlaceholder(new Label("No materials added for this course yet"));
            if (removed > 0) say(message, removed + " expired material(s) were removed automatically.", true);
        } catch (Exception e) {
            say(message, "Could not load materials: " + e.getMessage(), false);
        }
    }

    private void pick(Material m) {
        selected = m;
        if (m == null) return;
        titleField.setText(m.title());
        typeBox.setValue(m.type());
        chosenFile = null;
        if (m.type().equals("Link")) linkField.setText(m.filePath());
        fromPicker.setValue(m.availableFrom().isEmpty() ? null : LocalDate.parse(m.availableFrom()));
        untilPicker.setValue(m.availableUntil().isEmpty() ? null : LocalDate.parse(m.availableUntil()));
        fileLabel.setText("Current: " + m.filePath());
        message.setText("");
    }

    private void clearForm() {
        selected = null;
        chosenFile = null;
        titleField.clear();
        linkField.clear();
        typeBox.setValue("Lecture Notes");
        fromPicker.setValue(null);
        untilPicker.setValue(null);
        fileLabel.setText("No file chosen");
        table.getSelectionModel().clearSelection();
    }

    private void chooseFile() {
        File f = new FileChooser().showOpenDialog(getScene().getWindow());
        if (f == null) return;
        chosenFile = f;
        fileLabel.setText("New file: " + f.getName());
    }

    /** Error text, or null when the file is fine. */
    private String fileError(File f) {
        String name = f.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!ALLOWED.contains(ext)) return "File type not allowed (use pdf, docx, pptx, xlsx, txt, zip, png or jpg).";
        if (f.length() > MAX_SIZE) return "File is too large (maximum 20 MB).";
        return null;
    }

    /** Copies the file into the materials folder and returns the path to save in the database. */
    private String store(File f, String courseCode) throws Exception {
        Files.createDirectories(FOLDER);
        String safe = f.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        String name = courseCode + "_" + System.currentTimeMillis() + "_" + safe;
        Files.copy(f.toPath(), FOLDER.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return "materials/" + name;
    }

    private void save(boolean update) {
        Course c = courseBox.getValue();
        String title = titleField.getText().trim();
        String type = typeBox.getValue();
        boolean link = "Link".equals(type);

        if (update && selected == null) { say(message, "Select a material from the table first.", false); return; }
        if (c == null) { say(message, "Please select a course first.", false); return; }
        if (title.isEmpty()) { say(message, "Title cannot be empty.", false); return; }
        if (title.length() > 150) { say(message, "Title is too long (maximum 150 characters).", false); return; }

        LocalDate from = fromPicker.getValue();
        LocalDate until = untilPicker.getValue();
        try {
            LecturerDAO.validateMaterialDates(from, until);          // before any file is copied
        } catch (IllegalArgumentException e) {
            say(message, e.getMessage(), false);
            return;
        }

        try {
            String path;
            boolean newFile = false;
            if (link) {
                path = linkField.getText().trim();
                if (!path.startsWith("http://") && !path.startsWith("https://")) {
                    say(message, "A link must start with http:// or https://", false);
                    return;
                }
            } else if (chosenFile != null) {
                String err = fileError(chosenFile);
                if (err != null) { say(message, err, false); return; }
                path = store(chosenFile, c.code());
                newFile = true;
            } else if (update && !selected.type().equals("Link")) {
                path = selected.filePath();                     // keep the old file
            } else {
                say(message, "Please choose a file.", false);
                return;
            }

            if (update) {
                LecturerDAO.updateMaterial(selected.id(), title, type, path, from, until);
                if (newFile || link) deleteStored(selected.filePath());   // old file goes only after the update worked
            } else {
                LecturerDAO.addMaterial(c.code(), title, type, path, from, until);
            }
            clearForm();
            load();
            say(message, update ? "Material updated." : "Material added.", true);
        } catch (Exception e) {
            say(message, "Could not save the material: " + e.getMessage(), false);
        }
    }

    private void delete() {
        if (selected == null) { say(message, "Select a material from the table first.", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.title() + "\"? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

        try {
            LecturerDAO.deleteMaterial(selected.id());
            deleteStored(selected.filePath());
            clearForm();
            load();
            say(message, "Material deleted.", true);
        } catch (Exception e) {
            say(message, "Could not delete the material: " + e.getMessage(), false);
        }
    }

    private void deleteStored(String filePath) throws Exception {
        LecturerDAO.deleteStoredMaterialFile(filePath);
    }

    /** Text for the Until column. */
    private static String untilText(Material m) {
        return m.availableUntil().isEmpty() ? "No end date" : m.availableUntil();
    }

    /** Text for the Status column. */
    private static String availability(Material m) {
        LocalDate today = LocalDate.now();
        if (!m.availableFrom().isEmpty() && LocalDate.parse(m.availableFrom()).isAfter(today)) {
            return "Starts " + m.availableFrom();
        }
        if (m.availableUntil().isEmpty()) return "Available";
        long days = ChronoUnit.DAYS.between(today, LocalDate.parse(m.availableUntil()));
        return days == 0 ? "Last day today" : "Removed in " + days + " day(s)";
    }

    private void open() {
        if (selected == null) { say(message, "Select a material from the table first.", false); return; }
        String path = selected.filePath();
        if (!Desktop.isDesktopSupported()) { say(message, "Opening files is not supported here.", false); return; }

        new Thread(() -> {
            try {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    Desktop.getDesktop().browse(URI.create(path));
                } else {
                    File f = new File(path);
                    if (!f.isAbsolute()) f = new File(System.getProperty("user.dir"), path);
                    if (!f.exists()) {
                        Platform.runLater(() -> say(message, "File not found: " + path, false));
                        return;
                    }
                    Desktop.getDesktop().open(f);
                }
            } catch (Exception e) {
                Platform.runLater(() -> say(message, "Could not open it: " + e.getMessage(), false));
            }
        }).start();
    }
}
