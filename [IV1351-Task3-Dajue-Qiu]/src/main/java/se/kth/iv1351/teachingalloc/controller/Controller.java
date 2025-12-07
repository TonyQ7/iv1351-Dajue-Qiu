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

package se.kth.iv1351.teachingalloc.controller;

import java.math.BigDecimal;
import java.util.List;

import se.kth.iv1351.teachingalloc.integration.TeachingDAO;
import se.kth.iv1351.teachingalloc.integration.TeachingDBException;
import se.kth.iv1351.teachingalloc.model.AllocationRejectedException;
import se.kth.iv1351.teachingalloc.model.CostSummary;
import se.kth.iv1351.teachingalloc.model.CostSummaryDTO;
import se.kth.iv1351.teachingalloc.model.CourseInstance;
import se.kth.iv1351.teachingalloc.model.CourseInstanceDTO;
import se.kth.iv1351.teachingalloc.model.TeachingActivity;
import se.kth.iv1351.teachingalloc.model.TeachingActivityDTO;
import se.kth.iv1351.teachingalloc.model.TeachingAllocation;
import se.kth.iv1351.teachingalloc.model.TeachingAllocationDTO;
import se.kth.iv1351.teachingalloc.model.TeachingException;

/**
 * This is the application's only controller, all calls to the model pass here.
 * The controller is also responsible for calling the DAO. Typically, the
 * controller first calls the DAO to retrieve data (if needed), then operates on
 * the data, and finally tells the DAO to store the updated data (if any).
 * 
 * Business logic is handled here, NOT in the DAO.
 */
public class Controller {
    private static final int MAX_COURSES_PER_PERIOD = 4;
    private static final BigDecimal DEFAULT_AVG_SALARY_PER_HOUR = new BigDecimal("600.00");

    private final TeachingDAO teachingDb;

    /**
     * Creates a new instance, and retrieves a connection to the database.
     * 
     * @throws TeachingDBException If unable to connect to the database.
     */
    public Controller() throws TeachingDBException {
        teachingDb = new TeachingDAO();
    }

    /**
     * Lists all course instances.
     * 
     * @return A list containing all course instances.
     * @throws TeachingException If unable to retrieve course instances.
     */
    public List<? extends CourseInstanceDTO> getAllCourseInstances() throws TeachingException {
        try {
            return teachingDb.findAllCourseInstances();
        } catch (Exception e) {
            throw new TeachingException("Unable to list course instances.", e);
        }
    }

    /**
     * Lists course instances for a specific year (for Task A - current year
     * filter).
     * 
     * @param year The study year.
     * @return A list containing course instances for that year.
     * @throws TeachingException If unable to retrieve course instances.
     */
    public List<? extends CourseInstanceDTO> getCourseInstancesByYear(int year) throws TeachingException {
        try {
            return teachingDb.findCourseInstancesByYear(year);
        } catch (Exception e) {
            throw new TeachingException("Unable to list course instances for year " + year, e);
        }
    }

    /**
     * Gets allocations for a specific activity by name (for Task A req 4 - Exercise
     * query).
     * Returns extended info including teacher name and course name.
     * 
     * @param activityName The activity name (e.g., "Exercise").
     * @return A list of String arrays with allocation details.
     * @throws TeachingException If unable to retrieve allocations.
     */
    public List<String[]> getAllocationsByActivityName(String activityName) throws TeachingException {
        try {
            return teachingDb.findAllocationsByActivityName(activityName);
        } catch (Exception e) {
            throw new TeachingException("Unable to find allocations for activity: " + activityName, e);
        }
    }

    /**
     * Lists all teaching activities.
     * 
     * @return A list containing all teaching activities.
     * @throws TeachingException If unable to retrieve activities.
     */
    public List<? extends TeachingActivityDTO> getAllTeachingActivities() throws TeachingException {
        try {
            return teachingDb.findAllTeachingActivities();
        } catch (Exception e) {
            throw new TeachingException("Unable to list teaching activities.", e);
        }
    }

    /**
     * Computes the teaching cost (planned and actual) for a course instance.
     * 
     * @param instanceId The course instance ID.
     * @return A cost summary with planned and actual costs.
     * @throws TeachingException If unable to compute cost.
     */
    public CostSummaryDTO computeTeachingCost(String instanceId) throws TeachingException {
        String failureMsg = "Could not compute teaching cost for: " + instanceId;

        if (instanceId == null || instanceId.isEmpty()) {
            throw new TeachingException(failureMsg + " - invalid instance ID");
        }

        try {
            CourseInstance instance = teachingDb.findCourseInstanceById(instanceId, false);
            if (instance == null) {
                throw new TeachingException("Course instance not found: " + instanceId);
            }

            BigDecimal plannedCost = teachingDb.findPlannedCostByInstance(instanceId, DEFAULT_AVG_SALARY_PER_HOUR);
            BigDecimal actualCost = teachingDb.findActualCostByInstance(instanceId);

            return new CostSummary(
                    instance.getCourseCode(),
                    instance.getInstanceId(),
                    instance.getStudyPeriod(),
                    plannedCost,
                    actualCost);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        }
    }

    /**
     * Increases the number of students for a course instance.
     * 
     * @param instanceId The course instance ID.
     * @param count      The number of students to add.
     * @throws AllocationRejectedException If the count is invalid.
     * @throws TeachingException           If failed to update.
     */
    public void increaseStudentCount(String instanceId, int count)
            throws AllocationRejectedException, TeachingException {
        String failureMsg = "Could not increase student count for: " + instanceId;

        if (instanceId == null || instanceId.isEmpty()) {
            throw new TeachingException(failureMsg + " - invalid instance ID");
        }

        try {
            // Read with lock to prevent lost updates
            CourseInstance instance = teachingDb.findCourseInstanceById(instanceId, true);
            if (instance == null) {
                throw new TeachingException("Course instance not found: " + instanceId);
            }

            // Business logic: increase students (checks for negative in model)
            instance.increaseStudents(count);

            // Persist the change
            teachingDb.updateCourseInstanceStudents(instance);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        } catch (AllocationRejectedException are) {
            try {
                teachingDb.commit();
            } catch (TeachingDBException tdbe) {
                throw new TeachingException(failureMsg, tdbe);
            }
            throw are;
        }
    }

