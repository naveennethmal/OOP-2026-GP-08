import javafx.scene.layout.VBox;

public class Attendance extends VBox {
    public Attendance() {
        super(18);
        getStyleClass().add("content");

        // course, present, absent, late
        String[][] d = {
                {"Java Programming", "9", "0", "1"}, {"Database Systems", "8", "1", "0"},
                {"Software Engineering", "8", "1", "0"}, {"Web Technologies", "7", "0", "0"},
                {"Computer Networks", "7", "1", "0"}, {"Data Structures & Algorithms", "7", "0", "0"}};

        Object[][] rows = new Object[d.length][];
        int P = 0, A = 0, L = 0;
        for (int i = 0; i < d.length; i++) {
            int p = Integer.parseInt(d[i][1]), a = Integer.parseInt(d[i][2]), l = Integer.parseInt(d[i][3]);
            int t = p + a + l, pct = p * 100 / t;
            P += p; A += a; L += l;
            rows[i] = new Object[]{d[i][0], p, a, l, t,
                    Ui.tag(pct + "%", pct >= 90 ? "green" : pct >= 80 ? "yellow" : "red")};
        }
        int total = P + A + L;

        getChildren().addAll(
                Ui.header("Attendance", "Your attendance summary for this semester"),
                Ui.stats(new String[][]{
                        {"Total Classes", "" + total, "blue"}, {"Present", "" + P, "green"},
                        {"Absent", "" + A, "red"}, {"Late", "" + L, "yellow"},
                        {"Attendance", (P * 100 / total) + "%", "purple"}}),
                Ui.card("Course-wise Attendance", Ui.table(
                        new String[]{"Course", "Present", "Absent", "Late", "Total", "Percentage"}, rows)));
    }
}
