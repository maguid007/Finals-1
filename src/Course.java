import java.io.Serializable;

public class Course implements Comparable<Course>, Serializable {
    private int yearLevel;
    private String course;
    private String courseCode;
    private String courseTitle;
    private double units;
    private String prerequisite;
    private String grade;
    private int semester; // 1 for first sem, 2 for second sem, 3 for short term

    public Course(int yearLevel, String course, String courseCode, String courseTitle,
                  double units, String prerequisite, String grade) {
        this.yearLevel = yearLevel;
        this.course = course;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.units = units;
        this.prerequisite = prerequisite;
        this.grade = grade;
        this.semester = determineSemester(courseCode);
    }

    private int determineSemester(String courseCode) {
        // Extract the numeric part after the program code
        String numericPart = courseCode.replaceAll("[^0-9]", "");
        if (numericPart.length() >= 3) {
            int courseNum = Integer.parseInt(numericPart.substring(0, 3));
            if (courseNum >= 111 && courseNum <= 113 ||
                    courseCode.startsWith("G") || courseCode.startsWith("FIT") ||
                    courseCode.startsWith("CFE 101") || courseCode.startsWith("NSTP-CWTS 1")) {
                return 1; // First Semester
            } else if (courseNum >= 121 && courseNum <= 123 ||
                    courseCode.startsWith("CFE 102") || courseCode.startsWith("NSTP-CWTS 2")) {
                return 2; // Second Semester
            } else if (courseNum >= 131 || courseCode.startsWith("CFE 103")) {
                return 3; // Short Term
            } else if (courseNum >= 211 && courseNum <= 213) {
                return 1;
            } else if (courseNum >= 221 && courseNum <= 223) {
                return 2;
            } else if (courseNum >= 231 || courseCode.startsWith("CFE 104")) {
                return 3;
            } else if (courseNum >= 311 && courseNum <= 315) {
                return 1;
            } else if (courseNum >= 321 && courseNum <= 325) {
                return 2;
            } else if (courseNum >= 331 || courseCode.startsWith("CFE 105")) {
                return 3;
            } else if (courseNum >= 411) {
                return 1;
            } else if (courseNum >= 421) {
                return 2;
            }
        }
        return 1; // Default to first semester
    }

    // Getters and Setters
    public int getYearLevel() { return yearLevel; }
    public String getCourse() { return course; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public double getUnits() { return units; }
    public String getPrerequisite() { return prerequisite; }
    public String getGrade() { return grade; }
    public int getSemester() { return semester; }

    public void setGrade(String grade) { this.grade = grade; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public void setUnits(double units) { this.units = units; }
    public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }

    public String getTermKey() {
        return yearLevel + "-" + course + "-" + semester;
    }

    public String getSemesterName() {
        switch (semester) {
            case 1: return "First Semester";
            case 2: return "Second Semester";
            case 3: return "Short Term";
            default: return "Unknown Term";
        }
    }

    @Override
    public int compareTo(Course other) {
        int yearCompare = Integer.compare(this.yearLevel, other.yearLevel);
        if (yearCompare != 0) return yearCompare;
        int semCompare = Integer.compare(this.semester, other.semester);
        if (semCompare != 0) return semCompare;
        return this.courseCode.compareTo(other.courseCode);
    }
}