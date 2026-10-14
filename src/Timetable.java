import javafx.scene.layout.VBox;

public class Timetable extends VBox {
    public Timetable() {
        super(18);
        getStyleClass().add("content");

        Object[][] rows = {
                {"10:00 - 11:00", "Java Programming\nLab 1", "Web Technologies\nLab 3", "Database Systems\nLab 2",
                        "Computer Networks\nRoom 101", "Software Engineering\nHall A"},
                {"11:00 - 12:00", "Database Systems\nLab 2", "Java Programming\nLab 1", "Web Technologies\nLab 3",
                        "Software Engineering\nHall A", "Computer Networks\nRoom 101"},
                {"02:00 - 03:00", "Database Systems\nLab 2", "Data Structures\nRoom 102", "Java Programming\nLab 1",
                        "Web Technologies\nLab 3", "Data Structures\nRoom 102"},
                {"04:00 - 05:00", "Computer Networks\nRoom 101", "Software Engineering\nHall A", "Data Structures\nRoom 102",
                        "Java Programming\nLab 1", "Database Systems\nLab 2"}};

        getChildren().addAll(
                Ui.header("Timetable", "Your weekly class schedule"),
                Ui.card("Weekly Schedule", Ui.table(
                        new String[]{"Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"}, rows)));
    }
}