import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EligibilityService {

    private final MarkDAO dao = new MarkDAO();

    // CA = every assessment except the Final exam
    public List<AssessmentItem> getCaAssessments(String courseCode) throws SQLException {
        List<AssessmentItem> ca = new ArrayList<>();
        for (AssessmentItem a : dao.getAssessments(courseCode)) {
            if (!a.getType().equals("Final")) ca.add(a);
        }
        return ca;
    }

    // CA result for every enrolled student of the course
    public List<CAResult> calculate(String courseCode) throws SQLException {
        List<AssessmentItem> caList = getCaAssessments(courseCode);

        double totalWeight = 0;
        for (AssessmentItem a : caList) totalWeight += a.getWeight();

        Map<String, CAResult> map = new LinkedHashMap<>();
        for (AssessmentItem a : caList) {
            for (MarkEntry e : dao.getStudentMarks(courseCode, a.getId())) {
                CAResult r = map.get(e.getRegNo());
                if (r == null) {
                    r = new CAResult(e.getRegNo(), e.getFullName(), totalWeight);
                    map.put(e.getRegNo(), r);
                }
                Double mark = e.hasMark() ? Double.valueOf(e.getMarks()) : null;
                r.addComponent(new CAResult.Component(
                        a.getName(), a.getType(), a.getWeight(), mark));
            }
        }
        return new ArrayList<>(map.values());
    }
}