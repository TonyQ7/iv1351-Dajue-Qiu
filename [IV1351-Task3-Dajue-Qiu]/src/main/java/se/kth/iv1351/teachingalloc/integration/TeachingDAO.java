package se.kth.iv1351.teachingalloc.integration;

import java.sql.Connection;
// DriverManager removed - TransactionManager handles connections
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import se.kth.iv1351.teachingalloc.model.CourseInstance;
import se.kth.iv1351.teachingalloc.model.CourseInstanceDTO;
import se.kth.iv1351.teachingalloc.model.TeachingAllocation;
import se.kth.iv1351.teachingalloc.model.TeachingAllocationDTO;
import se.kth.iv1351.teachingalloc.model.TeachingActivity;
import se.kth.iv1351.teachingalloc.model.TeachingActivityDTO;
import se.kth.iv1351.teachingalloc.model.ActivityAllocation;

/**
 * This data access object (DAO) encapsulates all database calls in the teaching
 * allocation application. No code outside this class shall have any knowledge
 * about the database. All methods follow CRUD naming conventions (Create, Read,
 * Update, Delete).
 * 
 * IMPORTANT: Read methods do NOT commit. Only write methods
 * (create/update/delete) commit.
 * The Controller is responsible for calling commit() after business operations.
 */
public class TeachingDAO {
    private Connection connection;

    // Prepared statements
    private PreparedStatement readAllCourseInstancesStmt;
    private PreparedStatement readCourseInstancesByYearStmt;
    private PreparedStatement readCourseInstanceByIdStmt;
    private PreparedStatement readCourseInstanceByIdLockingStmt;
    private PreparedStatement updateCourseInstanceStudentsStmt;
    private PreparedStatement readPlannedHoursByInstanceStmt;
    private PreparedStatement readActualCostByInstanceStmt;
    private PreparedStatement readAllocationCountByEmployeePeriodLockingStmt;
    private PreparedStatement readAllocationsByEmployeePeriodStmt;
    private PreparedStatement readAllocationsByInstanceStmt;
    private PreparedStatement readAllocationsByActivityNameStmt;
    private PreparedStatement readExistingAllocationStmt;
    private PreparedStatement createAllocationStmt;
    private PreparedStatement terminateAllocationStmt;
    private PreparedStatement reactivateAllocationStmt;
    private PreparedStatement readAllTeachingActivitiesStmt;
    private PreparedStatement readActivityByNameStmt;
    private PreparedStatement createTeachingActivityStmt;
    private PreparedStatement createPlannedActivityStmt;
    private PreparedStatement readLatestSalaryVersionStmt;
    private PreparedStatement readAllocationLimitStmt;
    private PreparedStatement lockEmployeeStmt;
    private PreparedStatement readActivityByIdStmt;

    /**
     * Constructs a new DAO with the given database connection.
     * DAO instances are per-transaction and should not be reused.
     *
     * @param connection The database connection (provided by TransactionManager).
     */
    public TeachingDAO(Connection connection) throws TeachingDBException {
        try {
            this.connection = connection;
            prepareStatements();
        } catch (SQLException exception) {
            throw new TeachingDBException("Could not prepare statements.", exception);
        }
    }

    // ==================== READ METHODS (No commits!) ====================