    /**
     * Allocates teaching to an employee for a course instance activity.
     * Checks the 4-course limit before creating the allocation.
     * 
     * @param employeeId The employee ID.
     * @param instanceId The course instance ID.
     * @param activityId The teaching activity ID.
     * @param hours      The hours to allocate.
     * @throws AllocationRejectedException If the allocation violates business
     *                                     rules.
     * @throws TeachingException           If failed to allocate.
     */
    public void allocateTeaching(int employeeId, String instanceId, int activityId, BigDecimal hours)
            throws AllocationRejectedException, TeachingException {
        String failureMsg = "Could not allocate teaching for employee " + employeeId;

        try {
            // 1. Read the course instance to get period/year (with lock for update)
            CourseInstance instance = teachingDb.findCourseInstanceById(instanceId, true);
            if (instance == null) {
                throw new TeachingException("Course instance not found: " + instanceId);
            }

            String period = instance.getStudyPeriod();
            int year = instance.getStudyYear();

            // 2. Read current allocation count for this employee in this period
            // This is the business logic check - done in controller, not DAO
            int currentCount = teachingDb.findCourseInstanceCountByEmployeePeriod(
                    employeeId, period, year, false);

            // 3. Check if adding this instance would exceed the limit
            // We need to check if this specific instance is already counted
            List<TeachingAllocation> existingAllocations = teachingDb.findAllocationsByEmployeePeriod(employeeId,
                    period, year);

            boolean alreadyAllocatedToInstance = existingAllocations.stream()
                    .anyMatch(a -> a.getCourseInstanceId().equals(instanceId));

            if (!alreadyAllocatedToInstance && currentCount >= MAX_COURSES_PER_PERIOD) {
                teachingDb.commit(); // Release locks
                throw new AllocationRejectedException(
                        "Employee " + employeeId + " would exceed " + MAX_COURSES_PER_PERIOD +
                                " course instances in " + period + " " + year);
            }

            // 4. Get employee's current salary version
            int salaryVersionId = teachingDb.findLatestSalaryVersionByEmployee(employeeId);
            if (salaryVersionId == 0) {
                throw new TeachingException("No salary version found for employee: " + employeeId);
            }

            // 5. Create the allocation
            TeachingAllocation allocation = new TeachingAllocation(
                    employeeId, instanceId, activityId, salaryVersionId, hours);
            teachingDb.createAllocation(allocation);

        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        }
    }

    /**
     * Deallocates teaching from an employee for a course instance activity.
     * 
     * @param employeeId The employee ID.
     * @param instanceId The course instance ID.
     * @param activityId The teaching activity ID.
     * @throws TeachingException If failed to deallocate.
     */
    public void deallocateTeaching(int employeeId, String instanceId, int activityId)
            throws TeachingException {
        String failureMsg = "Could not deallocate teaching for employee " + employeeId;

        try {
            teachingDb.deleteAllocation(employeeId, instanceId, activityId);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        }
    }

    /**
     * Lists allocations for a specific employee in a period.
     * 
     * @param employeeId The employee ID.
     * @param period     The study period.
     * @param year       The study year.
     * @return List of allocations.
     * @throws TeachingException If failed to retrieve allocations.
     */
    public List<? extends TeachingAllocationDTO> getTeacherAllocations(int employeeId, String period, int year)
            throws TeachingException {
        try {
            return teachingDb.findAllocationsByEmployeePeriod(employeeId, period, year);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException("Could not retrieve allocations for employee " + employeeId, tdbe);
        }
    }

    /**
     * Lists all allocations for a course instance.
     * 
     * @param instanceId The course instance ID.
     * @return List of allocations.
     * @throws TeachingException If failed to retrieve allocations.
     */
    public List<? extends TeachingAllocationDTO> getInstanceAllocations(String instanceId)
            throws TeachingException {
        try {
            return teachingDb.findAllocationsByInstance(instanceId);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException("Could not retrieve allocations for instance " + instanceId, tdbe);
        }
    }

    /**
     * Creates a new teaching activity.
     * 
     * @param name   The activity name.
     * @param factor The hour factor.
     * @throws TeachingException If failed to create activity.
     */
    public void createTeachingActivity(String name, BigDecimal factor) throws TeachingException {
        String failureMsg = "Could not create teaching activity: " + name;

        if (name == null || name.isEmpty()) {
            throw new TeachingException(failureMsg + " - invalid name");
        }

        try {
            TeachingActivity activity = new TeachingActivity(name, factor, false);
            teachingDb.createTeachingActivity(activity);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        }
    }

    /**
     * Associates a teaching activity with a course instance (creates planned
     * activity).
     * 
     * @param instanceId   The course instance ID.
     * @param activityId   The activity ID.
     * @param plannedHours The planned hours.
     * @throws TeachingException If failed to create planned activity.
     */
    public void associateActivityWithInstance(String instanceId, int activityId, BigDecimal plannedHours)
            throws TeachingException {
        String failureMsg = "Could not associate activity with instance: " + instanceId;

        try {
            teachingDb.createPlannedActivity(instanceId, activityId, plannedHours);
        } catch (TeachingDBException tdbe) {
            throw new TeachingException(failureMsg, tdbe);
        }
    }
}
