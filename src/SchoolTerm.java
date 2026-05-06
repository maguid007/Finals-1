import java.util.ArrayList;
import java.util.List;

public class SchoolTerm<T extends Course> {
    private String termName;
    private List<T> courses;

    public SchoolTerm(String termName) {
        this.termName = termName;
        this.courses = new ArrayList<>();
    }

    public void addCourse(T course) {
        courses.add(course);
    }

    public String getTermName() {
        return termName;
    }

    public List<T> getCourses() {
        return courses;
    }

    public int getCourseCount() {
        return courses.size();
    }

    @Override
    public String toString() {
        return termName;
    }
}