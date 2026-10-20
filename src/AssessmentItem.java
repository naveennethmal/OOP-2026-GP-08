public class AssessmentItem {
    private final int id;
    private final String name;
    private final String type;
    private final double weight;

    public AssessmentItem(int id, String name, String type, double weight) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.weight = weight;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getWeight() { return weight; }

    @Override
    public String toString() {
        return name + " (" + type + ", " + weight + "%)";
    }
}