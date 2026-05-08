import java.util.*;
import java.io.*;
import java.util.stream.*;

public class CurriculumApp {
    private static List<Course> allCourses = new ArrayList<>();
    private static CurriculumManager<Course> curriculumManager;
    private static FileHandler<Course> fileHandler;
    private static final String CSV_FILE = "BSITBSCSRecords.csv";
    private static final String BACKUP_FILE = "BSITBSCSRecords_backup.csv";
    private static String currentStudentId, currentCourseTaken;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        fileHandler = new FileHandler<>();
        curriculumManager = new CurriculumManager<>(CSV_FILE);
        createBackup();
        loadAllRecords();

        if (!loginWithStudentId()) {
            System.out.println("Exiting program. Goodbye!");
            return;
        }

        boolean running = true;
        while (running) {
            displayMenu();
            switch (getIntInput("Enter your choice: ")) {
                case 1: showSubjects(false); break;
                case 2: showSubjects(true); break;
                case 3: enterGrades(); break;
                case 4: editCourse(); break;
                case 5: switchCourse(); break;
                case 6: saveAndQuit(); running = false; break;
                default: System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void createBackup() {
        try { copyFile(CSV_FILE, BACKUP_FILE); System.out.println("Backup created: " + BACKUP_FILE); }
        catch (IOException e) { System.out.println("Warning: Could not create backup file."); }
    }

    private static void loadAllRecords() {
        allCourses = fileHandler.readCoursesFromCSV(CSV_FILE);
        System.out.println("Loaded " + allCourses.size() + " total records from " + CSV_FILE);
        curriculumManager.groupByTerm(allCourses, Course::getTermKey);
    }

    private static boolean loginWithStudentId() {
        printHeader("CURRICULUM MANAGEMENT SYSTEM", "STUDENT LOGIN");

        while (true) {
            System.out.print("\nEnter Student ID (or 0 to exit): ");
            String studentId = scanner.nextLine().trim();
            if (studentId.equals("0")) return false;
            if (studentId.isEmpty()) { System.out.println("Student ID cannot be empty. Please try again."); continue; }

            List<Course> studentCourses = curriculumManager.getCoursesByStudentId(studentId);
            if (!studentCourses.isEmpty()) {
                currentStudentId = studentId;
                currentCourseTaken = studentCourses.get(0).getCourseTaken();
                System.out.println("\nLogin successful!\nStudent ID: " + currentStudentId +
                        "\nCourse: " + currentCourseTaken + getCourseFullName(currentCourseTaken) +
                        "\nTotal records loaded: " + studentCourses.size());
                return true;
            }
            System.out.println("\nError: No records found for Student ID: " + studentId);
            listAvailableStudentIds();
        }
    }

    private static String getCourseFullName(String course) {
        return course.equals("BSIT") ? " (BS Information Technology)" : " (BS Computer Science)";
    }

    private static void listAvailableStudentIds() {
        System.out.println("\nAvailable Student IDs:\n----------------------------------------");
        allCourses.stream().map(Course::getSchoolId).collect(Collectors.toSet()).stream().sorted()
                .forEach(id -> System.out.println("  " + id + " (" + curriculumManager.getStudentCourse(id) + ")"));
        System.out.println("----------------------------------------");
    }

    private static void displayMenu() {
        printHeader("CURRICULUM MANAGEMENT SYSTEM", "Student ID: " + currentStudentId, "Course: " + currentCourseTaken);
        System.out.println("1. Show subjects for each school term\n2. Show subjects with grades for each term");
        System.out.println("3. Enter grades for subjects recently finished\n4. Edit a course\n5. Switch Course\n6. Save and Quit");
        System.out.println("===========================================");
    }

    private static void switchCourse() {
        printHeader("SWITCH COURSE");
        System.out.println("Current Course: " + currentCourseTaken + "\n\nAvailable Courses:");
        System.out.println("1. BS Information Technology (BSIT)\n2. BS Computer Science (BSCS)\n0. Cancel");

        int choice = getIntInput("Enter your choice: ");
        if (choice == 0) return;

        String newCourse = choice == 1 ? "BSIT" : choice == 2 ? "BSCS" : null;
        if (newCourse == null) { System.out.println("Invalid choice!"); return; }
        if (newCourse.equals(currentCourseTaken)) { System.out.println("You are already enrolled in " + currentCourseTaken + "."); return; }

        if (!curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, newCourse).isEmpty()) {
            currentCourseTaken = newCourse;
            System.out.println("\nYou already have records in " + newCourse + ".\nSwitching to " + newCourse + "...");
            System.out.println("Course switched successfully! Grades from previous course are retained.");
        } else {
            System.out.print("\nSwitching to " + newCourse + ". This will copy the curriculum structure.\nDo you want to proceed? (Y/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                copyCurriculumToNewCourse(newCourse);
                currentCourseTaken = newCourse;
                System.out.println("Course switched successfully!");
            } else System.out.println("Course switch cancelled.");
        }
    }

    private static void copyCurriculumToNewCourse(String newCourse) {
        List<Course> currentCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, currentCourseTaken);
        Map<String, String> gradeMap = currentCourses.stream()
                .filter(c -> !c.getGrade().equals("Not yet taken"))
                .collect(Collectors.toMap(Course::getCourseCode, Course::getGrade, (a, b) -> a));

        List<Course> templateCourses = allCourses.stream()
                .filter(c -> c.getCourseTaken().equals(newCourse) && !c.getSchoolId().equals(currentStudentId))
                .collect(Collectors.toMap(c -> c.getCourseCode() + c.getYearLevel() + c.getSemester(), c -> c, (a, b) -> a))
                .values().stream().collect(Collectors.toList());

        List<Course> newCourses = templateCourses.stream()
                .map(t -> new Course(currentStudentId, t.getYearLevel(), newCourse, t.getSemester(),
                        t.getCourseCode(), t.getCourseTitle(), t.getUnits(), t.getPrerequisite(),
                        gradeMap.getOrDefault(t.getCourseCode(), "Not yet taken"), newCourse))
                .collect(Collectors.toList());

        allCourses.addAll(newCourses);
        curriculumManager.groupByTerm(allCourses, Course::getTermKey);
        System.out.println("Added " + newCourses.size() + " courses for " + newCourse);
        if (!gradeMap.isEmpty()) System.out.println("Preserved " + gradeMap.size() + " grades from previous course.");
    }

