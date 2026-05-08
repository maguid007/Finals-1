import java.util.*;
import java.util.stream.Collectors;

public class CurriculumManager<T extends Course> {
    private Map<String, List<T>> termGroups;
    private String dataFilePath;

    public CurriculumManager(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.termGroups = new LinkedHashMap<>();
    }

    @FunctionalInterface
    public interface TermExtractor<T, R> {
        R apply(T t);
    }

    public void groupByTerm(List<T> courses, TermExtractor<T, String> termExtractor) {
        termGroups.clear();
        for (T course : courses) {
            String termKey = termExtractor.apply(course);
            termGroups.computeIfAbsent(termKey, k -> new ArrayList<>()).add(course);
        }
    }

    public Map<String, List<T>> getTermGroups() {
        return termGroups;
    }

    public List<T> getAllCourses() {
        List<T> allCourses = new ArrayList<>();
        for (List<T> courses : termGroups.values()) {
            allCourses.addAll(courses);
        }
        return allCourses;
    }

    public List<T> getCoursesByStudentId(String schoolId) {
        return getAllCourses().stream()
                .filter(course -> course.getSchoolId().equals(schoolId))
                .collect(Collectors.toList());
    }

    public List<T> getCoursesByStudentIdAndProgram(String schoolId, String courseTaken) {
        return getAllCourses().stream()
                .filter(course -> course.getSchoolId().equals(schoolId) &&
                        course.getCourseTaken().equals(courseTaken))
                .collect(Collectors.toList());
    }

    public List<T> getCoursesByProgram(String program) {
        return getAllCourses().stream()
                .filter(course -> course.getCourseTaken().equals(program))
                .collect(Collectors.toList());
    }

    public T findCourse(String schoolId, String courseCode) {
        for (List<T> courses : termGroups.values()) {
            for (T course : courses) {
                if (course.getSchoolId().equals(schoolId) &&
                        course.getCourseCode().equalsIgnoreCase(courseCode)) {
                    return course;
                }
            }
        }
        return null;
    }

    public List<T> getCoursesWithoutGrades(String schoolId, String courseTaken) {
        return getCoursesByStudentIdAndProgram(schoolId, courseTaken).stream()
                .filter(course -> course.getGrade().equals("Not yet taken"))
                .collect(Collectors.toList());
    }

    public List<T> getCoursesWithGrades(String schoolId, String courseTaken) {
        return getCoursesByStudentIdAndProgram(schoolId, courseTaken).stream()
                .filter(course -> !course.getGrade().equals("Not yet taken"))
                .collect(Collectors.toList());
    }

    public void updateCourseTakenForStudent(String schoolId, String newCourseTaken) {
        for (Map.Entry<String, List<T>> entry : termGroups.entrySet()) {
            List<T> courses = entry.getValue();
            for (T course : courses) {
                if (course.getSchoolId().equals(schoolId)) {
                    course.setCourseTaken(newCourseTaken);
                }
            }
        }
    }

    public String getStudentCourse(String schoolId) {
        for (List<T> courses : termGroups.values()) {
            for (T course : courses) {
                if (course.getSchoolId().equals(schoolId)) {
                    return course.getCourseTaken();
                }
            }
        }
        return "Unknown";
    }
}