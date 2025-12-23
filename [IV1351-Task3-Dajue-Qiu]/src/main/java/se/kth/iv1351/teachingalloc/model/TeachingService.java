package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import se.kth.iv1351.teachingalloc.integration.TransactionManager;

/**
 * Service layer containing all business logic for teaching allocation.
 * Uses TransactionManager for transaction handling.
 * Does NOT contain commit/rollback calls - that is the TransactionManager's
 * responsibility.
 */
public class TeachingService {
    // Default average salary for planned cost estimation (SEK/hour)
    private static final BigDecimal DEFAULT_AVG_SALARY_PER_HOUR = new BigDecimal("600.00");
    // Divisor for converting SEK to KSEK
    private static final BigDecimal KSEK_DIVISOR = new BigDecimal("1000");

    private final TransactionManager transactionManager;

    /**
     * Creates a new teaching service.
     */
    public TeachingService() {
        this.transactionManager = new TransactionManager();
    }

    /**
     * Lists all course instances.
     */
    public List<? extends CourseInstanceDTO> GetAllCourseInstances() throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readAllCourseInstances());
        } catch (Exception e) {
            throw new TeachingException("Unable to list course instances.", e);
        }
    }

    /**
     * Lists course instances for a specific year.
     */
    public List<? extends CourseInstanceDTO> GetCourseInstancesByYear(int year) throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readCourseInstancesByYear(year));
        } catch (Exception e) {
            throw new TeachingException("Unable to list course instances for year " + year, e);
        }
    }

    /**
     * Lists all teaching activities.
     */
    public List<? extends TeachingActivityDTO> GetAllTeachingActivities() throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readAllTeachingActivities());
        } catch (Exception e) {
            throw new TeachingException("Unable to list teaching activities.", e);
        }
    }

    /**
     * Gets allocations for a specific activity by name.
     */
    public List<? extends ActivityAllocationDTO> GetAllocationsByActivityName(String activityName)
            throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readAllocationsByActivityName(activityName));
        } catch (Exception e) {
            throw new TeachingException("Unable to find allocations for activity: " + activityName, e);
        }
    }

    /**
     * Lists allocations for a specific teacher in a period.
     */
    public List<? extends TeachingAllocationDTO> GetTeacherAllocations(int employeeId, String period, int year)
            throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readAllocationsByEmployeePeriod(employeeId, period, year));
        } catch (Exception e) {
            throw new TeachingException("Could not retrieve allocations for employee " + employeeId, e);
        }
    }

    /**
     * Lists all allocations for a course instance.
     */
    public List<? extends TeachingAllocationDTO> GetInstanceAllocations(String instanceId)
            throws TeachingException {
        try {
            return transactionManager.Execute(dao -> dao.readAllocationsByInstance(instanceId));
        } catch (Exception e) {
            throw new TeachingException("Could not retrieve allocations for instance " + instanceId, e);
        }
    }

    /**
     * Computes the teaching cost (planned and actual) for a course instance.
     * BUSINESS LOGIC: Cost calculation is performed here, not in DAO.
     */
    public CostSummaryDTO ComputeTeachingCost(String instanceId) throws TeachingException {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new TeachingException("Invalid instance ID");
        }

        try {
            return transactionManager.Execute(dao -> {
                CourseInstance instance = dao.readCourseInstanceById(instanceId, false);
                if (instance == null) {
                    throw new TeachingException("Course instance not found: " + instanceId);
                }

                // Get raw data from DAO
                BigDecimal totalPlannedHours = dao.readPlannedHoursByInstance(instanceId);
                BigDecimal totalActualCost = dao.readActualCostByInstance(instanceId);

                // BUSINESS LOGIC: Calculate planned cost
                BigDecimal plannedCost = totalPlannedHours
                        .multiply(DEFAULT_AVG_SALARY_PER_HOUR)
                        .divide(KSEK_DIVISOR, 2, RoundingMode.HALF_UP);

                // BUSINESS LOGIC: Convert actual cost to KSEK
                BigDecimal actualCostKSEK = totalActualCost
                        .divide(KSEK_DIVISOR, 2, RoundingMode.HALF_UP);

                return new CostSummary(
                        instance.getCourseCode(),
                        instance.getInstanceId(),
                        instance.getStudyPeriod(),
                        plannedCost,
                        actualCostKSEK);
            });
        } catch (TeachingException e) {
            throw e;
        } catch (Exception e) {
            throw new TeachingException("Could not compute teaching cost for: " + instanceId, e);
        }
    }

    /**
     * Increases the number of students for a course instance.
     * Uses locking read to prevent lost updates.
     */
    public void IncreaseStudentCount(String instanceId, int count) throws TeachingException {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new TeachingException("Invalid instance ID");
        }

        try {
            transactionManager.ExecuteVoid(dao -> {
                // 1. Read with lock (FOR NO KEY UPDATE) to prevent lost updates
                CourseInstance instance = dao.readCourseInstanceById(instanceId, true);
                if (instance == null) {
                    throw new TeachingException("Course instance not found: " + instanceId);
                }

                // 2. BUSINESS LOGIC: Increase students
                instance.increaseStudents(count);

                // 3. Persist the change
                dao.updateCourseInstanceStudents(instance);
            });
        } catch (TeachingException e) {
            throw e;
        } catch (Exception e) {
            throw new TeachingException("Could not increase student count for: " + instanceId, e);
        }
    }

    /**
     * Allocates teaching to an employee for a course instance activity.
     * BUSINESS LOGIC: Enforces 4-course limit per period.
     * Uses locking to prevent race conditions.
     */
    public void AllocateTeaching(int employeeId, String instanceId, int activityId, BigDecimal hours)
            throws AllocationRejectedException, TeachingException {
        try {
            transactionManager.ExecuteVoid(dao -> {
                // 1. LOCK THE EMPLOYEE (anchor row for serialization)
                dao.ReadEmployeeForUpdate(employeeId);

                // 2. Read course instance with lock
                CourseInstance instance = dao.readCourseInstanceById(instanceId, true);
                if (instance == null) {
                    throw new TeachingException("Course instance not found: " + instanceId);
                }

                String period = instance.getStudyPeriod();
                int year = instance.getStudyYear();

                // 3. Read allocation limit from database
                int maxCoursesPerPeriod = dao.readAllocationLimit();

                // 4. Check if allocation already exists
                TeachingAllocation existing = dao.readExistingAllocation(employeeId, instanceId, activityId);
                if (existing != null) {
                    if (existing.isTerminated()) {
                        // Reactivate terminated allocation
                        int salaryVersionId = dao.readLatestSalaryVersionByEmployee(employeeId);
                        if (salaryVersionId == 0) {
                            throw new TeachingException("No salary version found for employee: " + employeeId);
                        }
                        dao.reactivateAllocation(employeeId, instanceId, activityId, salaryVersionId, hours);
                        return;
                    } else {
                        throw new AllocationRejectedException(
                                "Allocation already exists for employee " + employeeId +
                                        " on instance " + instanceId + " activity " + activityId);
                    }
                }

                // 5. Read current allocation count
                int currentCount = dao.readAllocationCountByEmployeePeriod(employeeId, period, year);

                // 6. Check if this instance is already allocated to this employee
                List<TeachingAllocation> existingAllocations = dao.readAllocationsByEmployeePeriod(
                        employeeId, period, year);
                boolean alreadyAllocatedToInstance = existingAllocations.stream()
                        .anyMatch(a -> a.getCourseInstanceId().equals(instanceId));

                // 7. BUSINESS LOGIC: Enforce limit
                if (!alreadyAllocatedToInstance && currentCount >= maxCoursesPerPeriod) {
                    throw new AllocationRejectedException(
                            "Employee " + employeeId + " would exceed " + maxCoursesPerPeriod +
                                    " course instances in " + period + " " + year);
                }

                // 8. Get salary version
                int salaryVersionId = dao.readLatestSalaryVersionByEmployee(employeeId);
                if (salaryVersionId == 0) {
                    throw new TeachingException("No salary version found for employee: " + employeeId);
                }

                // 9. Create allocation
                TeachingAllocation allocation = new TeachingAllocation(
                        employeeId, instanceId, activityId, salaryVersionId, hours);
                dao.createAllocation(allocation);
            });
        } catch (AllocationRejectedException e) {
            throw e;
        } catch (TeachingException e) {
            throw e;
        } catch (Exception e) {
            throw new TeachingException("Could not allocate teaching for employee " + employeeId, e);
        }
    }

    /**
     * Deallocates (terminates) teaching for an employee.
     */
    public void DeallocateTeaching(int employeeId, String instanceId, int activityId)
            throws TeachingException {
        try {
            transactionManager.ExecuteVoid(dao -> dao.terminateAllocation(employeeId, instanceId, activityId));
        } catch (Exception e) {
            throw new TeachingException("Could not deallocate teaching for employee " + employeeId, e);
        }
    }

    /**
     * Creates a new teaching activity.
     */
    public void CreateTeachingActivity(String name, BigDecimal factor) throws TeachingException {
        if (name == null || name.isEmpty()) {
            throw new TeachingException("Invalid activity name");
        }

        try {
            transactionManager.ExecuteVoid(dao -> {
                TeachingActivity activity = new TeachingActivity(name, factor, false);
                dao.createTeachingActivity(activity);
            });
        } catch (Exception e) {
            throw new TeachingException("Could not create teaching activity: " + name, e);
        }
    }

    /**
     * Associates a teaching activity with a course instance.
     * BUSINESS LOGIC: Derived activities cannot have planned hours.
     */
    public void AssociateActivityWithInstance(String instanceId, int activityId, BigDecimal plannedHours)
            throws TeachingException {
        try {
            transactionManager.ExecuteVoid(dao -> {
                // BUSINESS LOGIC: Check if activity is derived
                TeachingActivity activity = dao.readActivityById(activityId);
                if (activity == null) {
                    throw new TeachingException("Activity not found: " + activityId);
                }
                if (activity.isDerived()) {
                    throw new TeachingException(
                            "Cannot create planned hours for derived activity: " + activity.getActivityName() +
                                    ". Derived activities are computed automatically.");
                }

                dao.createPlannedActivity(instanceId, activityId, plannedHours);
            });
        } catch (TeachingException e) {
            throw e;
        } catch (Exception e) {
            throw new TeachingException("Could not associate activity with instance: " + instanceId, e);
        }
    }
}