    private static void showSubjects(boolean withGrades) {
        List<Course> studentCourses = curriculumManager.getCoursesByStudentIdAndProgram(currentStudentId, currentCourseTaken);
        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = groupCoursesByYearSem(studentCourses);

        if (yearSemGroups.isEmpty()) { System.out.println("\nNo courses found for " + currentCourseTaken + "."); return; }

        int yearChoice = selectYear(yearSemGroups);
        if (yearChoice == 0) return;
        if (!yearSemGroups.containsKey(yearChoice)) { System.out.println("Invalid year level! Returning to main menu."); return; }

        displaySemesters(yearSemGroups.get(yearChoice), withGrades);
    }

    private static Map<Integer, Map<Integer, List<Course>>> groupCoursesByYearSem(List<Course> courses) {
        Map<Integer, Map<Integer, List<Course>>> groups = new TreeMap<>();
        courses.forEach(c -> groups.computeIfAbsent(c.getYearLevel(), k -> new TreeMap<>())
                .computeIfAbsent(c.getSemesterOrder(), k -> new ArrayList<>()).add(c));
        return groups;
    }

    private static int selectYear(Map<Integer, Map<Integer, List<Course>>> yearSemGroups) {
        System.out.println("\nSelect Year Level:");
        yearSemGroups.keySet().forEach(y -> System.out.println(y + ". Year " + y));
        System.out.println("0. Return to Main Menu");
        return getIntInput("Enter your choice: ");
    }

    private static void displaySemesters(Map<Integer, List<Course>> selectedYear, boolean withGrades) {
        List<Integer> semesters = new ArrayList<>(selectedYear.keySet());
        Collections.sort(semesters);

        for (int i = 0; i < semesters.size(); i++) {
            List<Course> courses = selectedYear.get(semesters.get(i));
            Collections.sort(courses);
            String semName = courses.get(0).getSemester();

            printHeader("Year " + courses.get(0).getYearLevel() + " - " + semName + " - " + currentCourseTaken);

            if (withGrades) displayGradesTable(courses);
            else displaySubjectsTable(courses);

            if (i < semesters.size() - 1) System.out.print("\nPress ENTER for next semester or type 0 to return to menu: ");
            else System.out.print("\nPress ENTER to return to menu: ");

            if (scanner.nextLine().trim().equals("0")) return;
        }
    }

