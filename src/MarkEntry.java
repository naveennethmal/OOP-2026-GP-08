import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MarkEntry {
    private final String regNo;
    private final String fullName;
    private final StringProperty marks = new SimpleStringProperty("");

    public MarkEntry(String regNo, String fullName, String marks) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.marks.set(marks == null ? "" : marks);
    }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public String getMarks() { return marks.get(); }
    public void setMarks(String m) { marks.set(m); }
    public StringProperty marksProperty() { return marks; }

    public boolean hasMark() {
        return marks.get() != null && !marks.get().trim().isEmpty();
    }

    // Validates and returns the mark (must be a number from 0 to 100)
    public double getMarkValue() throws InvalidMarkException {
        double v;
        try {
            v = Double.parseDouble(marks.get().trim());
        } catch (NumberFormatException e) {
            throw new InvalidMarkException(regNo + ": '" + marks.get() + "' is not a number");
        }
        if (v < 0 || v > 100) {
            throw new InvalidMarkException(regNo + ": marks must be between 0 and 100");
        }
        return v;
    }
}