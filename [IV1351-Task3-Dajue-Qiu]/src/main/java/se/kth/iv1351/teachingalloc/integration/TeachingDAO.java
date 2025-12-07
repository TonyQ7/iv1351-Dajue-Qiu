/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package se.kth.iv1351.teachingalloc.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import se.kth.iv1351.teachingalloc.model.CourseInstance;
import se.kth.iv1351.teachingalloc.model.CourseInstanceDTO;
import se.kth.iv1351.teachingalloc.model.TeachingAllocation;
import se.kth.iv1351.teachingalloc.model.TeachingAllocationDTO;
import se.kth.iv1351.teachingalloc.model.TeachingActivity;
import se.kth.iv1351.teachingalloc.model.TeachingActivityDTO;

/**
 * This data access object (DAO) encapsulates all database calls in the teaching
 * allocation application. No code outside this class shall have any knowledge
 * about the database. All methods follow CRUD naming conventions.
 */
public class TeachingDAO {
    private Connection connection;

    // Prepared statements
    private PreparedStatement findAllCourseInstancesStmt;
    private PreparedStatement findCourseInstancesByYearStmt;
    private PreparedStatement findCourseInstanceByIdStmt;
    private PreparedStatement findCourseInstanceByIdLockingStmt;
    private PreparedStatement updateCourseInstanceStudentsStmt;
    private PreparedStatement findPlannedCostByInstanceStmt;
    private PreparedStatement findActualCostByInstanceStmt;
    private PreparedStatement findAllocationsByEmployeePeriodStmt;
    private PreparedStatement findAllocationsByEmployeePeriodLockingStmt;
    private PreparedStatement findAllocationsByInstanceStmt;
    private PreparedStatement findAllocationsByActivityNameStmt;
    private PreparedStatement createAllocationStmt;
    private PreparedStatement deleteAllocationStmt;
    private PreparedStatement findAllTeachingActivitiesStmt;
    private PreparedStatement findActivityByNameStmt;
    private PreparedStatement createTeachingActivityStmt;
    private PreparedStatement createPlannedActivityStmt;
    private PreparedStatement findLatestSalaryVersionStmt;
    private PreparedStatement findCourseInstanceCountByEmployeePeriodStmt;

    /**
     * Constructs a new DAO object connected to the teaching database.
     */
    public TeachingDAO() throws TeachingDBException {
        try {
            connectToTeachingDB();
            prepareStatements();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new TeachingDBException("Could not connect to datasource.", exception);
        }
    }

