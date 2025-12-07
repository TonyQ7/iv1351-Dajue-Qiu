/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * Specifies a read-only view of a teaching allocation.
 */
public interface TeachingAllocationDTO {
    /**
     * @return The employee ID.
     */
    int getEmployeeId();

    /**
     * @return The course instance ID.
     */
    String getCourseInstanceId();

    /**
     * @return The activity ID.
     */
    int getActivityId();

    /**
     * @return The activity name.
     */
    String getActivityName();

    /**
     * @return The salary version ID.
     */
    int getSalaryVersionId();

    /**
     * @return The allocated hours.
     */
    BigDecimal getAllocatedHours();

    /**
     * @return True if this allocation is terminated (soft deleted).
     */
    boolean isTerminated();
}
