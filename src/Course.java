import java.io.Serializable;

public class Course implements Comparable<Course>, Serializable {
    private String schoolId;
    private int yearLevel;
    private String course;
    private String semester;
    private String courseCode;
    private String courseTitle;
    private double units;
    private String prerequisite;
    private String grade;
    private String courseTaken;

    public Course(String schoolId, int yearLevel, String course, String semester,
                  String courseCode, String courseTitle, double units,
                  String prerequisite, String grade, String courseTaken) {
        this.schoolId = schoolId;
        this.yearLevel = yearLevel;
        this.course = course;
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.units = units;
        this.prerequisite = prerequisite;
        this.grade = grade;
        this.courseTaken = courseTaken;
    }

    // Getters
    public String getSchoolId() { return schoolId; }
    public int getYearLevel() { return yearLevel; }
    public String getCourse() { return course; }
    public String getSemester() { return semester; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public double getUnits() { return units; }
    public String getPrerequisite() { return prerequisite; }
    public String getGrade() { return grade; }
    public String getCourseTaken() { return courseTaken; }

    // Setters
    public void setGrade(String grade) { this.grade = grade; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public void setUnits(double units) { this.units = units; }
    public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }
    public void setCourseTaken(String courseTaken) { this.courseTaken = courseTaken; }

    public String getTermKey() {
        return schoolId + "-" + yearLevel + "-" + courseTaken + "-" + getSemesterOrder();
    }

    public int getSemesterOrder() {
        switch (semester.toLowerCase()) {
            case "1st semester":
                return 1;
            case "2nd semester":
                return 2;
            case "short term":
                return 3;
            default:
                return 99;
        }
    }

    @Override
    public int compareTo(Course other) {
        int idCompare = this.schoolId.compareTo(other.schoolId);
        if (idCompare != 0) return idCompare;
        int courseCompare = this.courseTaken.compareTo(other.courseTaken);
        if (courseCompare != 0) return courseCompare;
        int yearCompare = Integer.compare(this.yearLevel, other.yearLevel);
        if (yearCompare != 0) return yearCompare;
        int semCompare = Integer.compare(this.getSemesterOrder(), other.getSemesterOrder());
        if (semCompare != 0) return semCompare;
        return this.courseCode.compareTo(other.courseCode);
    }
}