    /**
     * Retrieves all existing course instances.
     *
     * @return A list with all existing course instances.
     * @throws TeachingDBException If failed to search for course instances.
     */
    public List<CourseInstance> findAllCourseInstances() throws TeachingDBException {
        String failureMsg = "Could not list course instances.";
        List<CourseInstance> instances = new ArrayList<>();
        try (ResultSet result = findAllCourseInstancesStmt.executeQuery()) {
            while (result.next()) {
                instances.add(createCourseInstanceFromResultSet(result));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
        return instances;
    }

    /**
     * Searches for the course instance with the specified ID.
     *
     * @param instanceId    The instance ID.
     * @param lockExclusive If true, acquires FOR NO KEY UPDATE lock.
     * @return The course instance, or null if not found.
     * @throws TeachingDBException If failed to search.
     */
    public CourseInstance findCourseInstanceById(String instanceId, boolean lockExclusive)
            throws TeachingDBException {
        PreparedStatement stmtToExecute = lockExclusive ? findCourseInstanceByIdLockingStmt
                : findCourseInstanceByIdStmt;

        String failureMsg = "Could not search for specified course instance.";
        ResultSet result = null;
        try {
            stmtToExecute.setString(1, instanceId);
            result = stmtToExecute.executeQuery();
            if (result.next()) {
                return createCourseInstanceFromResultSet(result);
            }
            if (!lockExclusive) {
                connection.commit();
            }
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Updates the number of students for a course instance.
     *
     * @param instance The course instance to update.
     * @throws TeachingDBException If unable to update.
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
     * Reads the planned cost for a course instance using the v_activity_hours view.
     *
     * @param instanceId       The course instance ID.
     * @param avgSalaryPerHour Average salary per hour for cost calculation.
     * @return The planned cost in KSEK.
     * @throws TeachingDBException If failed to read.
     */
    public BigDecimal findPlannedCostByInstance(String instanceId, BigDecimal avgSalaryPerHour)
            throws TeachingDBException {
        String failureMsg = "Could not read planned cost.";
        ResultSet result = null;
        try {
            findPlannedCostByInstanceStmt.setString(1, instanceId);
            result = findPlannedCostByInstanceStmt.executeQuery();
            if (result.next()) {
                BigDecimal totalHours = result.getBigDecimal("total_hours");
                if (totalHours != null) {
                    // Cost in KSEK = (hours * salary) / 1000
                    BigDecimal cost = totalHours.multiply(avgSalaryPerHour)
                            .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                    connection.commit();
                    return cost;
                }
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Reads the actual cost for a course instance from allocations.
     *
     * @param instanceId The course instance ID.
     * @return The actual cost in KSEK.
     * @throws TeachingDBException If failed to read.
     */
    public BigDecimal findActualCostByInstance(String instanceId) throws TeachingDBException {
        String failureMsg = "Could not read actual cost.";
        ResultSet result = null;
        try {
            findActualCostByInstanceStmt.setString(1, instanceId);
            result = findActualCostByInstanceStmt.executeQuery();
            if (result.next()) {
                BigDecimal totalCost = result.getBigDecimal("total_cost");
                if (totalCost != null) {
                    // Convert to KSEK
                    BigDecimal costKSEK = totalCost.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                    connection.commit();
                    return costKSEK;
                }
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Reads the count of distinct course instances an employee is allocated to
     * in a specific period and year.
     *
     * @param employeeId    The employee ID.
     * @param period        The study period (e.g., "P1").
     * @param year          The study year.
     * @param lockExclusive If true, acquires lock for subsequent update.
     * @return The count of distinct course instances.
     * @throws TeachingDBException If failed to read.
     */
    public int findCourseInstanceCountByEmployeePeriod(int employeeId, String period, int year,
            boolean lockExclusive) throws TeachingDBException {

        String failureMsg = "Could not read allocation count.";
        ResultSet result = null;
        try {
            findCourseInstanceCountByEmployeePeriodStmt.setInt(1, employeeId);
            findCourseInstanceCountByEmployeePeriodStmt.setString(2, period);
            findCourseInstanceCountByEmployeePeriodStmt.setInt(3, year);
            result = findCourseInstanceCountByEmployeePeriodStmt.executeQuery();
            if (result.next()) {
                int count = result.getInt("instance_count");
                if (!lockExclusive) {
                    connection.commit();
                }
                return count;
            }
            if (!lockExclusive) {
                connection.commit();
            }
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return 0;
    }

    /**
     * Reads allocations for a specific employee in a period.
     *
     * @param employeeId The employee ID.
     * @param period     The study period.
     * @param year       The study year.
     * @return List of allocations.
     * @throws TeachingDBException If failed to read.
     */
    public List<TeachingAllocation> findAllocationsByEmployeePeriod(int employeeId, String period, int year)
            throws TeachingDBException {
        String failureMsg = "Could not read allocations.";
        List<TeachingAllocation> allocations = new ArrayList<>();
        ResultSet result = null;
        try {
            findAllocationsByEmployeePeriodStmt.setInt(1, employeeId);
            findAllocationsByEmployeePeriodStmt.setString(2, period);
            findAllocationsByEmployeePeriodStmt.setInt(3, year);
            result = findAllocationsByEmployeePeriodStmt.executeQuery();
            while (result.next()) {
                allocations.add(createAllocationFromResultSet(result));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return allocations;
    }

    /**
     * Reads all allocations for a course instance.
     *
     * @param instanceId The course instance ID.
     * @return List of allocations.
     * @throws TeachingDBException If failed to read.
     */
    public List<TeachingAllocation> findAllocationsByInstance(String instanceId) throws TeachingDBException {
        String failureMsg = "Could not read allocations for instance.";
        List<TeachingAllocation> allocations = new ArrayList<>();
        ResultSet result = null;
        try {
            findAllocationsByInstanceStmt.setString(1, instanceId);
            result = findAllocationsByInstanceStmt.executeQuery();
            while (result.next()) {
                allocations.add(createAllocationFromResultSet(result));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return allocations;
    }

    /**
     * Creates a new allocation.
     *
     * @param allocation The allocation to create.
     * @throws TeachingDBException If failed to create.
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
     * Deletes an allocation.
     *
     * @param employeeId The employee ID.
     * @param instanceId The course instance ID.
     * @param activityId The activity ID.
     * @throws TeachingDBException If failed to delete.
     */
    public void deleteAllocation(int employeeId, String instanceId, int activityId)
            throws TeachingDBException {
        String failureMsg = "Could not delete allocation.";
        try {
            deleteAllocationStmt.setInt(1, employeeId);
            deleteAllocationStmt.setString(2, instanceId);
            deleteAllocationStmt.setInt(3, activityId);
            int updatedRows = deleteAllocationStmt.executeUpdate();
            if (updatedRows != 1) {
                handleException(failureMsg, null);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Retrieves all teaching activities.
     *
     * @return List of all teaching activities.
     * @throws TeachingDBException If failed to read.
     */
    public List<TeachingActivity> findAllTeachingActivities() throws TeachingDBException {
        String failureMsg = "Could not list teaching activities.";
        List<TeachingActivity> activities = new ArrayList<>();
        try (ResultSet result = findAllTeachingActivitiesStmt.executeQuery()) {
            while (result.next()) {
                activities.add(new TeachingActivity(
                        result.getInt("activity_id"),
                        result.getString("activity_name"),
                        result.getBigDecimal("factor"),
                        result.getBoolean("is_derived")));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
        return activities;
    }

    /**
     * Creates a new teaching activity.
     *
     * @param activity The activity to create.
     * @throws TeachingDBException If failed to create.
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
     *
     * @param instanceId   The course instance ID.
     * @param activityId   The activity ID.
     * @param plannedHours The planned hours.
     * @throws TeachingDBException If failed to create.
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

    /**
     * Finds the latest salary version ID for an employee.
     *
     * @param employeeId The employee ID.
     * @return The salary version ID, or 0 if not found.
     * @throws TeachingDBException If failed to read.
     */
    public int findLatestSalaryVersionByEmployee(int employeeId) throws TeachingDBException {
        String failureMsg = "Could not find salary version.";
        ResultSet result = null;
        try {
            findLatestSalaryVersionStmt.setInt(1, employeeId);
            result = findLatestSalaryVersionStmt.executeQuery();
            if (result.next()) {
                int versionId = result.getInt("salary_version_id");
                connection.commit();
                return versionId;
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return 0;
    }

    /**
     * Retrieves course instances for a specific study year.
     *
     * @param year The study year.
     * @return A list of course instances for that year.
     * @throws TeachingDBException If failed to search.
     */
    public List<CourseInstance> findCourseInstancesByYear(int year) throws TeachingDBException {
        String failureMsg = "Could not list course instances for year " + year;
        List<CourseInstance> instances = new ArrayList<>();
        ResultSet result = null;
        try {
            findCourseInstancesByYearStmt.setInt(1, year);
            result = findCourseInstancesByYearStmt.executeQuery();
            while (result.next()) {
                instances.add(createCourseInstanceFromResultSet(result));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return instances;
    }

    /**
     * Retrieves allocations for a specific activity by name (for Task A req 4 -
     * Exercise query).
     * Returns detailed info including course name and teacher name.
     *
     * @param activityName The activity name (e.g., "Exercise").
     * @return A list of allocations for that activity with extended info.
     * @throws TeachingDBException If failed to search.
     */
    public List<String[]> findAllocationsByActivityName(String activityName) throws TeachingDBException {
        String failureMsg = "Could not find allocations for activity: " + activityName;
        List<String[]> results = new ArrayList<>();
        ResultSet result = null;
        try {
            findAllocationsByActivityNameStmt.setString(1, activityName);
            result = findAllocationsByActivityNameStmt.executeQuery();
            while (result.next()) {
                String[] row = new String[8];
                row[0] = String.valueOf(result.getInt("employee_id"));
                row[1] = result.getString("first_name") + " " + result.getString("last_name");
                row[2] = result.getString("course_instance_id");
                row[3] = result.getString("course_name");
                row[4] = result.getString("activity_name");
                row[5] = result.getBigDecimal("allocated_hours").toString();
                row[6] = String.valueOf(result.getInt("study_year"));
                row[7] = result.getString("study_period");
                results.add(row);
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return results;
    }

    /**
     * Finds a teaching activity by name.
     *
     * @param activityName The activity name.
     * @return The activity, or null if not found.
     * @throws TeachingDBException If failed to search.
     */
    public TeachingActivity findActivityByName(String activityName) throws TeachingDBException {
        String failureMsg = "Could not find activity: " + activityName;
        ResultSet result = null;
        try {
            findActivityByNameStmt.setString(1, activityName);
            result = findActivityByNameStmt.executeQuery();
            if (result.next()) {
                TeachingActivity activity = new TeachingActivity(
                        result.getInt("activity_id"),
                        result.getString("activity_name"),
                        result.getBigDecimal("factor"),
                        result.getBoolean("is_derived"));
                connection.commit();
                return activity;
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        } finally {
            closeResultSet(failureMsg, result);
        }
        return null;
    }

    /**
     * Commits the current transaction.
     * 
     * @throws TeachingDBException If unable to commit.
     */
    public void commit() throws TeachingDBException {
        try {
            connection.commit();
        } catch (SQLException e) {
            handleException("Failed to commit", e);
        }
    }

    private void connectToTeachingDB() throws ClassNotFoundException, SQLException {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/dsp_university",
                "postgres", "postgres");
        // Autocommit MUST be off for proper ACID transaction handling
        connection.setAutoCommit(false);
    }

    private void prepareStatements() throws SQLException {
        // Find all course instances with header info
        findAllCourseInstancesStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "ORDER BY ci.study_year DESC, ci.study_period, ci.course_code");

        // Find course instance by ID
        findCourseInstanceByIdStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.instance_id = ?");

        // Find course instance by ID with lock
        findCourseInstanceByIdLockingStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.instance_id = ? FOR NO KEY UPDATE OF ci");

        // Update student count
        updateCourseInstanceStudentsStmt = connection.prepareStatement(
                "UPDATE dsp.course_instance SET num_students = ? WHERE instance_id = ?");

        // Find planned cost (sum of effective hours from view)
        findPlannedCostByInstanceStmt = connection.prepareStatement(
                "SELECT SUM(effective_hours) AS total_hours " +
                        "FROM dsp.v_activity_hours WHERE course_instance_id = ?");

        // Find actual cost from allocations
        findActualCostByInstanceStmt = connection.prepareStatement(
                "SELECT SUM(cost) AS total_cost FROM dsp.v_allocation_cost " +
                        "WHERE course_instance_id = ?");

        // Count distinct instances for employee in period
        findCourseInstanceCountByEmployeePeriodStmt = connection.prepareStatement(
                "SELECT COUNT(DISTINCT a.course_instance_id) AS instance_count " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ?");

        // Find allocations by employee and period
        findAllocationsByEmployeePeriodStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, ta.activity_name " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ?");

        // Find allocations by employee and period with lock
        findAllocationsByEmployeePeriodLockingStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, ta.activity_name " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "WHERE a.employee_id = ? AND ci.study_period = ? AND ci.study_year = ? " +
                        "FOR NO KEY UPDATE OF a");

        // Find allocations by instance
        findAllocationsByInstanceStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, ta.activity_name " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "WHERE a.course_instance_id = ?");

        // Create allocation
        createAllocationStmt = connection.prepareStatement(
                "INSERT INTO dsp.allocation (employee_id, course_instance_id, activity_id, " +
                        "salary_version_id, allocated_hours) VALUES (?, ?, ?, ?, ?)");

        // Delete allocation
        deleteAllocationStmt = connection.prepareStatement(
                "DELETE FROM dsp.allocation WHERE employee_id = ? AND course_instance_id = ? " +
                        "AND activity_id = ?");

        // Find all teaching activities
        findAllTeachingActivitiesStmt = connection.prepareStatement(
                "SELECT activity_id, activity_name, factor, is_derived " +
                        "FROM dsp.teaching_activity ORDER BY activity_id");

        // Create teaching activity
        createTeachingActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.teaching_activity (activity_name, factor, is_derived) " +
                        "VALUES (?, ?, ?)");

        // Create planned activity
        createPlannedActivityStmt = connection.prepareStatement(
                "INSERT INTO dsp.planned_activity (course_instance_id, activity_id, planned_hours) " +
                        "VALUES (?, ?, ?)");

        // Find latest salary version for employee
        findLatestSalaryVersionStmt = connection.prepareStatement(
                "SELECT salary_version_id FROM dsp.employee_salary_history " +
                        "WHERE employee_id = ? ORDER BY version_no DESC LIMIT 1");

        // Find course instances by year
        findCourseInstancesByYearStmt = connection.prepareStatement(
                "SELECT ci.instance_id, ci.course_code, cl.course_name, ci.study_year, " +
                        "ci.study_period, ci.num_students, ci.layout_version_no " +
                        "FROM dsp.course_instance ci " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "WHERE ci.study_year = ? " +
                        "ORDER BY ci.study_period, ci.course_code");

        // Find allocations by activity name (for Exercise query)
        findAllocationsByActivityNameStmt = connection.prepareStatement(
                "SELECT a.employee_id, a.course_instance_id, a.activity_id, " +
                        "a.salary_version_id, a.allocated_hours, ta.activity_name, " +
                        "cl.course_name, p.first_name, p.last_name, ci.study_year, ci.study_period " +
                        "FROM dsp.allocation a " +
                        "JOIN dsp.teaching_activity ta ON ta.activity_id = a.activity_id " +
                        "JOIN dsp.course_instance ci ON ci.instance_id = a.course_instance_id " +
                        "JOIN dsp.course_layout cl ON cl.course_code = ci.course_code " +
                        "JOIN dsp.employee e ON e.employee_id = a.employee_id " +
                        "JOIN dsp.person p ON p.personal_number = e.personal_number " +
                        "WHERE LOWER(ta.activity_name) = LOWER(?)");

        // Find activity by name
        findActivityByNameStmt = connection.prepareStatement(
                "SELECT activity_id, activity_name, factor, is_derived " +
                        "FROM dsp.teaching_activity WHERE LOWER(activity_name) = LOWER(?)");
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
                result.getBigDecimal("allocated_hours"));
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
