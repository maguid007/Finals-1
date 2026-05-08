import java.io.*;
import java.util.*;

public class FileHandler<T extends Course> {

    public List<Course> readCoursesFromCSV(String filename) {
        List<Course> courses = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (fields.length >= 10) {
                    try {
                        Course course = new Course(
                                fields[0].trim(),                    // SchoolId
                                Integer.parseInt(fields[1].trim()),  // YearLevel
                                fields[2].trim(),                    // Course
                                fields[3].trim(),                    // Semester
                                fields[4].trim(),                    // CourseCode
                                fields[5].trim(),                    // CourseTitle
                                Double.parseDouble(fields[6].trim()), // Units
                                fields[7].trim().replace("\"", ""),  // Prerequisite
                                fields[8].trim(),                    // Grade
                                fields[9].trim()                     // CourseTaken
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
            pw.println("SchoolId,YearLevel,Course,Semester,CourseCode,CourseTitle,Units,Prerequisite,Grade,CourseTaken");

            courses.sort(null);

            for (Course course : courses) {
                pw.printf("%s,%d,%s,%s,%s,\"%s\",%.1f,\"%s\",%s,%s%n",
                        course.getSchoolId(),
                        course.getYearLevel(),
                        course.getCourse(),
                        course.getSemester(),
                        course.getCourseCode(),
                        course.getCourseTitle(),
                        course.getUnits(),
                        course.getPrerequisite(),
                        course.getGrade(),
                        course.getCourseTaken());
            }

        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
}