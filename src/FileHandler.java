import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler<T extends Course> {

    public List<Course> readCoursesFromCSV(String filename) {
        List<Course> courses = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (fields.length >= 8) {  // Now expecting 8 fields with the Semester column
                    try {
                        Course course = new Course(
                                Integer.parseInt(fields[0].trim()),  // YearLevel
                                fields[1].trim(),                     // Course (BSIT/BSCS)
                                fields[2].trim(),                     // Semester
                                fields[3].trim(),                     // CourseCode
                                fields[4].trim(),                     // CourseTitle
                                Double.parseDouble(fields[5].trim()), // Units
                                fields[6].trim().replace("\"", ""),   // Prerequisite
                                fields[7].trim()                      // Grade
                        );
                        courses.add(course);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing line: " + line);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return courses;
    }

    public void writeCoursesToCSV(List<Course> courses, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("YearLevel,Course,Semester,CourseCode,CourseTitle,Units,Prerequisite,Grade");

            courses.sort(null); // Uses natural ordering (Comparable)

            for (Course course : courses) {
                pw.printf("%d,%s,%s,%s,\"%s\",%.1f,\"%s\",%s%n",
                        course.getYearLevel(),
                        course.getCourse(),
                        course.getSemester(),
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        course.getUnits(),
                        course.getPrerequisite(),
                        course.getGrade());
            }

        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
}