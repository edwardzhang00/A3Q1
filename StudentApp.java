/**
 * Student CRUD application using JDBC against PostgreSQL.
 */

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class StudentApp {

    //JDBC and database credentials
    private static final String URL = "jdbc:postgresql://localhost:5433/students";
    private static final String USER = "postgres";
    private static final String PASSWORD = "comp3005!";
    private static final Scanner SCANNER = new Scanner(System.in);

    //closes database resources safely to prevent resource leaks checking if the resource exists before attempting to close it
    private static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) { }
        }
    }


    //READ, retrieves every row from the table and prints it to stdout
    public static void getAllStudents(Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            //statement to execute an SQL query
            stmt = conn.createStatement();

            //sql query retrieves student information and sorts it by student ID
            String sql = "SELECT student_id, first_name, last_name, email, enrollment_date " +
                    "FROM students ORDER BY student_id";
            rs = stmt.executeQuery(sql); //executes and stores result

            //headers for each column
            System.out.printf("%-4s %-10s %-12s %-28s %-12s%n",
                    "ID", "First", "Last", "Email", "Enrolled");
            System.out.println("-".repeat(70));

            //print result if there are records
            boolean any = false;
            while (rs.next()) {
                any = true;
                int id = rs.getInt("student_id");
                String first = rs.getString("first_name");
                String last = rs.getString("last_name");
                String email = rs.getString("email");
                Date date = rs.getDate("enrollment_date");

                System.out.printf("%-4d %-10s %-12s %-28s %-12s%n",
                        id, first, last, email, date);
            }
            if (!any) System.out.println("(no records)");

        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
        } finally {
            //close resources in reverse order of acquisition
            closeQuietly(rs);
            closeQuietly(stmt);
        }
    }


    //CREATE, inserts a new student
    //enrollment_date must be in "YYYY-MM-DD" format
    public static void addStudent(Connection conn, String firstName, String lastName, String email, LocalDate enrollmentDate) {

        //executes SQL commands using parameter values
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            //SQL INSERT adds a new student to the students table
            //RETURNING student_id retrieves the new student's ID
            String sql = "INSERT INTO students (first_name, last_name, email, enrollment_date) " +
                    "VALUES (?, ?, ?, ?) RETURNING student_id";
            ps = conn.prepareStatement(sql);

            //assign student information to the SQL query placeholders
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setDate(4, Date.valueOf(enrollmentDate));

            //execute the INSERT command and retrieve the new student's ID
            rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Student added with id " + rs.getInt(1) + ".");
            }

        } catch (SQLException e) {
            //23505 = unique_violation, duplicate email
            if ("23505".equals(e.getSQLState())) {
                System.out.println("Error: the email '" + email + "' is already in use.");
            } else {
                System.out.println("DB error: " + e.getMessage());
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }


    //UPDATE, changes the email of the student with the given student_id
    //executeUpdate() returns rows affected, 0 means no such id
    public static void updateStudentEmail(Connection conn, int studentId, String newEmail) {
        PreparedStatement ps = null;
        try {

            //sql update changes the email for the matching student ID
            String sql = "UPDATE students SET email = ? WHERE student_id = ?";
            ps = conn.prepareStatement(sql);

            //assign to placeholders
            ps.setString(1, newEmail);
            ps.setInt(2, studentId);

            //execute and update
            int rows = ps.executeUpdate();

            if (rows == 0) {
                System.out.println("No student found with id " + studentId + ".");
            } else {
                System.out.println("Email updated for student " + studentId + ".");
            }

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                System.out.println("Error: the email '" + newEmail + "' is already in use.");
            } else {
                System.out.println("DB error: " + e.getMessage());
            }
        } finally {
            closeQuietly(ps);
        }
    }

    //DELETE, deletes the row with the given student_id
    public static void deleteStudent(Connection conn, int studentId) {
        PreparedStatement ps = null;
        try {
            //sql delete removes the student matching with the ID
            String sql = "DELETE FROM students WHERE student_id = ?";
            ps = conn.prepareStatement(sql);

            //assign
            ps.setInt(1, studentId);

            //execute and update
            int rows = ps.executeUpdate();

            if (rows == 0) {
                System.out.println("No student found with id " + studentId + ".");
            } else {
                System.out.println("Student " + studentId + " deleted.");
            }

        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
        } finally {
            closeQuietly(ps);
        }
    }

    //helper function for main for parsing
    private static String prompt(String label) {
        System.out.print(label);
        return SCANNER.nextLine().trim();
    }

    private static Integer promptInt(String label) {
        try {
            return Integer.parseInt(prompt(label));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    //loads the driver, opens a connection, runs the menu, and closes it
    public static void main(String[] args) {

        Connection conn = null;

        //boilerplate from l12
        try {
            Class.forName("org.postgresql.Driver");
            // Connect to the database
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            if (conn != null) {
                System.out.println("Connected to PostgreSQL successfully!");
            } else {
                System.out.println("Failed to establish connection.");
                return;
            }

            //interactive menu
            while (true) {
                System.out.println();
                System.out.println("Student Database");
                System.out.println("1. Get all students");
                System.out.println("2. Add student");
                System.out.println("3. Update student email");
                System.out.println("4. Delete student");
                System.out.println("5. Quit");

                String choice = prompt("Choose an option: ");

                switch (choice) {
                    case "1" -> getAllStudents(conn);

                    case "2" -> {
                        String first = prompt("First name: ");
                        String last = prompt("Last name: ");
                        String email = prompt("Email: ");

                        LocalDate date;
                        try {
                            date = LocalDate.parse(prompt("Enrollment date (YYYY-MM-DD): "));
                        } catch (DateTimeParseException e) {
                            System.out.println("Invalid date format.");
                            continue;
                        }
                        addStudent(conn, first, last, email, date);
                    }

                    case "3" -> {
                        Integer id = promptInt("Student id: ");
                        if (id == null) { System.out.println("Must be an integer."); continue; }
                        updateStudentEmail(conn, id, prompt("New email: "));
                    }

                    case "4" -> {
                        Integer id = promptInt("Student id: ");
                        if (id == null) { System.out.println("Must be an integer."); continue; }
                        deleteStudent(conn, id);
                    }

                    case "5" -> {
                        System.out.println("Bye.");
                        return;
                    }

                    default -> System.out.println("Not a valid option.");
                }
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            //close the connection
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}