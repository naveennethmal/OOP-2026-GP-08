package com.teclms.technician;

import com.teclms.technician.DemoData.Slot;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;


public class TimetableView extends VBox {

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final String[] PERIODS = {"08:00 - 10:00", "10:00 - 12:00", "13:00 - 15:00", "15:00 - 17:00"};

    public TimetableView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox legend = new HBox(8, UiKit.pill("Theory", "info"), UiKit.pill("Practical", "purple"));
        legend.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(
                UiKit.pageHeader("Timetable", "Department of ICT - Level II, Semester I", legend),
                UiKit.card(buildGrid()));
    }

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints timeCol = new ColumnConstraints(110);
        grid.getColumnConstraints().add(timeCol);
        for (int i = 0; i < DAYS.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(120);
            cc.setPrefWidth(160);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        for (int d = 0; d < DAYS.length; d++) {
            Label day = new Label(DAYS[d]);
            day.getStyleClass().add("tt-day");
            day.setMaxWidth(Double.MAX_VALUE);
            day.setAlignment(Pos.CENTER);
            grid.add(day, d + 1, 0);
        }

        Node[][] cells = new Node[PERIODS.length][DAYS.length];
        for (Slot s : DemoData.timetable()) cells[s.period()][s.day()] = slotCard(s);

        for (int p = 0; p < PERIODS.length; p++) {
            Label time = new Label(PERIODS[p]);
            time.getStyleClass().add("tt-time");
            grid.add(time, 0, p + 1);
            for (int d = 0; d < DAYS.length; d++) {
                Node cell = cells[p][d] != null ? cells[p][d] : emptyCell();
                grid.add(cell, d + 1, p + 1);
            }
        }
        return grid;
    }

    private VBox slotCard(Slot s) {
        Label code = new Label(s.code());
        code.getStyleClass().add("tt-code");
        Label name = new Label(s.name());
        name.setWrapText(true);
        name.getStyleClass().add("tt-name");
        Label room = new Label(s.room() + (s.practical() ? "  |  Practical" : "  |  Theory"));
        room.getStyleClass().add("tt-meta");
        VBox box = new VBox(3, code, name, room);
        box.getStyleClass().add(s.practical() ? "tt-practical" : "tt-theory");
        box.setMinHeight(88);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox emptyCell() {
        VBox box = new VBox();
        box.getStyleClass().add("tt-empty");
        box.setMinHeight(88);
        return box;
    }
}