    private static void displaySubjectsTable(List<Course> courses) {
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("%-12s  %-40s  %5s  %-10s\n", "Course Code", "Course Title", "Units", "Pre-req");
        System.out.println("-------------------------------------------------------------------------");
        courses.forEach(c -> System.out.printf("%-12s  %-40s  %5.1f  %-10s\n",
                c.getCourseCode(), truncate(c.getCourseTitle(), 40), c.getUnits(), truncate(c.getPrerequisite().isEmpty() ? "None" : c.getPrerequisite(), 10)));
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Total Courses: " + courses.size() + " | Total Units: " +
                String.format("%.1f", courses.stream().mapToDouble(Course::getUnits).sum()));
    }

    private static void displayGradesTable(List<Course> courses) {
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("%-12s  %-35s  %6s  %-10s\n", "Course Code", "Course Title", "Grade", "Status");
        System.out.println("---------------------------------------------------------------------------------");
        courses.forEach(c -> System.out.printf("%-12s  %-35s  %6s  %-10s\n",
                c.getCourseCode(), truncate(c.getCourseTitle(), 35), c.getGrade(), determineStatus(c.getGrade())));
        long passed = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("PASSED")).count();
        long failed = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("FAILED")).count();
        long na = courses.stream().filter(c -> c.getGrade().equals("Not yet taken")).count();
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("Summary: %d Total | %d Passed | %d Failed | %d N/A\n", courses.size(), passed, failed, na);
    }

    private static String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }

    private static String determineStatus(String grade) {
        switch (grade) {
            case "Not yet taken": return "N/A";
            case "INC": case "DRP": return grade;
            default:
                try { return Double.parseDouble(grade) >= 75 ? "PASSED" : "FAILED"; }
                catch (NumberFormatException e) { return "Invalid"; }
        }
    }

    private static void enterGrades() {
        printHeader("ENTER GRADES FOR FINISHED SUBJECTS");
        List<Course> ungraded = curriculumManager.getCoursesWithoutGrades(currentStudentId, currentCourseTaken);

        if (ungraded.isEmpty()) { System.out.println("\nAll courses for " + currentCourseTaken + " already have grades!"); return; }

        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = groupCoursesByYearSem(ungraded);
        List<Course> indexed = new ArrayList<>();
        int index = 1;

        System.out.println("\nCourses without grades (" + ungraded.size() + " total):\n--------------------------------------------------------------");
        for (Map.Entry<Integer, Map<Integer, List<Course>>> yEntry : yearSemGroups.entrySet()) {
            for (Map.Entry<Integer, List<Course>> sEntry : yEntry.getValue().entrySet()) {
                System.out.println("\nYear " + yEntry.getKey() + " - " + sEntry.getValue().get(0).getSemester() + ":");
                for (Course c : sEntry.getValue()) {
                    System.out.printf("  [%3d] %-12s  %-40s  %5.1f units\n", index++, c.getCourseCode(), c.getCourseTitle(), c.getUnits());
                    indexed.add(c);
                }
            }
        }

        while (true) {
            int choice = getIntInput("\nEnter course number to grade (0 to finish): ");
            if (choice == 0) break;
            if (choice > 0 && choice <= indexed.size()) assignGradeToCourse(indexed.get(choice - 1));
            else System.out.println("Invalid selection! Please choose between 1 and " + indexed.size());
        }
        System.out.println("\nGrade entry completed!");
    }

    private static void assignGradeToCourse(Course course) {
        System.out.println("\nSelected: " + course.getCourseCode() + " - " + course.getCourseTitle());
        System.out.print("Enter grade (50-100 for numeric, INC for Incomplete, DRP for Dropped): ");
        String grade = scanner.nextLine().trim().toUpperCase();

        if (grade.matches("INC|DRP")) { course.setGrade(grade); System.out.println("Grade set to " + grade); return; }
        try {
            double g = Double.parseDouble(grade);
            if (g >= 50 && g <= 100) { course.setGrade(grade); System.out.println("Grade set to " + grade); }
            else System.out.println("Invalid grade! Must be between 50-100.");
        } catch (NumberFormatException e) { System.out.println("Invalid input! Must be numeric grade (50-100) or INC/DRP."); }
    }

    private static void editCourse() {
        printHeader("EDIT A COURSE");
        System.out.print("Enter course code to edit: ");
        Course course = curriculumManager.findCourse(currentStudentId, scanner.nextLine().trim());

        if (course == null) { System.out.println("Course not found!"); return; }

        System.out.println("\nCurrent Course Details:\n------------------------");
        System.out.printf("Course Code: %s\nCourse: %s\nYear Level: %d\nSemester: %s\nCourse Title: %s\nUnits: %.1f\nPre-requisite: %s\nGrade: %s\n------------------------\n",
                course.getCourseCode(), course.getCourseTaken(), course.getYearLevel(), course.getSemester(),
                course.getCourseTitle(), course.getUnits(), course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite(), course.getGrade());

        System.out.println("\nWhat would you like to edit?\n  1. Course Title\n  2. Units\n  3. Pre-requisite\n  4. Grade\n  5. Cancel");
        switch (getIntInput("Enter choice: ")) {
            case 1: updateField("Enter new course title: ", v -> { if (!v.isEmpty()) course.setCourseTitle(v); return !v.isEmpty(); }, "Course title"); break;
            case 2: updateNumeric("Enter new units: ", v -> v > 0, course::setUnits, "Units"); break;
            case 3: System.out.print("Enter new prerequisite: "); course.setPrerequisite(scanner.nextLine().trim().replaceAll("(?i)None", "")); System.out.println("Prerequisite updated!"); break;
            case 4: System.out.print("Enter new grade: "); course.setGrade(scanner.nextLine().trim()); System.out.println("Grade updated!"); break;
            case 5: System.out.println("Edit cancelled."); break;
            default: System.out.println("Invalid choice!");
        }
    }

    private static void updateField(String prompt, java.util.function.Predicate<String> validator, String fieldName) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        System.out.println(validator.test(value) ? fieldName + " updated successfully!" : fieldName + " cannot be empty!");
    }

    private static void updateNumeric(String prompt, java.util.function.DoublePredicate validator, java.util.function.DoubleConsumer setter, String fieldName) {
        double value = getDoubleInput(prompt);
        System.out.println(validator.test(value) ? fieldName + " updated successfully!" : fieldName + " must be greater than 0!");
        if (validator.test(value)) setter.accept(value);
    }

    private static void saveAndQuit() {
        printHeader("SAVING CHANGES");
        System.out.print("Do you want to save changes before quitting? (Y/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) { System.out.println("Changes not saved."); return; }

        Collections.sort(allCourses);
        fileHandler.writeCoursesToCSV(allCourses, CSV_FILE);
        System.out.println("Data saved to " + CSV_FILE + "\n\nSave Summary:\n--------------\nStudent ID: " + currentStudentId +
                "\nCourse: " + currentCourseTaken + "\nTotal records: " + allCourses.size() +
                "\nYour records: " + curriculumManager.getCoursesByStudentId(currentStudentId).size() +
                "\nCourses with grades: " + curriculumManager.getCoursesWithGrades(currentStudentId, currentCourseTaken).size() +
                "\nCourses without grades: " + curriculumManager.getCoursesWithoutGrades(currentStudentId, currentCourseTaken).size() +
                "\n\nThank you for using Curriculum Management System!");
    }

    private static void copyFile(String source, String dest) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(source)); PrintWriter w = new PrintWriter(new FileWriter(dest))) {
            r.lines().forEach(w::println);
        }
    }

    private static void printHeader(String... lines) {
        System.out.println("\n===========================================");
        for (String line : lines) System.out.println("    " + line);
        System.out.println("===========================================");
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try { System.out.print(prompt); return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Invalid input! Please enter a valid number."); }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try { System.out.print(prompt); return Double.parseDouble(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Invalid input! Please enter a valid number."); }
        }
    }
}