import javafx.scene.layout.VBox;

public class GradesGpa extends VBox {

    public GradesGpa() {

        super(18);

        getStyleClass().add("content");

        // =========================================================
        // SUBJECT RESULTS
        // =========================================================

        String[][] g = {

                {"Java Programming",
                        "3",
                        "88",
                        "A",
                        "4.0"},

                {"Database Systems",
                        "3",
                        "82",
                        "A-",
                        "3.7"},

                {"Software Engineering",
                        "3",
                        "76",
                        "B+",
                        "3.3"},

                {"Web Technologies",
                        "3",
                        "91",
                        "A+",
                        "4.0"},

                {"Computer Networks",
                        "3",
                        "79",
                        "B+",
                        "3.3"},

                {"Data Structures & Algorithms",
                        "3",
                        "85",
                        "A",
                        "4.0"}
        };


        // =========================================================
        // TABLE ROWS
        // =========================================================

        Object[][] rows = new Object[g.length][];

        for (int i = 0; i < g.length; i++) {

            String grade = g[i][3];

            String color;

            if (grade.equals("A+") ||
                    grade.equals("A") ||
                    grade.equals("A-")) {

                color = "green";

            } else if (grade.startsWith("B")) {

                color = "blue";

            } else {

                color = "purple";
            }


            rows[i] = new Object[]{

                    g[i][0],              // Course

                    g[i][1],              // Credits

                    g[i][2],              // Marks

                    Ui.tag(
                            grade,
                            color
                    ),                    // Grade

                    g[i][4]               // Grade Points
            };
        }


        // =========================================================
        // PAGE CONTENT
        // =========================================================

        getChildren().addAll(

                // -------------------------------------------------
                // PAGE HEADER
                // -------------------------------------------------

                Ui.header(
                        "Grades & GPA",
                        "View your academic performance and examination results"
                ),


                // -------------------------------------------------
                // GPA STATISTICS
                // -------------------------------------------------

                Ui.stats(
                        new String[][]{

                                {
                                        "Current GPA",
                                        "3.75",
                                        "purple"
                                },

                                {
                                        "Total Credits",
                                        "18",
                                        "blue"
                                },

                                {
                                        "Class",
                                        "First Class",
                                        "green"
                                }

                        }
                ),


                // -------------------------------------------------
                // SEMESTER RESULTS
                // -------------------------------------------------

                Ui.card(
                        "Semester Results",

                        Ui.table(

                                new String[]{

                                        "Course",
                                        "Credits",
                                        "Marks",
                                        "Grade",
                                        "Grade Points"

                                },

                                rows
                        )
                )
        );
    }
}