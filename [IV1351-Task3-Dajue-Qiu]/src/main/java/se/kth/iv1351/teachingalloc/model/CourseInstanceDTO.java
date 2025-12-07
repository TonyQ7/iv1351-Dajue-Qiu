/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

/**
 * Specifies a read-only view of a course instance.
 */
public interface CourseInstanceDTO {
    /**
     * @return The course instance ID.
     */
    String getInstanceId();

    /**
     * @return The course code.
     */
    String getCourseCode();

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
     * @return The number of registered students.
     */
    int getNumStudents();

    /**
     * @return The layout version number.
     */
    int getLayoutVersionNo();
}
