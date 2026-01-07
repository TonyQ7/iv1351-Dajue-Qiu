package se.kth.iv1351.teachingalloc.controller;

import java.math.BigDecimal;
import java.util.List;

import se.kth.iv1351.teachingalloc.model.AllocationRejectedException;
import se.kth.iv1351.teachingalloc.model.CostSummaryDTO;
import se.kth.iv1351.teachingalloc.model.CourseInstanceDTO;
import se.kth.iv1351.teachingalloc.model.TeachingActivityDTO;
import se.kth.iv1351.teachingalloc.model.TeachingAllocationDTO;
import se.kth.iv1351.teachingalloc.model.TeachingException;
import se.kth.iv1351.teachingalloc.model.TeachingService;
import se.kth.iv1351.teachingalloc.model.ActivityAllocationDTO;

/**
 * This is the application's only controller, all calls to the model pass here.
 * The controller is a thin pass-through layer that coordinates flow between
 * View and Service. It does NOT contain business logic or transaction
 * management.
 */
public class Controller {
    private final TeachingService teachingService;

    /**
     * Creates a new Controller with TeachingService dependency.
     */
    public Controller() {
        this.teachingService = new TeachingService();
    }

    /**
     * Lists all course instances.
     */
    public List<? extends CourseInstanceDTO> getAllCourseInstances() throws TeachingException {
        return teachingService.GetAllCourseInstances();
    }

    /**
     * Lists course instances for a specific year.
     */
    public List<? extends CourseInstanceDTO> getCourseInstancesByYear(int year) throws TeachingException {
        return teachingService.GetCourseInstancesByYear(year);
    }

    /**
     * Gets allocations for a specific activity by name.
     */
    public List<? extends ActivityAllocationDTO> getAllocationsByActivityName(String activityName)
            throws TeachingException {
        return teachingService.GetAllocationsByActivityName(activityName);
    }

    /**
     * Lists all teaching activities.
     */
    public List<? extends TeachingActivityDTO> getAllTeachingActivities() throws TeachingException {
        return teachingService.GetAllTeachingActivities();
    }

    /**
     * Computes the teaching cost for a course instance.
     */
    public CostSummaryDTO computeTeachingCost(String instanceId) throws TeachingException {
        return teachingService.ComputeTeachingCost(instanceId);
    }

    /**
     * Increases the number of students for a course instance.
     */
    public void increaseStudentCount(String instanceId, int count) throws TeachingException {
        teachingService.IncreaseStudentCount(instanceId, count);
    }

    /**
     * Allocates teaching to an employee for a course instance activity.
     */
    public void allocateTeaching(int employeeId, String instanceId, int activityId, BigDecimal hours)
            throws AllocationRejectedException, TeachingException {
        teachingService.AllocateTeaching(employeeId, instanceId, activityId, hours);
    }

    /**
     * Deallocates teaching from an employee.
     */
    public void deallocateTeaching(int employeeId, String instanceId, int activityId)
            throws TeachingException {
        teachingService.DeallocateTeaching(employeeId, instanceId, activityId);
    }

    /**
     * Lists allocations for a specific employee in a period.
     */
    public List<? extends TeachingAllocationDTO> getTeacherAllocations(int employeeId, String period, int year)
            throws TeachingException {
        return teachingService.GetTeacherAllocations(employeeId, period, year);
    }

    /**
     * Lists all allocations for a course instance.
     */
    public List<? extends TeachingAllocationDTO> getInstanceAllocations(String instanceId)
            throws TeachingException {
        return teachingService.GetInstanceAllocations(instanceId);
    }

    /**
     * Creates a new teaching activity.
     */
    public void createTeachingActivity(String name, BigDecimal factor) throws TeachingException {
        teachingService.CreateTeachingActivity(name, factor);
    }

    /**
     * Associates a teaching activity with a course instance.
     */
    public void associateActivityWithInstance(String instanceId, int activityId, BigDecimal plannedHours)
            throws TeachingException {
        teachingService.AssociateActivityWithInstance(instanceId, activityId, plannedHours);
    }
}
