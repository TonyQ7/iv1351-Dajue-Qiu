/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

/**
 * A course instance in the teaching allocation system.
 */
public class CourseInstance implements CourseInstanceDTO {
    private String instanceId;
    private String courseCode;
    private String courseName;
    private int studyYear;
    private String studyPeriod;
    private int numStudents;
    private int layoutVersionNo;

    /**
     * Creates a course instance with all fields.
     */
    public CourseInstance(String instanceId, String courseCode, String courseName,
            int studyYear, String studyPeriod, int numStudents, int layoutVersionNo) {
        this.instanceId = instanceId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.studyYear = studyYear;
        this.studyPeriod = studyPeriod;
        this.numStudents = numStudents;
        this.layoutVersionNo = layoutVersionNo;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getCourseCode() {
        return courseCode;
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
    public int getNumStudents() {
        return numStudents;
    }

    @Override
    public int getLayoutVersionNo() {
        return layoutVersionNo;
    }

    /**
     * Increases the number of students by the specified count.
     *
     * @param count The number to add.
     * @throws AllocationRejectedException If count is negative.
     */
    public void increaseStudents(int count) throws AllocationRejectedException {
        if (count < 0) {
            throw new AllocationRejectedException(
                    "Cannot increase students by negative amount: " + count);
        }
        this.numStudents += count;
    }

    @Override
    public String toString() {
        return "CourseInstance[id=" + instanceId + ", code=" + courseCode +
                ", name=" + courseName + ", year=" + studyYear +
                ", period=" + studyPeriod + ", students=" + numStudents + "]";
    }
}
