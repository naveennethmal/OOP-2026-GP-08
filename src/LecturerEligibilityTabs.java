import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LecturerEligibilityTabs extends VBox {

    public LecturerEligibilityTabs(Lecturer lecturer) {
        LecturerOverallEligibilityPage overall = new LecturerOverallEligibilityPage(lecturer);
        LecturerCAPage ca = new LecturerCAPage(lecturer);
        overall.setPadding(new Insets(12, 0, 0, 0));
        ca.setPadding(new Insets(12, 0, 0, 0));

        TabPane tabs = new TabPane(
                new Tab("Overall Eligibility", overall),
                new Tab("CA Marks", ca));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }
}