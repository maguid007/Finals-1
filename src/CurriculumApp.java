import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// Generic class to represent a school term
class SchoolTerm<T extends Course> {
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

    @Override
    public String toString() {
        return termName;
    }
}

// Course class implementing Comparable for sorting
class Course implements Comparable<Course>, Serializable {
    private int yearLevel;
    private String course;
    private String courseCode;
    private String courseTitle;
    private double units;
    private String prerequisite;
    private String grade;

    public Course(int yearLevel, String course, String courseCode, String courseTitle,
                  double units, String prerequisite, String grade) {
        this.yearLevel = yearLevel;
        this.course = course;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.units = units;
        this.prerequisite = prerequisite;
        this.grade = grade;
    }

    // Getters and Setters
    public int getYearLevel() { return yearLevel; }
    public String getCourse() { return course; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public double getUnits() { return units; }
    public String getPrerequisite() { return prerequisite; }
    public String getGrade() { return grade; }

    public void setGrade(String grade) { this.grade = grade; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public void setUnits(double units) { this.units = units; }
    public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }

    public String getTermKey() {
        return yearLevel + "-" + course;
    }

    @Override
    public String toString() {
        return String.format("%-10s %-10s %-45s %-5.1f %-30s %-15s",
                courseCode, course, courseTitle, units,
                prerequisite.isEmpty() ? "None" : prerequisite, grade);
    }

    @Override
    public int compareTo(Course other) {
        int yearCompare = Integer.compare(this.yearLevel, other.yearLevel);
        if (yearCompare != 0) return yearCompare;
        return this.courseCode.compareTo(other.courseCode);
    }
}

// Generic Curriculum Manager
class CurriculumManager<T extends Course> {
    private Map<String, List<T>> termGroups;
    private Map<String, SchoolTerm<T>> schoolTerms;
    private String dataFilePath;

    public CurriculumManager(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.termGroups = new LinkedHashMap<>();
        this.schoolTerms = new LinkedHashMap<>();
    }

    // Generic method to group courses by term
    public <E extends T> void groupByTerm(List<E> courses, Function<E, String> termExtractor) {
        termGroups.clear();
        for (E course : courses) {
            String termKey = termExtractor.apply(course);
            termGroups.computeIfAbsent(termKey, k -> new ArrayList<>()).add(course);
        }
    }

    // Functional interface for term extraction
    @FunctionalInterface
    interface Function<T, R> {
        R apply(T t);
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

    public boolean updateCourse(String courseCode, T updatedCourse) {
        for (Map.Entry<String, List<T>> entry : termGroups.entrySet()) {
            List<T> courses = entry.getValue();
            for (int i = 0; i < courses.size(); i++) {
                if (courses.get(i).getCourseCode().equalsIgnoreCase(courseCode)) {
                    courses.set(i, updatedCourse);
                    return true;
                }
            }
        }
        return false;
    }
}

// Main Application Class
public class CurriculumApp {
    private static List<Course> allCourses = new ArrayList<>();
    private static CurriculumManager<Course> curriculumManager;
    private static final String CSV_FILE = "BSCSBSIT2018.csv";
    private static final String UPDATED_FILE = "BSCSBSIT2018_updated.csv";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        curriculumManager = new CurriculumManager<>(CSV_FILE);

        // Load data from CSV
        loadCoursesFromCSV(CSV_FILE);

        // Group courses by term
        curriculumManager.groupByTerm(allCourses, course -> course.getTermKey());

        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    showSubjectsForEachTerm();
                    break;
                case 2:
                    showSubjectsWithGrades();
                    break;
                case 3:
                    enterGrades();
                    break;
                case 4:
                    editCourse();
                    break;
                case 5:
                    saveAndQuit();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n===========================================");
        System.out.println("    CURRICULUM MANAGEMENT SYSTEM");
        System.out.println("===========================================");
        System.out.println("1. Show subjects for each school term");
        System.out.println("2. Show subjects with grades for each term");
        System.out.println("3. Enter grades for subjects recently finished");
        System.out.println("4. Edit a course");
        System.out.println("5. Quit");
        System.out.println("===========================================");
    }

