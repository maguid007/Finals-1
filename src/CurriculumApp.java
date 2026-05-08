import java.util.*;
import java.io.*;

public class CurriculumApp {
    private static List<Course> allCourses = new ArrayList<>();
    private static CurriculumManager<Course> curriculumManager;
    private static FileHandler<Course> fileHandler;
    private static final String CSV_FILE = "BSITBSCSRecords.csv";
    private static final String BACKUP_FILE = "BSITBSCSRecords_backup.csv";
    private static String currentStudentId;
    private static String currentCourseTaken;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        fileHandler = new FileHandler<>();
        curriculumManager = new CurriculumManager<>(CSV_FILE);

        // Create backup of original file
        createBackup();

        // Load all records
        loadAllRecords();

        // Login with student ID
        if (!loginWithStudentId()) {
            System.out.println("Exiting program. Goodbye!");
            return;
        }

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
                    switchCourse();
                    break;
                case 6:
                    saveAndQuit();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void createBackup() {
        try {
            copyFile(CSV_FILE, BACKUP_FILE);
            System.out.println("Backup created: " + BACKUP_FILE);
        } catch (IOException e) {
            System.out.println("Warning: Could not create backup file.");
        }
    }

    private static void loadAllRecords() {
        allCourses = fileHandler.readCoursesFromCSV(CSV_FILE);
        System.out.println("Loaded " + allCourses.size() + " total records from " + CSV_FILE);
        curriculumManager.groupByTerm(allCourses, Course::getTermKey);
    }

    private static boolean loginWithStudentId() {
        System.out.println("\n===========================================");
        System.out.println("    CURRICULUM MANAGEMENT SYSTEM");
        System.out.println("         STUDENT LOGIN");
        System.out.println("===========================================");

        while (true) {
            System.out.print("\nEnter Student ID (or 0 to exit): ");
            String studentId = scanner.nextLine().trim();

            if (studentId.equals("0")) {
                return false;
            }

            if (studentId.isEmpty()) {
                System.out.println("Student ID cannot be empty. Please try again.");
                continue;
            }

            // Check if student exists in records
            List<Course> studentCourses = curriculumManager.getCoursesByStudentId(studentId);

            if (!studentCourses.isEmpty()) {
                currentStudentId = studentId;
                currentCourseTaken = studentCourses.get(0).getCourseTaken();
                System.out.println("\nLogin successful!");
                System.out.println("Student ID: " + currentStudentId);
                System.out.println("Course: " + currentCourseTaken +
                        (currentCourseTaken.equals("BSIT") ? " (BS Information Technology)" : " (BS Computer Science)"));
                System.out.println("Total records loaded: " + studentCourses.size());
                return true;
            } else {
                System.out.println("\nError: No records found for Student ID: " + studentId);
                System.out.println("\nAvailable Student IDs:");
                listAvailableStudentIds();
            }
        }
    }

    private static void listAvailableStudentIds() {
        Set<String> studentIds = new TreeSet<>();
        for (Course course : allCourses) {
            studentIds.add(course.getSchoolId());
        }

        System.out.println("----------------------------------------");
        for (String id : studentIds) {
            String course = curriculumManager.getStudentCourse(id);
            System.out.println("  " + id + " (" + course + ")");
        }
        System.out.println("----------------------------------------");
    }

    private static void displayMenu() {
        System.out.println("\n===========================================");
        System.out.println("    CURRICULUM MANAGEMENT SYSTEM");
        System.out.println("    Student ID: " + currentStudentId);
        System.out.println("    Course: " + currentCourseTaken);
        System.out.println("===========================================");
        System.out.println("1. Show subjects for each school term");
        System.out.println("2. Show subjects with grades for each term");
        System.out.println("3. Enter grades for subjects recently finished");
        System.out.println("4. Edit a course");
        System.out.println("5. Switch Course");
        System.out.println("6. Save and Quit");
        System.out.println("===========================================");
    }

