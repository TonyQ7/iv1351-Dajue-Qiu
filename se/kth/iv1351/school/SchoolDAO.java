package se.kth.iv1351.school;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import se.kth.iv1351.school.SchoolModel.*;

public class SchoolDAO {
    private Connection connection;

    private PreparedStatement readInstanceStmt;
    private PreparedStatement readInstanceLockedStmt;
    private PreparedStatement updateInstanceStudentsStmt;
    private PreparedStatement readInstanceByIdStmt;
    private PreparedStatement readActivitiesByInstanceStmt;
    private PreparedStatement readAllocationsByActivityStmt;
    private PreparedStatement readTeacherStmt;
    private PreparedStatement readDistinctInstancesForTeacherStmt;
    private PreparedStatement createAllocationStmt;
    private PreparedStatement deleteAllocationStmt;
    private PreparedStatement createActivityStmt;
    private PreparedStatement checkActivityStmt;
    private PreparedStatement readAverageSalaryStmt;
    private PreparedStatement readTeacherCurrentSalaryVersionStmt;
    private PreparedStatement upsertPlannedActivityStmt;
    private PreparedStatement updateAllocationHoursStmt;

    public SchoolDAO() throws SchoolDBException {
        try {
            connect();
            prepareStatements();
        } catch (SQLException e) {
            throw new SchoolDBException("Could not connect to database.", e);
        }
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/dsp_university", "postgres",
                "postgres");
        connection.setAutoCommit(false);
    }

    public CourseInstanceDTO readCourseInstance(String courseCode, int year, String period, boolean lockExclusive)
            throws SchoolDBException {
        try {
            PreparedStatement stmt = lockExclusive ? readInstanceLockedStmt : readInstanceStmt;
            stmt.setString(1, courseCode);
            stmt.setInt(2, year);
            stmt.setString(3, period);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CourseInstanceDTO(
                            rs.getString("instance_id"),
                            rs.getString("course_code"),
                            rs.getInt("study_year"),
                            rs.getString("study_period"),
                            rs.getInt("num_students"),
                            rs.getDouble("hp"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new SchoolDBException("Could not read course instance", e);
        }
    }

    public CourseInstanceDTO readCourseInstanceById(String instanceId) throws SchoolDBException {
        try {
            readInstanceByIdStmt.setString(1, instanceId);
            try (ResultSet rs = readInstanceByIdStmt.executeQuery()) {
                if (rs.next()) {
                    return new CourseInstanceDTO(
                            rs.getString("instance_id"),
                            rs.getString("course_code"),
                            rs.getInt("study_year"),
                            rs.getString("study_period"),
                            rs.getInt("num_students"),
                            rs.getDouble("hp"));
                }
            }
            return null;
        } catch (SQLException e) {
            handleException("Could not read course instance by id", e);
            return null;
        }
    }

    public void updateCourseInstanceStudents(CourseInstanceDTO instance, int newCount) throws SchoolDBException {
        try {
            updateInstanceStudentsStmt.setInt(1, newCount);
            updateInstanceStudentsStmt.setString(2, instance.id); // Object Oriented Parameter Usage
            updateInstanceStudentsStmt.executeUpdate();
        } catch (SQLException e) {
            throw new SchoolDBException("Could not update students", e);
        }
    }

    public List<ActivityDTO> readActivitiesForInstance(String instanceId) throws SchoolDBException {
        List<ActivityDTO> activities = new ArrayList<>();
        try {
            readActivitiesByInstanceStmt.setString(1, instanceId);
            try (ResultSet rs = readActivitiesByInstanceStmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(new ActivityDTO(
                            rs.getInt("activity_id"),
                            rs.getString("activity_name"),
                            rs.getDouble("planned_hours"),
                            rs.getDouble("factor"),
                            rs.getBoolean("is_derived"),
                            rs.getDouble("const_coeff"),
                            rs.getDouble("hp_coeff"),
                            rs.getDouble("students_coeff")));
                }
            }
            return activities;
        } catch (SQLException e) {
            throw new SchoolDBException("Could not read activities", e);
        }
    }

    public List<AllocationDTO> readAllocationsForActivity(String instanceId, int activityId) throws SchoolDBException {
        List<AllocationDTO> allocations = new ArrayList<>();
        try {
            readAllocationsByActivityStmt.setString(1, instanceId);
            readAllocationsByActivityStmt.setInt(2, activityId);
            try (ResultSet rs = readAllocationsByActivityStmt.executeQuery()) {
                while (rs.next()) {
                    allocations.add(new AllocationDTO(
                            rs.getInt("allocation_id"),
                            rs.getString("name"),
                            rs.getDouble("salary_hour"),
                            rs.getDouble("allocated_hours")));
                }
            }
            return allocations;
        } catch (SQLException e) {
            throw new SchoolDBException("Could not read allocations", e);
        }
    }

    public int readTeacherInstanceCountInPeriod(TeacherDTO teacher, String period, int year) throws SchoolDBException {
        try {
            readDistinctInstancesForTeacherStmt.setInt(1, teacher.id); // Object Oriented
            readDistinctInstancesForTeacherStmt.setString(2, period);
            readDistinctInstancesForTeacherStmt.setInt(3, year);
            try (ResultSet rs = readDistinctInstancesForTeacherStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new SchoolDBException("Could not read teacher instance count", e);
        }
    }

    public void createAllocation(CourseInstanceDTO instance, int activityId, TeacherDTO teacher)
            throws SchoolDBException {
        try {
            int salaryVersionId = 0;
            readTeacherCurrentSalaryVersionStmt.setInt(1, teacher.id);
            try (ResultSet rs = readTeacherCurrentSalaryVersionStmt.executeQuery()) {
                if (rs.next())
                    salaryVersionId = rs.getInt("salary_version_id");
            }
            if (salaryVersionId == 0)
                throw new SchoolDBException("No salary history found for teacher");

            createAllocationStmt.setInt(1, teacher.id);
            createAllocationStmt.setString(2, instance.id); // Object Oriented
            createAllocationStmt.setInt(3, activityId);
            createAllocationStmt.setInt(4, salaryVersionId);
            createAllocationStmt.executeUpdate();
        } catch (SQLException e) {
            throw new SchoolDBException("Could not create allocation", e);
        }
    }

    public void createActivity(String instanceId, String type, double hours) throws SchoolDBException {
        try {
            int activityId = 0;
            checkActivityStmt.setString(1, type);
            try (ResultSet rs = checkActivityStmt.executeQuery()) {
                if (rs.next())
                    activityId = rs.getInt("activity_id");
            }
            if (activityId == 0)
                throw new SchoolDBException("Activity type " + type + " not found in DB configuration.");

            createActivityStmt.setString(1, instanceId);
            createActivityStmt.setInt(2, activityId);
            createActivityStmt.setDouble(3, hours);
            createActivityStmt.executeUpdate();
        } catch (SQLException e) {
            throw new SchoolDBException("Could not create activity", e);
        }
    }

    public void deleteAllocation(int allocationId) throws SchoolDBException {
        try {
            deleteAllocationStmt.setInt(1, allocationId);
            deleteAllocationStmt.executeUpdate();
        } catch (SQLException e) {
            throw new SchoolDBException("Could not delete allocation", e);
        }
    }

    public TeacherDTO readTeacherByName(String name) throws SchoolDBException {
        try {
            readTeacherStmt.setString(1, name);
            try (ResultSet rs = readTeacherStmt.executeQuery()) {
                if (rs.next())
                    return new TeacherDTO(rs.getInt("employee_id"), rs.getString("first_name"));
            }
            return null;
        } catch (SQLException e) {
            throw new SchoolDBException("Read teacher failed", e);
        }
    }

    public double readAverageTeacherSalary() throws SchoolDBException {
        try (ResultSet rs = readAverageSalaryStmt.executeQuery()) {
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            throw new SchoolDBException("Could not read average salary", e);
        }
        return 0.0;
    }

    public void updateAllocationHours(int allocationId, double newHours) throws SchoolDBException {
        try {
            updateAllocationHoursStmt.setDouble(1, newHours);
            updateAllocationHoursStmt.setInt(2, allocationId);
            updateAllocationHoursStmt.executeUpdate();
        } catch (SQLException e) {
            throw new SchoolDBException("Failed to update allocation hours", e);
        }
    }

    public void commit() throws SchoolDBException {
        try {
            connection.commit();
        } catch (SQLException e) {
            handleException("Failed to commit", e);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
        }
    }

    private void handleException(String msg, SQLException e) throws SchoolDBException {
        throw new SchoolDBException(msg, e);
    }

    private void prepareStatements() throws SQLException {
        readInstanceStmt = connection.prepareStatement(
                "SELECT ci.*, clv.hp FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout_version clv ON ci.course_code = clv.course_code AND ci.layout_version_no = clv.version_no "
                        +
                        "WHERE ci.course_code = ? AND ci.study_year = ? AND ci.study_period = ?");

        readInstanceByIdStmt = connection.prepareStatement(
                "SELECT ci.*, clv.hp FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout_version clv ON ci.course_code = clv.course_code AND ci.layout_version_no = clv.version_no "
                        +
                        "WHERE ci.instance_id = ?");

        readInstanceLockedStmt = connection.prepareStatement(
                "SELECT ci.*, clv.hp FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout_version clv ON ci.course_code = clv.course_code AND ci.layout_version_no = clv.version_no "
                        +
                        "WHERE ci.course_code = ? AND ci.study_year = ? AND ci.study_period = ? FOR NO KEY UPDATE");

        updateInstanceStudentsStmt = connection
                .prepareStatement("UPDATE dsp.course_instance SET num_students = ? WHERE instance_id = ?");

        readActivitiesByInstanceStmt = connection.prepareStatement(
                "SELECT pa.activity_id, ta.activity_name, pa.planned_hours, ta.factor, ta.is_derived, " +
                        "dac.const AS const_coeff, dac.hp_coeff, dac.students_coeff " +
                        "FROM dsp.planned_activity pa " +
                        "JOIN dsp.teaching_activity ta ON pa.activity_id = ta.activity_id " +
                        "LEFT JOIN dsp.derived_activity_coeffs dac ON ta.activity_id = dac.activity_id " +
                        "WHERE pa.course_instance_id = ?");

        readAllocationsByActivityStmt = connection.prepareStatement(
                "SELECT a.allocation_id, a.allocated_hours, p.first_name || ' ' || p.last_name as name, s.salary_hour "
                        +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.employee e ON a.employee_id = e.employee_id " +
                        "JOIN dsp.person p ON e.personal_number = p.personal_number " +
                        "JOIN dsp.employee_salary_history s ON a.salary_version_id = s.salary_version_id " +
                        "WHERE a.course_instance_id = ? AND a.activity_id = ? AND a.is_terminated = FALSE");

        readDistinctInstancesForTeacherStmt = connection.prepareStatement(
                "SELECT COUNT(DISTINCT ci.instance_id) " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON a.course_instance_id = ci.instance_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ? AND a.is_terminated = FALSE");

        createAllocationStmt = connection.prepareStatement(
                "INSERT INTO dsp.allocation (employee_id, course_instance_id, activity_id, salary_version_id, allocated_hours, is_terminated) "
                        +
                        "VALUES (?, ?, ?, ?, 0, FALSE) " +
                        "ON CONFLICT (employee_id, course_instance_id, activity_id) DO UPDATE SET is_terminated = FALSE");

        deleteAllocationStmt = connection
                .prepareStatement("UPDATE dsp.allocation SET is_terminated = TRUE WHERE allocation_id = ?");

        createActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.planned_activity (course_instance_id, activity_id, planned_hours) VALUES (?, ?, ?)");

        upsertPlannedActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.planned_activity (course_instance_id, activity_id, planned_hours) VALUES (?, ?, ?) " +
                        "ON CONFLICT (course_instance_id, activity_id) DO UPDATE SET planned_hours = EXCLUDED.planned_hours");

        checkActivityStmt = connection
                .prepareStatement("SELECT activity_id FROM dsp.teaching_activity WHERE activity_name = ?");

        readTeacherStmt = connection.prepareStatement(
                "SELECT e.employee_id, p.first_name FROM dsp.employee e " +
                        "JOIN dsp.person p ON e.personal_number = p.personal_number WHERE p.first_name = ?");

        readTeacherCurrentSalaryVersionStmt = connection.prepareStatement(
                "SELECT salary_version_id FROM dsp.employee_salary_history " +
                        "WHERE employee_id = ? ORDER BY version_no DESC LIMIT 1");

        readAverageSalaryStmt = connection.prepareStatement(
                "SELECT AVG(salary_hour) FROM (" +
                        "  SELECT DISTINCT ON (employee_id) salary_hour " +
                        "  FROM dsp.employee_salary_history ORDER BY employee_id, version_no DESC" +
                        ") as latest_salaries");

        updateAllocationHoursStmt = connection
                .prepareStatement("UPDATE dsp.allocation SET allocated_hours = ? WHERE allocation_id = ?");
    }
}
