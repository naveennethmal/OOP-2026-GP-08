import java.util.ArrayList;
import java.util.List;

public class CAResult {

    public static final double PASS_PERCENTAGE = 40.0;

    // One row of the individual breakdown (one assessment)
    public static class Component {
        private final String name;
        private final String type;
        private final double weight;
        private final Double mark;   // null = not entered yet

        public Component(String name, String type, double weight, Double mark) {
            this.name = name;
            this.type = type;
            this.weight = weight;
            this.mark = mark;
        }

        public boolean hasMark() { return mark != null; }
        public double getContribution() { return mark == null ? 0 : mark / 100.0 * weight; }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getWeightText() { return String.format("%.1f%%", weight); }
        public String getMarkText() { return mark == null ? "-" : String.format("%.2f", mark); }
        public String getContributionText() { return String.format("%.2f", getContribution()); }
    }

    private final String regNo;
    private final String fullName;
    private final double totalWeight;
    private final List<Component> components = new ArrayList<>();

    public CAResult(String regNo, String fullName, double totalWeight) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.totalWeight = totalWeight;
    }

    public void addComponent(Component c) { components.add(c); }

    public double getEarned() {
        double sum = 0;
        for (Component c : components) sum += c.getContribution();
        return sum;
    }

    public int getEntered() {
        int n = 0;
        for (Component c : components) if (c.hasMark()) n++;
        return n;
    }

    public double getPercentage() {
        return totalWeight == 0 ? 0 : getEarned() / totalWeight * 100;
    }

    public boolean isEligible() {
        return totalWeight > 0 && getPercentage() >= PASS_PERCENTAGE;
    }

    public String getStatus() {
        if (isEligible()) return "Eligible";
        if (getEntered() < components.size()) return "Pending";   // some marks not uploaded yet
        return "Not Eligible";
    }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public List<Component> getComponents() { return components; }
    public String getCaText() { return String.format("%.2f / %.2f", getEarned(), totalWeight); }
    public String getPercentageText() { return String.format("%.2f%%", getPercentage()); }
    public String getEnteredText() { return getEntered() + " / " + components.size(); }
}