/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * A teaching allocation in the system.
 */
public class TeachingAllocation implements TeachingAllocationDTO {
    private int employeeId;
    private String courseInstanceId;
    private int activityId;
    private String activityName;
    private int salaryVersionId;
    private BigDecimal allocatedHours;

    /**
     * Creates a new teaching allocation.
     */
    public TeachingAllocation(int employeeId, String courseInstanceId, int activityId,
            String activityName, int salaryVersionId, BigDecimal allocatedHours) {
        this.employeeId = employeeId;
        this.courseInstanceId = courseInstanceId;
        this.activityId = activityId;
        this.activityName = activityName;
        this.salaryVersionId = salaryVersionId;
        this.allocatedHours = allocatedHours;
    }

    /**
     * Creates a new teaching allocation without activity name (for creation).
     */
    public TeachingAllocation(int employeeId, String courseInstanceId, int activityId,
            int salaryVersionId, BigDecimal allocatedHours) {
        this(employeeId, courseInstanceId, activityId, null, salaryVersionId, allocatedHours);
    }

    @Override
    public int getEmployeeId() {
        return employeeId;
    }

    @Override
    public String getCourseInstanceId() {
        return courseInstanceId;
    }

    @Override
    public int getActivityId() {
        return activityId;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    @Override
    public int getSalaryVersionId() {
        return salaryVersionId;
    }

    @Override
    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    @Override
    public String toString() {
        return "Allocation[emp=" + employeeId + ", instance=" + courseInstanceId +
                ", activity=" + activityName + "(" + activityId + "), hours=" + allocatedHours + "]";
    }
}
