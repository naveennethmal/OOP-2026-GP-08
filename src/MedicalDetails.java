import javafx.scene.control.Button;
import javafx.scene.layout.*;

public class MedicalDetails extends VBox {
    public MedicalDetails() {
        super(18);
        getStyleClass().add("content");

        HBox banner = new HBox(16, Ui.text("✅", "banner-icon"),
                new VBox(4, Ui.text("All Clear", "banner-title"),
                        Ui.text("You have no pending medical submissions or health alerts.", "banner-sub")));
        banner.getStyleClass().add("banner-ok");

        String[][] info = {
                {"Blood Group", "O+"}, {"Allergies", "None"}, {"Chronic Conditions", "None"},
                {"Emergency Contact", "Kamal Perera (Father) - +94 77 123 4567"}};
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(70);
        grid.getColumnConstraints().addAll(c1, c2);
        for (int i = 0; i < info.length; i++) {
            grid.add(Ui.text(info[i][0], "field-label"), 0, i);
            grid.add(Ui.text(info[i][1], "table-cell"), 1, i);
        }

        Object[][] history = {
                {"2026-08-12", "Fever", "2 days", Ui.tag("Approved", "green")},
                {"2026-05-03", "Sports injury", "3 days", Ui.tag("Approved", "green")}};

        Button submit = new Button("Submit New Medical");
        submit.getStyleClass().add("primary-btn");

        getChildren().addAll(
                Ui.header("Medical Details", "Your health information and medical submissions"),
                banner,
                Ui.card("Health Information", grid),
                Ui.card("Submitted Medicals", new VBox(16,
                        Ui.table(new String[]{"Date", "Reason", "Duration", "Status"}, history), submit)));
    }
}