    private static void switchCourse() {
        System.out.println("\n===========================================");
        System.out.println("SWITCH COURSE");
        System.out.println("===========================================");
        System.out.println("Current Course: " + currentCourseTaken);
        System.out.println("\nAvailable Courses:");
        System.out.println("1. BS Information Technology (BSIT)");
        System.out.println("2. BS Computer Science (BSCS)");
        System.out.println("0. Cancel");

        int choice = getIntInput("Enter your choice: ");

        String newCourse = null;
        switch (choice) {
            case 1:
                newCourse = "BSIT";
                break;
            case 2:
                newCourse = "BSCS";
                break;
            case 0:
                return;
            default:
                System.out.println("Invalid choice!");
                return;
        }

        if (newCourse.equals(currentCourseTaken)) {
            System.out.println("You are already enrolled in " + currentCourseTaken + ".");
            return;
        }

        // Check if student already has courses in the new program
        List<Course> existingNewCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, newCourse);

        if (!existingNewCourses.isEmpty()) {
            // Student already has some courses in this program
            System.out.println("\nYou already have records in " + newCourse + ".");
            System.out.println("Switching to " + newCourse + "...");
            currentCourseTaken = newCourse;
            System.out.println("Course switched successfully! Grades from previous course are retained.");
        } else {
            // Need to copy curriculum structure but preserve grades where possible
            System.out.print("\nSwitching to " + newCourse + ". This will copy the curriculum structure.");
            System.out.print("\nDo you want to proceed? (Y/N): ");
            String confirm = scanner.nextLine().trim().toUpperCase();

            if (confirm.equals("Y") || confirm.equals("YES")) {
                copyCurriculumToNewCourse(newCourse);
                currentCourseTaken = newCourse;
                System.out.println("Course switched successfully!");
            } else {
                System.out.println("Course switch cancelled.");
            }
        }
    }

    private static void copyCurriculumToNewCourse(String newCourse) {
        // Get current courses
        List<Course> currentCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, currentCourseTaken);

        // Create new courses for the new program
        List<Course> newCourses = new ArrayList<>();
        Map<String, String> gradeMap = new HashMap<>();

        // Store grades from current courses (by course code)
        for (Course course : currentCourses) {
            if (!course.getGrade().equals("Not yet taken")) {
                gradeMap.put(course.getCourseCode(), course.getGrade());
            }
        }

        // Get template courses for the new program (from other students)
        List<Course> templateCourses = new ArrayList<>();
        for (Course course : allCourses) {
            if (course.getCourseTaken().equals(newCourse) &&
                    !course.getSchoolId().equals(currentStudentId)) {
                // Check if this course code already exists for this student
                boolean exists = false;
                for (Course existing : newCourses) {
                    if (existing.getCourseCode().equals(course.getCourseCode()) &&
                            existing.getYearLevel() == course.getYearLevel() &&
                            existing.getSemester().equals(course.getSemester())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    templateCourses.add(course);
                }
            }
        }

        // Create new courses with preserved grades where applicable
        for (Course template : templateCourses) {
            String grade = "Not yet taken";
            // Check if there's a matching course code with a grade
            if (gradeMap.containsKey(template.getCourseCode())) {
                grade = gradeMap.get(template.getCourseCode());
            }

            Course newCourseObj = new Course(
                    currentStudentId,
                    template.getYearLevel(),
                    newCourse,
                    template.getSemester(),
                    template.getCourseCode(),
                    template.getCourseTitle(),
                    template.getUnits(),
                    template.getPrerequisite(),
                    grade,
                    newCourse
            );

            newCourses.add(newCourseObj);
        }

        // Add new courses to the system
        allCourses.addAll(newCourses);
        curriculumManager.groupByTerm(allCourses, Course::getTermKey);

        System.out.println("Added " + newCourses.size() + " courses for " + newCourse);
        if (!gradeMap.isEmpty()) {
            System.out.println("Preserved " + gradeMap.size() + " grades from previous course.");
        }
    }

    private static void showSubjectsForEachTerm() {
        List<Course> studentCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, currentCourseTaken);

        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : studentCourses) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemesterOrder(), k -> new ArrayList<>())
                    .add(course);
        }

        if (yearSemGroups.isEmpty()) {
            System.out.println("\nNo courses found for " + currentCourseTaken + ".");
            return;
        }

        System.out.println("\nSelect Year Level:");
        for (int year : yearSemGroups.keySet()) {
            System.out.println(year + ". Year " + year);
        }
        System.out.println("0. Return to Main Menu");

        int yearChoice = getIntInput("Enter your choice: ");

        if (yearChoice == 0) {
            return;
        }

        if (!yearSemGroups.containsKey(yearChoice)) {
            System.out.println("Invalid year level! Returning to main menu.");
            return;
        }

        Map<Integer, List<Course>> selectedYear = yearSemGroups.get(yearChoice);
        List<Integer> semesters = new ArrayList<>(selectedYear.keySet());
        Collections.sort(semesters);

        for (int i = 0; i < semesters.size(); i++) {
            int semesterOrder = semesters.get(i);
            List<Course> courses = selectedYear.get(semesterOrder);
            Collections.sort(courses);

            String semName = courses.get(0).getSemester();

            System.out.println("\n===========================================");
            System.out.println("Year " + yearChoice + " - " + semName + " - " + currentCourseTaken);
            System.out.println("===========================================");
            System.out.println("-------------------------------------------------------------------------");
            System.out.printf("%-12s  %-40s  %5s  %-10s\n",
                    "Course Code", "Course Title", "Units", "Pre-req");
            System.out.println("-------------------------------------------------------------------------");

            for (Course course : courses) {
                String prerequisite = course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite();
                if (prerequisite.length() > 10) {
                    prerequisite = prerequisite.substring(0, 7) + "...";
                }
                String courseTitle = course.getCourseTitle();
                if (courseTitle.length() > 40) {
                    courseTitle = courseTitle.substring(0, 37) + "...";
                }
                System.out.printf("%-12s  %-40s  %5.1f  %-10s\n",
                        course.getCourseCode(),
                        courseTitle,
                        course.getUnits(),
                        prerequisite);
            }

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("Total Courses: " + courses.size() + " | Total Units: " +
                    String.format("%.1f", courses.stream().mapToDouble(Course::getUnits).sum()));

            if (i < semesters.size() - 1) {
                System.out.print("\nPress ENTER for next semester or type 0 to return to menu: ");
            } else {
                System.out.print("\nPress ENTER to return to menu: ");
            }

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return;
            }
        }
    }

    private static void showSubjectsWithGrades() {
        List<Course> studentCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, currentCourseTaken);

        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : studentCourses) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemesterOrder(), k -> new ArrayList<>())
                    .add(course);
        }

        if (yearSemGroups.isEmpty()) {
            System.out.println("\nNo courses found for " + currentCourseTaken + ".");
            return;
        }

        System.out.println("\nSelect Year Level:");
        for (int year : yearSemGroups.keySet()) {
            System.out.println(year + ". Year " + year);
        }
        System.out.println("0. Return to Main Menu");

        int yearChoice = getIntInput("Enter your choice: ");

        if (yearChoice == 0) {
            return;
        }

        if (!yearSemGroups.containsKey(yearChoice)) {
            System.out.println("Invalid year level! Returning to main menu.");
            return;
        }

        Map<Integer, List<Course>> selectedYear = yearSemGroups.get(yearChoice);
        List<Integer> semesters = new ArrayList<>(selectedYear.keySet());
        Collections.sort(semesters);

        for (int i = 0; i < semesters.size(); i++) {
            int semesterOrder = semesters.get(i);
            List<Course> courses = selectedYear.get(semesterOrder);
            Collections.sort(courses);

            String semName = courses.get(0).getSemester();

            System.out.println("\n===========================================");
            System.out.println("Year " + yearChoice + " - " + semName + " - " + currentCourseTaken);
            System.out.println("===========================================");
            System.out.println("---------------------------------------------------------------------------------");
            System.out.printf("%-12s  %-35s  %6s  %-10s\n",
                    "Course Code", "Course Title", "Grade", "Status");
            System.out.println("---------------------------------------------------------------------------------");

            for (Course course : courses) {
                String grade = course.getGrade();
                String status = determineStatus(grade);

                String courseTitle = course.getCourseTitle();
                if (courseTitle.length() > 35) {
                    courseTitle = courseTitle.substring(0, 32) + "...";
                }

                System.out.printf("%-12s  %-35s  %6s  %-10s\n",
                        course.getCourseCode(),
                        courseTitle,
                        grade,
                        status);
            }

            long passedCount = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("PASSED")).count();
            long failedCount = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("FAILED")).count();
            long naCount = courses.stream().filter(c -> c.getGrade().equals("Not yet taken")).count();

            System.out.println("---------------------------------------------------------------------------------");
            System.out.printf("Summary: %d Total | %d Passed | %d Failed | %d N/A\n",
                    courses.size(), passedCount, failedCount, naCount);

            if (i < semesters.size() - 1) {
                System.out.print("\nPress ENTER for next semester or type 0 to return to menu: ");
            } else {
                System.out.print("\nPress ENTER to return to menu: ");
            }

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return;
            }
        }
    }

    private static String determineStatus(String grade) {
        if (grade.equals("Not yet taken")) {
            return "N/A";
        } else if (grade.equals("INC")) {
            return "INC";
        } else if (grade.equals("DRP")) {
            return "DRP";
        } else {
            try {
                double gradeNum = Double.parseDouble(grade);
                return gradeNum >= 75 ? "PASSED" : "FAILED";
            } catch (NumberFormatException e) {
                return "Invalid";
            }
        }
    }

    private static void enterGrades() {
        System.out.println("\n===========================================");
        System.out.println("ENTER GRADES FOR FINISHED SUBJECTS");
        System.out.println("===========================================");

        List<Course> ungradedCourses = curriculumManager.getCoursesWithoutGrades(currentStudentId, currentCourseTaken);

        if (ungradedCourses.isEmpty()) {
            System.out.println("\nAll courses for " + currentCourseTaken + " already have grades!");
            return;
        }

        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : ungradedCourses) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemesterOrder(), k -> new ArrayList<>())
                    .add(course);
        }

        List<Course> indexedCourses = new ArrayList<>();
        int index = 1;

        System.out.println("\nCourses without grades (" + ungradedCourses.size() + " total):");
        System.out.println("--------------------------------------------------------------");

        for (Map.Entry<Integer, Map<Integer, List<Course>>> yearEntry : yearSemGroups.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<Course>> semEntry : yearEntry.getValue().entrySet()) {
                int semesterOrder = semEntry.getKey();
                List<Course> courses = semEntry.getValue();

                String semName = courses.get(0).getSemester();
                System.out.println("\nYear " + year + " - " + semName + ":");

                for (Course course : courses) {
                    System.out.printf("  [%3d] %-12s  %-40s  %5.1f units\n",
                            index,
                            course.getCourseCode(),
                            course.getCourseTitle(),
                            course.getUnits());
                    indexedCourses.add(course);
                    index++;
                }
            }
        }

        while (true) {
            int choice = getIntInput("\nEnter course number to grade (0 to finish): ");

            if (choice == 0) {
                break;
            } else if (choice > 0 && choice <= indexedCourses.size()) {
                Course selected = indexedCourses.get(choice - 1);
                assignGradeToCourse(selected);
            } else {
                System.out.println("Invalid selection! Please choose between 1 and " + indexedCourses.size());
            }
        }

        System.out.println("\nGrade entry completed!");
    }

    private static void assignGradeToCourse(Course course) {
        System.out.println("\nSelected: " + course.getCourseCode() + " - " + course.getCourseTitle());
        System.out.print("Enter grade (50-100 for numeric, INC for Incomplete, DRP for Dropped): ");
        String grade = scanner.nextLine().trim().toUpperCase();

        if (grade.equals("INC") || grade.equals("DRP")) {
            course.setGrade(grade);
            System.out.println("Grade set to " + grade);
        } else {
            try {
                double gradeNum = Double.parseDouble(grade);
                if (gradeNum >= 50 && gradeNum <= 100) {
                    course.setGrade(grade);
                    System.out.println("Grade set to " + grade);
                } else {
                    System.out.println("Invalid grade! Must be between 50-100.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Must be numeric grade (50-100) or INC/DRP.");
            }
        }
    }

    private static void editCourse() {
        System.out.println("\n===========================================");
        System.out.println("EDIT A COURSE");
        System.out.println("===========================================");

        System.out.print("Enter course code to edit: ");
        String courseCode = scanner.nextLine().trim();

        Course course = curriculumManager.findCourse(currentStudentId, courseCode);
        if (course == null) {
            System.out.println("Course not found: " + courseCode);
            return;
        }

        displayCourseDetails(course);
        editCourseMenu(course);
    }

    private static void displayCourseDetails(Course course) {
        System.out.println("\nCurrent Course Details:");
        System.out.println("------------------------");
        System.out.println("Course Code   : " + course.getCourseCode());
        System.out.println("Course        : " + course.getCourseTaken());
        System.out.println("Year Level    : " + course.getYearLevel());
        System.out.println("Semester      : " + course.getSemester());
        System.out.println("Course Title  : " + course.getCourseTitle());
        System.out.println("Units         : " + course.getUnits());
        System.out.println("Pre-requisite : " + (course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite()));
        System.out.println("Grade         : " + course.getGrade());
        System.out.println("------------------------");
    }

    private static void editCourseMenu(Course course) {
        System.out.println("\nWhat would you like to edit?");
        System.out.println("  1. Course Title");
        System.out.println("  2. Units");
        System.out.println("  3. Pre-requisite");
        System.out.println("  4. Grade");
        System.out.println("  5. Cancel");

        int choice = getIntInput("Enter choice: ");

        switch (choice) {
            case 1:
                System.out.print("Enter new course title: ");
                String newTitle = scanner.nextLine().trim();
                if (!newTitle.isEmpty()) {
                    course.setCourseTitle(newTitle);
                    System.out.println("Course title updated successfully!");
                } else {
                    System.out.println("Course title cannot be empty!");
                }
                break;

            case 2:
                double newUnits = getDoubleInput("Enter new units: ");
                if (newUnits > 0) {
                    course.setUnits(newUnits);
                    System.out.println("Units updated successfully!");
                } else {
                    System.out.println("Units must be greater than 0!");
                }
                break;

            case 3:
                System.out.print("Enter new prerequisite (or 'None' for no prerequisite): ");
                String newPrereq = scanner.nextLine().trim();
                course.setPrerequisite(newPrereq.equalsIgnoreCase("None") ? "" : newPrereq);
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
        System.out.println("\n===========================================");
        System.out.println("SAVING CHANGES");
        System.out.println("===========================================");

        System.out.print("Do you want to save changes before quitting? (Y/N): ");
        String response = scanner.nextLine().trim().toUpperCase();

        if (response.equals("Y") || response.equals("YES")) {
            Collections.sort(allCourses);
            fileHandler.writeCoursesToCSV(allCourses, CSV_FILE);
            System.out.println("Data saved successfully to " + CSV_FILE);

            System.out.println("\nSave Summary:");
            System.out.println("--------------");
            System.out.println("Student ID: " + currentStudentId);
            System.out.println("Course: " + currentCourseTaken);
            System.out.println("Total records: " + allCourses.size());

            int studentRecords = curriculumManager.getCoursesByStudentId(currentStudentId).size();
            System.out.println("Your records: " + studentRecords);

            List<Course> graded = curriculumManager.getCoursesWithGrades(currentStudentId, currentCourseTaken);
            List<Course> ungraded = curriculumManager.getCoursesWithoutGrades(currentStudentId, currentCourseTaken);
            System.out.println("Courses with grades: " + graded.size());
            System.out.println("Courses without grades: " + ungraded.size());

            System.out.println("\nThank you for using Curriculum Management System!");
        } else {
            System.out.println("Changes not saved. Original data preserved.");
        }
    }

    private static void copyFile(String sourceFile, String destFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
             PrintWriter writer = new PrintWriter(new FileWriter(destFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
        }
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = scanner.nextLine();
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = scanner.nextLine();
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
            }
        }
    }
}