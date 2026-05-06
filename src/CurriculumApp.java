import java.util.*;

public class CurriculumApp {
    private static List<Course> allCourses = new ArrayList<>();
    private static CurriculumManager<Course> curriculumManager;
    private static FileHandler<Course> fileHandler;
    private static final String CSV_FILE = "BSCSBSIT2018.csv";
    private static final String UPDATED_FILE = "BSCSBSIT2018_updated.csv";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        curriculumManager = new CurriculumManager<>(CSV_FILE);
        fileHandler = new FileHandler<>();

        // Load data from CSV
        allCourses = fileHandler.readCoursesFromCSV(CSV_FILE);
        System.out.println("Loaded " + allCourses.size() + " courses from " + CSV_FILE);

        // Group courses by term
        curriculumManager.groupByTerm(allCourses, Course::getTermKey);

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

    private static void showSubjectsForEachTerm() {
        String program = selectProgram();
        if (program == null) return;

        System.out.println("\n===========================================");
        System.out.println("SUBJECTS FOR EACH SCHOOL TERM - " +
                (program.equals("BSIT") ? "BS INFORMATION TECHNOLOGY" : "BS COMPUTER SCIENCE"));
        System.out.println("===========================================");

        // Get courses for selected program
        List<Course> programCourses = curriculumManager.getCoursesByProgram(program);

        // Group by year and semester
        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : programCourses) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemester(), k -> new ArrayList<>())
                    .add(course);
        }

        for (Map.Entry<Integer, Map<Integer, List<Course>>> yearEntry : yearSemGroups.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<Course>> semEntry : yearEntry.getValue().entrySet()) {
                int semester = semEntry.getKey();
                List<Course> courses = semEntry.getValue();
                Collections.sort(courses);

                String semName = courses.get(0).getSemesterName();

                System.out.println("\nYear " + year + " - " + semName);
                System.out.println("----------------------------------------------------------------------------------------");
                System.out.printf("%-12s %-45s %-6s %-15s\n",
                        "Course Code", "Course Title", "Units", "Pre-requisite");
                System.out.println("----------------------------------------------------------------------------------------");

                for (Course course : courses) {
                    String prerequisite = course.getPrerequisite().isEmpty() ? "None" : course.getPrerequisite();
                    System.out.printf("%-12s %-45s %-6.1f %-15s\n",
                            course.getCourseCode(),
                            course.getCourseTitle(),
                            course.getUnits(),
                            prerequisite);
                }

                System.out.println("----------------------------------------------------------------------------------------");

                // Prompt user
                System.out.print("\n[Enter 0 to return to menu, any other key to continue]: ");
                String input = scanner.nextLine().trim();
                if (input.equals("0")) {
                    return;
                }
            }
        }
    }

    private static void showSubjectsWithGrades() {
        String program = selectProgram();
        if (program == null) return;

        System.out.println("\n===========================================");
        System.out.println("SUBJECTS WITH GRADES - " +
                (program.equals("BSIT") ? "BS INFORMATION TECHNOLOGY" : "BS COMPUTER SCIENCE"));
        System.out.println("===========================================");

        List<Course> programCourses = curriculumManager.getCoursesByProgram(program);

        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : programCourses) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemester(), k -> new ArrayList<>())
                    .add(course);
        }

        for (Map.Entry<Integer, Map<Integer, List<Course>>> yearEntry : yearSemGroups.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<Course>> semEntry : yearEntry.getValue().entrySet()) {
                int semester = semEntry.getKey();
                List<Course> courses = semEntry.getValue();
                Collections.sort(courses);

                String semName = courses.get(0).getSemesterName();

                System.out.println("\nYear " + year + " - " + semName);
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-12s %-40s %-8s %-12s\n",
                        "Course Code", "Course Title", "Grade", "Status");
                System.out.println("----------------------------------------------------------------------------------------------------");

                for (Course course : courses) {
                    String grade = course.getGrade();
                    String status = determineStatus(grade);

                    System.out.printf("%-12s %-40s %-8s %-12s\n",
                            course.getCourseCode(),
                            course.getCourseTitle(),
                            grade,
                            status);
                }

                // Show summary for this semester
                long passedCount = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("PASSED")).count();
                long failedCount = courses.stream().filter(c -> determineStatus(c.getGrade()).equals("FAILED")).count();
                long naCount = courses.stream().filter(c -> c.getGrade().equals("Not yet taken")).count();

                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("Summary: %d Total | %d Passed | %d Failed | %d N/A\n",
                        courses.size(), passedCount, failedCount, naCount);

                // Prompt user
                System.out.print("\n[Enter 0 to return to menu, any other key to continue]: ");
                String input = scanner.nextLine().trim();
                if (input.equals("0")) {
                    return;
                }
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

    private static String selectProgram() {
        System.out.println("\nSelect Program:");
        System.out.println("1. BS Information Technology (BSIT)");
        System.out.println("2. BS Computer Science (BSCS)");
        System.out.println("0. Return to Main Menu");

        int choice = getIntInput("Enter your choice: ");

        switch (choice) {
            case 1:
                return "BSIT";
            case 2:
                return "BSCS";
            case 0:
                return null;
            default:
                System.out.println("Invalid choice! Returning to main menu.");
                return null;
        }
    }

    private static void enterGrades() {
        String program = selectProgram();
        if (program == null) return;

        System.out.println("\n===========================================");
        System.out.println("ENTER GRADES FOR FINISHED SUBJECTS");
        System.out.println("===========================================");

        List<Course> ungradedCourses = curriculumManager.getCoursesWithoutGrades();
        List<Course> programUngraded = new ArrayList<>();

        for (Course course : ungradedCourses) {
            if (course.getCourse().equals(program)) {
                programUngraded.add(course);
            }
        }

        if (programUngraded.isEmpty()) {
            System.out.println("\nAll courses for " + program + " already have grades!");
            return;
        }

        // Group ungraded courses by year and semester
        Map<Integer, Map<Integer, List<Course>>> yearSemGroups = new TreeMap<>();
        for (Course course : programUngraded) {
            yearSemGroups.computeIfAbsent(course.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(course.getSemester(), k -> new ArrayList<>())
                    .add(course);
        }

        List<Course> indexedCourses = new ArrayList<>();
        int index = 1;

        System.out.println("\nCourses without grades (" + programUngraded.size() + " total):");
        System.out.println("--------------------------------------------------------------");

        for (Map.Entry<Integer, Map<Integer, List<Course>>> yearEntry : yearSemGroups.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<Integer, List<Course>> semEntry : yearEntry.getValue().entrySet()) {
                int semester = semEntry.getKey();
                List<Course> courses = semEntry.getValue();

                String semName = courses.get(0).getSemesterName();
                System.out.println("\nYear " + year + " - " + semName + ":");

                for (Course course : courses) {
                    System.out.printf("  [%3d] %-12s %-45s %5.1f units\n",
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
            System.out.print("\nEnter course number to grade (0 to finish): ");
            int choice = getIntInput("");

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
        System.out.println("Enter grade (50-100 for numeric, INC for Incomplete, DRP for Dropped):");
        System.out.print("Grade: ");
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

        Course course = curriculumManager.findCourse(courseCode);
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
        System.out.println("Program       : " + course.getCourse());
        System.out.println("Year Level    : " + course.getYearLevel());
        System.out.println("Semester      : " + course.getSemesterName());
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
                System.out.print("Enter new units: ");
                double newUnits = getDoubleInput("");
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
            fileHandler.writeCoursesToCSV(allCourses, UPDATED_FILE);
            System.out.println("Data saved successfully to " + UPDATED_FILE);

            System.out.println("\nSave Summary:");
            System.out.println("--------------");
            System.out.println("Total courses saved: " + allCourses.size());
            System.out.println("Courses with grades: " + curriculumManager.getCoursesWithGrades().size());
            System.out.println("Courses without grades: " + curriculumManager.getCoursesWithoutGrades().size());

            System.out.println("\nThank you for using Curriculum Management System!");
        } else {
            System.out.println("Changes not saved. Original data preserved.");
        }
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                if (!prompt.isEmpty()) {
                    System.out.print(prompt);
                }
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
                if (!prompt.isEmpty()) {
                    System.out.print(prompt);
                }
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                if (!prompt.isEmpty()) {
                    System.out.print(prompt);
                }
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
                if (!prompt.isEmpty()) {
                    System.out.print(prompt);
                }
            }
        }
    }
}