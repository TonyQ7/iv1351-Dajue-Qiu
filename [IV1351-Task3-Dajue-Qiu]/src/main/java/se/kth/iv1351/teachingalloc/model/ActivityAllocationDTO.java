package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * Specifies the read-only view of a detailed activity allocation.
 * Used for reporting allocations by activity type (Task A req 4).
 */
public interface ActivityAllocationDTO {
    /**
     * @return The employee ID.
     */
    int getEmployeeId();

    /**
     * @return The employee's full name.
     */
    String getEmployeeName();

    /**
     * @return The course instance ID.
     */
    String getCourseInstanceId();

    /**
     * @return The course name.
     */
    String getCourseName();

    /**
     * @return The study year.
     */
    int getStudyYear();

    /**
     * @return The study period.
     */
    String getStudyPeriod();

    /**
     * @return The activity name.
     */
    String getActivityName();

    /**
     * @return The allocated hours.
     */
    BigDecimal getAllocatedHours();
}