    private static void loadCoursesFromCSV(String filename) {
        allCourses.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (fields.length >= 7) {
                    Course course = new Course(
                            Integer.parseInt(fields[0].trim()),
                            fields[1].trim(),
                            fields[2].trim(),
                            fields[3].trim(),
                            Double.parseDouble(fields[4].trim()),
                            fields[5].trim().replace("\"", ""),
                            fields[6].trim()
                    );
                    allCourses.add(course);
                }
            }
            System.out.println("Loaded " + allCourses.size() + " courses from " + filename);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void showSubjectsForEachTerm() {
        System.out.println("\n===========================================");
        System.out.println("      SUBJECTS FOR EACH SCHOOL TERM");
        System.out.println("===========================================");

        Map<String, List<Course>> grouped = curriculumManager.getTermGroups();

        // Sort by term
        Map<String, List<Course>> sortedGroups = new TreeMap<>(grouped);

        int termCount = 0;
        for (Map.Entry<String, List<Course>> entry : sortedGroups.entrySet()) {
            termCount++;
            String[] termParts = entry.getKey().split("-");
            String termDescription = String.format("Year %s - %s",
                    termParts[0],
                    termParts[1].equals("BSIT") ? "BS Information Technology" : "BS Computer Science");

            System.out.println("\n" + termDescription);
            System.out.println(String.join("", Collections.nCopies(90, "-")));
            System.out.printf("%-12s %-50s %-6s %-15s\n", "Course Code", "Course Title", "Units", "Pre-requisite");
            System.out.println(String.join("", Collections.nCopies(90, "-")));

            List<Course> courses = entry.getValue();
            Collections.sort(courses);

            for (Course course : courses) {
                System.out.printf("%-12s %-50s %-6.1f %-15s\n",
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        course.getUnits(),
                        course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite());
            }

            if (termCount % 2 == 0) {
                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void showSubjectsWithGrades() {
        System.out.println("\n===========================================");
        System.out.println("   SUBJECTS WITH GRADES FOR EACH TERM");
        System.out.println("===========================================");

        Map<String, List<Course>> grouped = curriculumManager.getTermGroups();
        Map<String, List<Course>> sortedGroups = new TreeMap<>(grouped);

        for (Map.Entry<String, List<Course>> entry : sortedGroups.entrySet()) {
            String[] termParts = entry.getKey().split("-");
            String termDescription = String.format("Year %s - %s",
                    termParts[0],
                    termParts[1].equals("BSIT") ? "BSIT" : "BSCS");

            System.out.println("\n" + termDescription);
            System.out.println(String.join("", Collections.nCopies(100, "-")));
            System.out.printf("%-12s %-50s %-10s %-20s\n",
                    "Course Code", "Course Title", "Grade", "Status");
            System.out.println(String.join("", Collections.nCopies(100, "-")));

            List<Course> courses = entry.getValue();
            Collections.sort(courses);

            for (Course course : courses) {
                String grade = course.getGrade();
                String status;
                if (grade.equals("Not yet taken")) {
                    status = "Not Taken";
                } else if (grade.equals("INC") || grade.equals("DRP")) {
                    status = grade;
                } else {
                    try {
                        double gradeNum = Double.parseDouble(grade);
                        status = gradeNum >= 75 ? "PASSED" : "FAILED";
                    } catch (NumberFormatException e) {
                        status = "Invalid Grade";
                    }
                }

                System.out.printf("%-12s %-50s %-10s %-20s\n",
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        grade,
                        status);
            }
        }
    }

    private static void enterGrades() {
        System.out.println("\n===========================================");
        System.out.println("    ENTER GRADES FOR FINISHED SUBJECTS");
        System.out.println("===========================================");

        // Show only courses without grades
        System.out.println("\nCourses without grades:");
        System.out.println(String.join("", Collections.nCopies(80, "-")));
        System.out.printf("%-5s %-12s %-50s %-6s\n", "No.", "Course Code", "Course Title", "Units");
        System.out.println(String.join("", Collections.nCopies(80, "-")));

        List<Course> ungradedCourses = new ArrayList<>();
        int index = 1;
        for (Course course : allCourses) {
            if (course.getGrade().equals("Not yet taken")) {
                System.out.printf("%-5d %-12s %-50s %-6.1f\n",
                        index,
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        course.getUnits());
                ungradedCourses.add(course);
                index++;
            }
        }

        if (ungradedCourses.isEmpty()) {
            System.out.println("\nAll courses already have grades!");
            return;
        }

        System.out.print("\nEnter course number to grade (0 to cancel): ");
        int choice = getIntInput("");
        if (choice > 0 && choice <= ungradedCourses.size()) {
            Course selected = ungradedCourses.get(choice - 1);
            System.out.println("\nSelected: " + selected.getCourseCode() + " - " + selected.getCourseTitle());
            System.out.print("Enter grade (50-100, or INC/DRP): ");
            String grade = scanner.nextLine().trim();

            // Validate grade
            if (grade.equalsIgnoreCase("INC") || grade.equalsIgnoreCase("DRP")) {
                selected.setGrade(grade.toUpperCase());
                System.out.println("Grade set to " + grade.toUpperCase());
            } else {
                try {
                    double gradeNum = Double.parseDouble(grade);
                    if (gradeNum >= 50 && gradeNum <= 100) {
                        selected.setGrade(grade);
                        System.out.println("Grade set to " + grade);
                    } else {
                        System.out.println("Invalid grade! Must be between 50-100 or INC/DRP");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input! Must be numeric grade or INC/DRP");
                }
            }
        } else if (choice != 0) {
            System.out.println("Invalid selection!");
        }
    }

    private static void editCourse() {
        System.out.println("\n===========================================");
        System.out.println("          EDIT A COURSE");
        System.out.println("===========================================");

        System.out.print("Enter course code to edit: ");
        String courseCode = scanner.nextLine().trim();

        Course course = curriculumManager.findCourse(courseCode);
        if (course == null) {
            System.out.println("Course not found!");
            return;
        }

        System.out.println("\nCurrent details:");
        System.out.println("Course Code: " + course.getCourseCode());
        System.out.println("Course Title: " + course.getCourseTitle());
        System.out.println("Units: " + course.getUnits());
        System.out.println("Pre-requisite: " + (course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite()));
        System.out.println("Grade: " + course.getGrade());

        System.out.println("\nWhat would you like to edit?");
        System.out.println("1. Course Title");
        System.out.println("2. Units");
        System.out.println("3. Pre-requisite");
        System.out.println("4. Grade");
        System.out.println("5. Cancel");

        int choice = getIntInput("Enter choice: ");

        switch (choice) {
            case 1:
                System.out.print("Enter new course title: ");
                String newTitle = scanner.nextLine().trim();
                if (!newTitle.isEmpty()) {
                    course.setCourseTitle(newTitle);
                    System.out.println("Course title updated successfully!");
                }
                break;
            case 2:
                System.out.print("Enter new units: ");
                double newUnits = getDoubleInput("");
                if (newUnits > 0) {
                    course.setUnits(newUnits);
                    System.out.println("Units updated successfully!");
                }
                break;
            case 3:
                System.out.print("Enter new prerequisite: ");
                String newPrereq = scanner.nextLine().trim();
                course.setPrerequisite(newPrereq);
                System.out.println("Prerequisite updated successfully!");
                break;
            case 4:
                System.out.print("Enter new grade: ");
                String newGrade = scanner.nextLine().trim();
                course.setGrade(newGrade);
                System.out.println("Grade updated successfully!");
                break;
            case 5:
                System.out.println("Edit cancelled.");
                break;
            default:
                System.out.println("Invalid choice!");
        }
    }

    private static void saveAndQuit() {
        System.out.println("\nSaving changes...");

        try (PrintWriter pw = new PrintWriter(new FileWriter(UPDATED_FILE))) {
            // Write header
            pw.println("YearLevel,Course,CourseCode,CourseTitle,Units,Prerequisite,Grade");

            // Sort courses before saving
            List<Course> sortedCourses = new ArrayList<>(allCourses);
            Collections.sort(sortedCourses);

            for (Course course : sortedCourses) {
                pw.printf("%d,%s,%s,\"%s\",%.1f,\"%s\",%s%n",
                        course.getYearLevel(),
                        course.getCourse(),
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        course.getUnits(),
                        course.getPrerequisite(),
                        course.getGrade());
            }

            System.out.println("Data saved to " + UPDATED_FILE);
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }

        System.out.println("\nThank you for using Curriculum Management System!");
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }
}
