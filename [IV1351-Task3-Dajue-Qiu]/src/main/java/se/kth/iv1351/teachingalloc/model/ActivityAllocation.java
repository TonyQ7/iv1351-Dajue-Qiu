package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * An implementation of {@link ActivityAllocationDTO}.
 * Represents a detailed view of an allocation for a specific activity,
 * joining data from employee, course, and allocation tables.
 */
public class ActivityAllocation implements ActivityAllocationDTO {
    private final int employeeId;
    private final String employeeName;
    private final String courseInstanceId;
    private final String courseName;
    private final int studyYear;
    private final String studyPeriod;
    private final String activityName;
    private final BigDecimal allocatedHours;

    /**
     * Creates a new instance.
     */
    public ActivityAllocation(int employeeId, String employeeName, String courseInstanceId,
            String courseName, int studyYear, String studyPeriod, String activityName,
            BigDecimal allocatedHours) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.courseInstanceId = courseInstanceId;
        this.courseName = courseName;
        this.studyYear = studyYear;
        this.studyPeriod = studyPeriod;
        this.activityName = activityName;
        this.allocatedHours = allocatedHours;
    }

    @Override
    public int getEmployeeId() {
        return employeeId;
    }

    @Override
    public String getEmployeeName() {
        return employeeName;
    }

    @Override
    public String getCourseInstanceId() {
        return courseInstanceId;
    }

    @Override
    public String getCourseName() {
        return courseName;
    }

    @Override
    public int getStudyYear() {
        return studyYear;
    }

    @Override
    public String getStudyPeriod() {
        return studyPeriod;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    @Override
    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }
}

