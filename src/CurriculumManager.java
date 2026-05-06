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

    public Map<String, List<T>> getSortedTermGroups() {
        return new TreeMap<>(termGroups);
    }

    public List<T> getAllCourses() {
        List<T> allCourses = new ArrayList<>();
        for (List<T> courses : termGroups.values()) {
            allCourses.addAll(courses);
        }
        return allCourses;
    }

    public List<T> getCoursesByProgram(String program) {
        List<T> result = new ArrayList<>();
        for (List<T> courses : termGroups.values()) {
            for (T course : courses) {
                if (course.getCourse().equalsIgnoreCase(program)) {
                    result.add(course);
                }
            }
        }
        return result;
    }

    public List<T> getCoursesByProgramAndYear(String program, int yearLevel) {
        List<T> result = new ArrayList<>();
        for (List<T> courses : termGroups.values()) {
            for (T course : courses) {
                if (course.getCourse().equalsIgnoreCase(program) &&
                        course.getYearLevel() == yearLevel) {
                    result.add(course);
                }
            }
        }
        return result;
    }

    public T findCourse(String courseCode) {
        for (List<T> courses : termGroups.values()) {
            for (T course : courses) {
                if (course.getCourseCode().equalsIgnoreCase(courseCode)) {
                    return course;
                }
            }
        }
        return null;
    }

    public List<T> getCoursesWithoutGrades() {
        return getAllCourses().stream()
                .filter(course -> course.getGrade().equals("Not yet taken"))
                .collect(Collectors.toList());
    }

    public List<T> getCoursesWithGrades() {
        return getAllCourses().stream()
                .filter(course -> !course.getGrade().equals("Not yet taken"))
                .collect(Collectors.toList());
    }

    public int getTotalCourses() {
        return getAllCourses().size();
    }
}