    /**
     * Reads all existing course instances.
     */
    public List<CourseInstance> readAllCourseInstances() throws TeachingDBException {
        String failureMsg = "Could not list course instances.";
        List<CourseInstance> instances = new ArrayList<>();
        try (ResultSet result = readAllCourseInstancesStmt.executeQuery()) {
            while (result.next()) {
                instances.add(createCourseInstanceFromResultSet(result));
            }
            // NO COMMIT - Controller handles transaction
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
        return instances;
    }

    /**
     * Reads course instances for a specific study year.
     */
    public List<CourseInstance> readCourseInstancesByYear(int year) throws TeachingDBException {
        String failureMsg = "Could not list course instances for year " + year;
        List<CourseInstance> instances = new ArrayList<>();
        ResultSet result = null;
        try {
            readCourseInstancesByYearStmt.setInt(1, year);
            result = readCourseInstancesByYearStmt.executeQuery();
            while (result.next()) {
                instances.add(createCourseInstanceFromResultSet(result));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return instances;
    }

    /**
     * Reads a course instance by ID.
     */
    public CourseInstance readCourseInstanceById(String instanceId, boolean lockExclusive)
            throws TeachingDBException {
        PreparedStatement stmtToExecute = lockExclusive ? readCourseInstanceByIdLockingStmt
                : readCourseInstanceByIdStmt;

        String failureMsg = "Could not read course instance: " + instanceId;
        ResultSet result = null;
        try {
            stmtToExecute.setString(1, instanceId);
            result = stmtToExecute.executeQuery();
            if (result.next()) {
                return createCourseInstanceFromResultSet(result);
            }
            // NO COMMIT - lock held for Controller's business logic
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Reads the total planned hours for a course instance.
     * Returns RAW data - no business logic calculation here.
     */
    public BigDecimal readPlannedHoursByInstance(String instanceId) throws TeachingDBException {
        String failureMsg = "Could not read planned hours.";
        ResultSet result = null;
        try {
            readPlannedHoursByInstanceStmt.setString(1, instanceId);
            result = readPlannedHoursByInstanceStmt.executeQuery();
            if (result.next()) {
                BigDecimal totalHours = result.getBigDecimal("total_hours");
                // NO COMMIT
                return totalHours != null ? totalHours : BigDecimal.ZERO;
            }
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Reads the total actual cost for a course instance from allocations.
     * Returns RAW data from the view.
     */
    public BigDecimal readActualCostByInstance(String instanceId) throws TeachingDBException {
        String failureMsg = "Could not read actual cost.";
        ResultSet result = null;
        try {
            readActualCostByInstanceStmt.setString(1, instanceId);
            result = readActualCostByInstanceStmt.executeQuery();
            if (result.next()) {
                BigDecimal totalCost = result.getBigDecimal("total_cost");
                // NO COMMIT
                return totalCost != null ? totalCost : BigDecimal.ZERO;
            }
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Reads the count of distinct course instances an employee is allocated to
     * in a specific period and year. Uses FOR UPDATE to lock rows for the limit
     * check.
     */
    public int readAllocationCountByEmployeePeriod(int employeeId, String period, int year)
            throws TeachingDBException {
        String failureMsg = "Could not read allocation count.";
        ResultSet result = null;
        try {
            readAllocationCountByEmployeePeriodLockingStmt.setInt(1, employeeId);
            readAllocationCountByEmployeePeriodLockingStmt.setString(2, period);
            readAllocationCountByEmployeePeriodLockingStmt.setInt(3, year);
            result = readAllocationCountByEmployeePeriodLockingStmt.executeQuery();
            if (result.next()) {
                return result.getInt("instance_count");
            }
            // NO COMMIT - lock held for Controller's business logic
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return 0;
    }

    /**
     * Reads allocations for a specific employee in a period.
     */
    public List<TeachingAllocation> readAllocationsByEmployeePeriod(int employeeId, String period, int year)
            throws TeachingDBException {
        String failureMsg = "Could not read allocations.";
        List<TeachingAllocation> allocations = new ArrayList<>();
        ResultSet result = null;
        try {
            readAllocationsByEmployeePeriodStmt.setInt(1, employeeId);
            readAllocationsByEmployeePeriodStmt.setString(2, period);
            readAllocationsByEmployeePeriodStmt.setInt(3, year);
            result = readAllocationsByEmployeePeriodStmt.executeQuery();
            while (result.next()) {
                allocations.add(createAllocationFromResultSet(result));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return allocations;
    }

    /**
     * Reads all active allocations for a course instance.
     */
    public List<TeachingAllocation> readAllocationsByInstance(String instanceId) throws TeachingDBException {
        String failureMsg = "Could not read allocations for instance.";
        List<TeachingAllocation> allocations = new ArrayList<>();
        ResultSet result = null;
        try {
            readAllocationsByInstanceStmt.setString(1, instanceId);
            result = readAllocationsByInstanceStmt.executeQuery();
            while (result.next()) {
                allocations.add(createAllocationFromResultSet(result));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return allocations;
    }

    /**
     * Reads allocations for a specific activity by name (for Task A req 4).
     */
    public List<ActivityAllocation> readAllocationsByActivityName(String activityName) throws TeachingDBException {
        String failureMsg = "Could not find allocations for activity: " + activityName;
        List<ActivityAllocation> results = new ArrayList<>();
        ResultSet result = null;
        try {
            readAllocationsByActivityNameStmt.setString(1, activityName);
            result = readAllocationsByActivityNameStmt.executeQuery();
            while (result.next()) {
                results.add(new ActivityAllocation(
                        result.getInt("employee_id"),
                        result.getString("first_name") + " " + result.getString("last_name"),
                        result.getString("course_instance_id"),
                        result.getString("course_name"),
                        result.getInt("study_year"),
                        result.getString("study_period"),
                        result.getString("activity_name"),
                        result.getBigDecimal("allocated_hours")));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return results;
    }

    /**
     * Reads an existing allocation (active or terminated) for reactivation check.
     */
    public TeachingAllocation readExistingAllocation(int employeeId, String instanceId, int activityId)
            throws TeachingDBException {
        String failureMsg = "Could not check existing allocation.";
        ResultSet result = null;
        try {
            readExistingAllocationStmt.setInt(1, employeeId);
            readExistingAllocationStmt.setString(2, instanceId);
            readExistingAllocationStmt.setInt(3, activityId);
            result = readExistingAllocationStmt.executeQuery();
            if (result.next()) {
                return new TeachingAllocation(
                        result.getInt("employee_id"),
                        result.getString("course_instance_id"),
                        result.getInt("activity_id"),
                        null, // no activity name in this query
                        result.getInt("salary_version_id"),
                        result.getBigDecimal("allocated_hours"),
                        result.getBoolean("is_terminated"));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Reads all teaching activities.
     */
    public List<TeachingActivity> readAllTeachingActivities() throws TeachingDBException {
        String failureMsg = "Could not list teaching activities.";
        List<TeachingActivity> activities = new ArrayList<>();
        try (ResultSet result = readAllTeachingActivitiesStmt.executeQuery()) {
            while (result.next()) {
                activities.add(new TeachingActivity(
                        result.getInt("activity_id"),
                        result.getString("activity_name"),
                        result.getBigDecimal("factor"),
                        result.getBoolean("is_derived")));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
        return activities;
    }

    /**
     * Reads a teaching activity by name.
     */
    public TeachingActivity readActivityByName(String activityName) throws TeachingDBException {
        String failureMsg = "Could not find activity: " + activityName;
        ResultSet result = null;
        try {
            readActivityByNameStmt.setString(1, activityName);
            result = readActivityByNameStmt.executeQuery();
            if (result.next()) {
                return new TeachingActivity(
                        result.getInt("activity_id"),
                        result.getString("activity_name"),
                        result.getBigDecimal("factor"),
                        result.getBoolean("is_derived"));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Reads a teaching activity by ID.
     */
    public TeachingActivity readActivityById(int activityId) throws TeachingDBException {
        String failureMsg = "Could not find activity: " + activityId;
        ResultSet result = null;
        try {
            readActivityByIdStmt.setInt(1, activityId);
            result = readActivityByIdStmt.executeQuery();
            if (result.next()) {
                return new TeachingActivity(
                        result.getInt("activity_id"),
                        result.getString("activity_name"),
                        result.getBigDecimal("factor"),
                        result.getBoolean("is_derived"));
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Reads the latest salary version ID for an employee.
     */
    public int readLatestSalaryVersionByEmployee(int employeeId) throws TeachingDBException {
        String failureMsg = "Could not find salary version.";
        ResultSet result = null;
        try {
            readLatestSalaryVersionStmt.setInt(1, employeeId);
            result = readLatestSalaryVersionStmt.executeQuery();
            if (result.next()) {
                return result.getInt("salary_version_id");
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return 0;
    }

    /**
     * Reads the allocation limit from the allocation_rule table.
     */
    public int readAllocationLimit() throws TeachingDBException {
        String failureMsg = "Could not read allocation limit.";
        ResultSet result = null;
        try {
            result = readAllocationLimitStmt.executeQuery();
            if (result.next()) {
                return result.getInt("max_instances_per_period");
            }
            // NO COMMIT
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return 4; // Default if not configured
    }

    // ==================== WRITE METHODS (Commit after success)
    // ====================

    /**
     * Updates the number of students for a course instance.
     */
    public void updateCourseInstanceStudents(CourseInstanceDTO instance) throws TeachingDBException {
        String failureMsg = "Could not update course instance: " + instance.getInstanceId();
        try {
            updateCourseInstanceStudentsStmt.setInt(1, instance.getNumStudents());
            updateCourseInstanceStudentsStmt.setString(2, instance.getInstanceId());
            int updatedRows = updateCourseInstanceStudentsStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Creates a new allocation.
     */
    public void createAllocation(TeachingAllocationDTO allocation) throws TeachingDBException {
        String failureMsg = "Could not create allocation.";
        try {
            createAllocationStmt.setInt(1, allocation.getEmployeeId());
            createAllocationStmt.setString(2, allocation.getCourseInstanceId());
            createAllocationStmt.setInt(3, allocation.getActivityId());
            createAllocationStmt.setInt(4, allocation.getSalaryVersionId());
            createAllocationStmt.setBigDecimal(5, allocation.getAllocatedHours());
            int updatedRows = createAllocationStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Terminates an allocation (soft delete - sets is_terminated = TRUE).
     */
    public void terminateAllocation(int employeeId, String instanceId, int activityId)
            throws TeachingDBException {
        String failureMsg = "Could not terminate allocation.";
        try {
            terminateAllocationStmt.setInt(1, employeeId);
            terminateAllocationStmt.setString(2, instanceId);
            terminateAllocationStmt.setInt(3, activityId);
            int updatedRows = terminateAllocationStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Reactivates a terminated allocation (sets is_terminated = FALSE with new
     * hours/salary).
     */
    public void reactivateAllocation(int employeeId, String instanceId, int activityId,
            int salaryVersionId, BigDecimal hours) throws TeachingDBException {
        String failureMsg = "Could not reactivate allocation.";
        try {
            reactivateAllocationStmt.setInt(1, salaryVersionId);
            reactivateAllocationStmt.setBigDecimal(2, hours);
            reactivateAllocationStmt.setInt(3, employeeId);
            reactivateAllocationStmt.setString(4, instanceId);
            reactivateAllocationStmt.setInt(5, activityId);
            int updatedRows = reactivateAllocationStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Creates a new teaching activity.
     */
    public void createTeachingActivity(TeachingActivityDTO activity) throws TeachingDBException {
        String failureMsg = "Could not create teaching activity.";
        try {
            createTeachingActivityStmt.setString(1, activity.getActivityName());
            createTeachingActivityStmt.setBigDecimal(2, activity.getFactor());
            createTeachingActivityStmt.setBoolean(3, activity.isDerived());
            int updatedRows = createTeachingActivityStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Creates a planned activity for a course instance.
     */
    public void createPlannedActivity(String instanceId, int activityId, BigDecimal plannedHours)
            throws TeachingDBException {
        String failureMsg = "Could not create planned activity.";
        try {
            createPlannedActivityStmt.setString(1, instanceId);
            createPlannedActivityStmt.setInt(2, activityId);
            createPlannedActivityStmt.setBigDecimal(3, plannedHours);
            int updatedRows = createPlannedActivityStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    // commit() removed - TransactionManager handles transactions

    // ==================== PRIVATE HELPERS ====================

    // connectToTeachingDB() removed - TransactionManager provides connection

    private void prepareStatements() throws SQLException {
        // Read all course instances
        readAllCourseInstancesStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "ORDER BY ci.study_year DESC, ci.study_period, ci.course_code");

        // Read course instances by year
        readCourseInstancesByYearStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.study_year = ? " +
                        "ORDER BY ci.study_period, ci.course_code");

        // Read course instance by ID
        readCourseInstanceByIdStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.instance_id = ?");

        // Read course instance by ID with lock
        readCourseInstanceByIdLockingStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.instance_id = ? FOR NO KEY UPDATE OF ci");

        // Update student count
        updateCourseInstanceStudentsStmt = connection.prepareStatement(
                "UPDATE dsp.course_instance SET num_students = ? WHERE instance_id = ?");

        // Read planned hours (sum of effective hours from view)
        readPlannedHoursByInstanceStmt = connection.prepareStatement(
                "SELECT SUM(effective_hours) AS total_hours " +
                        "FROM dsp.v_activity_hours WHERE course_instance_id = ?");

        // Read actual cost from allocations (view already filters terminated)
        readActualCostByInstanceStmt = connection.prepareStatement(
                "SELECT SUM(cost) AS total_cost FROM dsp.v_allocation_cost " +
                        "WHERE course_instance_id = ?");

        // Read allocation count by employee and period (Locking handled by parent lock)
        readAllocationCountByEmployeePeriodLockingStmt = connection.prepareStatement(
                "SELECT COUNT(DISTINCT a.course_instance_id) AS instance_count " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ? " +
                        "AND a.is_terminated = FALSE");

        // Read allocations by employee and period (excludes terminated)
        readAllocationsByEmployeePeriodStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, a.is_terminated, ta.activity_name " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ? " +
                        "AND a.is_terminated = FALSE");

        // Read allocations by instance (excludes terminated)
        readAllocationsByInstanceStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, a.is_terminated, ta.activity_name " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "WHERE a.course_instance_id = ? AND a.is_terminated = FALSE");

        // Read allocations by activity name (for Exercise query, excludes terminated)
        readAllocationsByActivityNameStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, ta.activity_name, " +
                        "cl.course_name, p.first_name, p.last_name, ci.study_year, ci.study_period " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "JOIN dsp.employee e ON e.employee_id = a.employee_id " +
                        "JOIN dsp.person p ON p.personal_number = e.personal_number " +
                        "WHERE LOWER(ta.activity_name) = LOWER(?) AND a.is_terminated = FALSE");

        // Read existing allocation (includes terminated for reactivation check)
        readExistingAllocationStmt = connection.prepareStatement(
                "SELECT employee_id, course_instance_id, activity_id, salary_version_id, " +
                        "allocated_hours, is_terminated " +
                        "FROM dsp.allocation " +
                        "WHERE employee_id = ? AND course_instance_id = ? AND activity_id = ?");

        // Create allocation
        createAllocationStmt = connection.prepareStatement(
                "INSERT INTO dsp.allocation (employee_id, course_instance_id, activity_id, " +
                        "salary_version_id, allocated_hours) VALUES (?, ?, ?, ?, ?)");

        // Terminate allocation (soft delete)
        terminateAllocationStmt = connection.prepareStatement(
                "UPDATE dsp.allocation SET is_terminated = TRUE " +
                        "WHERE employee_id = ? AND course_instance_id = ? AND activity_id = ?");

        // Reactivate allocation
        reactivateAllocationStmt = connection.prepareStatement(
                "UPDATE dsp.allocation SET is_terminated = FALSE, salary_version_id = ?, allocated_hours = ? " +
                        "WHERE employee_id = ? AND course_instance_id = ? AND activity_id = ?");

        // Read all teaching activities
        readAllTeachingActivitiesStmt = connection.prepareStatement(
                "SELECT activity_id, activity_name, factor, is_derived " +
                        "FROM dsp.teaching_activity ORDER BY activity_id");

        // Read activity by name
        readActivityByNameStmt = connection.prepareStatement(
                "SELECT activity_id, activity_name, factor, is_derived " +
                        "FROM dsp.teaching_activity WHERE LOWER(activity_name) = LOWER(?)");

        // Read activity by ID
        readActivityByIdStmt = connection.prepareStatement(
                "SELECT activity_id, activity_name, factor, is_derived " +
                        "FROM dsp.teaching_activity WHERE activity_id = ?");

        // Create teaching activity
        createTeachingActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.teaching_activity (activity_name, factor, is_derived) " +
                        "VALUES (?, ?, ?)");

        // Create planned activity
        createPlannedActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.planned_activity (course_instance_id, activity_id, planned_hours) " +
                        "VALUES (?, ?, ?)");

        // Read latest salary version for employee
        readLatestSalaryVersionStmt = connection.prepareStatement(
                "SELECT salary_version_id FROM dsp.employee_salary_history " +
                        "WHERE employee_id = ? ORDER BY version_no DESC LIMIT 1");

        // Read allocation limit from database
        readAllocationLimitStmt = connection.prepareStatement(
                "SELECT max_instances_per_period FROM dsp.allocation_rule LIMIT 1");

        // Lock employee row for serializing allocation operations
        lockEmployeeStmt = connection.prepareStatement(
                "SELECT employee_id FROM dsp.employee WHERE employee_id = ? FOR NO KEY UPDATE");
    }

    /**
     * Reads and locks the specified employee row to serialize allocation
     * operations.
     * Uses FOR NO KEY UPDATE to prevent phantom reads when checking allocation
     * limits.
     * Must be called BEFORE reading allocation counts.
     *
     * @param employeeId The employee to lock.
     * @throws TeachingDBException If employee not found or database error.
     */
    public void ReadEmployeeForUpdate(int employeeId) throws TeachingDBException {
        String failureMsg = "Could not lock employee: " + employeeId;
        ResultSet result = null;
        try {
            lockEmployeeStmt.setInt(1, employeeId);
            result = lockEmployeeStmt.executeQuery();
            if (!result.next()) {
                throw new TeachingDBException("Employee not found: " + employeeId);
            }
            // Lock acquired, held until transaction completes
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
    }

    private CourseInstance createCourseInstanceFromResultSet(ResultSet result) throws SQLException {
        return new CourseInstance(
                result.getString("instance_id"),
                result.getString("course_code"),
                result.getString("course_name"),
                result.getInt("study_year"),
                result.getString("study_period"),
                result.getInt("num_students"),
                result.getInt("layout_version_no"));
    }

    private TeachingAllocation createAllocationFromResultSet(ResultSet result) throws SQLException {
        return new TeachingAllocation(
                result.getInt("employee_id"),
                result.getString("course_instance_id"),
                result.getInt("activity_id"),
                result.getString("activity_name"),
                result.getInt("salary_version_id"),
                result.getBigDecimal("allocated_hours"),
                result.getBoolean("is_terminated"));
    }

    private void handleException(String failureMsg, Exception cause) throws TeachingDBException {
        String completeFailureMsg = failureMsg;
        try {
            connection.rollback();
        } catch (SQLException rollbackExc) {
            completeFailureMsg = completeFailureMsg +
                    ". Also failed to rollback transaction because of: " + rollbackExc.getMessage();
        }

        if (cause != null) {
            throw new TeachingDBException(failureMsg, cause);
        } else {
            throw new TeachingDBException(failureMsg);
        }
    }

    private void closeResultSet(String failureMsg, ResultSet result) throws TeachingDBException {
        if (result != null) {
            try {
                result.close();
            } catch (Exception e) {
                throw new TeachingDBException(failureMsg + " Could not close result set.", e);
            }
        }
    }
}
