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

                if (fields.length >= 7) {
                    try {
                        Course course = new Course(
                                Integer.parseInt(fields[0].trim()),
                                fields[1].trim(),
                                fields[2].trim(),
                                fields[3].trim(),
                                Double.parseDouble(fields[4].trim()),
                                fields[5].trim().replace("\"", ""),
                                fields[6].trim()
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
            pw.println("YearLevel,Course,CourseCode,CourseTitle,Units,Prerequisite,Grade");

            courses.sort(null); // Uses natural ordering (Comparable)

            for (Course course : courses) {
                pw.printf("%d,%s,%s,\"%s\",%.1f,\"%s\",%s%n",
                        course.getYearLevel(),
                        course.getCourse(),
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