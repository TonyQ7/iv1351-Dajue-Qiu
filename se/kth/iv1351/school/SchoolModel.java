package se.kth.iv1351.school;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated Model definitions.
 */
public class SchoolModel {
    // Exception
    public static class SchoolDBException extends Exception {
        public SchoolDBException(String message) {
            super(message);
        }

        public SchoolDBException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // DTOs
    public static class ActivityDTO {
        public final int id;
        public final String name;
        public final double rawPlannedHours;
        public final double factor;
        public final boolean isDerived;
        public final double constCoeff;
        public final double hpCoeff;
        public final double studentsCoeff;

        public ActivityDTO(int id, String name, double rawPlannedHours, double factor, boolean isDerived,
                double constCoeff, double hpCoeff, double studentsCoeff) {
            this.id = id;
            this.name = name;
            this.rawPlannedHours = rawPlannedHours;
            this.factor = factor;
            this.isDerived = isDerived;
            this.constCoeff = constCoeff;
            this.hpCoeff = hpCoeff;
            this.studentsCoeff = studentsCoeff;
        }

        public double calculateEffectiveHours(int numStudents, double hp) {
            double base = isDerived ? (constCoeff + hpCoeff * hp + studentsCoeff * numStudents) : rawPlannedHours;
            return base * factor;
        }
    }

    public static class AllocationDTO {
        public final int id;
        public final String teacherName;
        public final double hourlySalary;
        public final double allocatedHours;

        public AllocationDTO(int id, String teacherName, double hourlySalary, double allocatedHours) {
            this.id = id;
            this.teacherName = teacherName;
            this.hourlySalary = hourlySalary;
            this.allocatedHours = allocatedHours;
        }
    }

    public static class CourseInstanceDTO {
        public final String id;
        public final String courseCode;
        public final int year;
        public final String period;
        public final int numStudents;
        public final double hp;

        public CourseInstanceDTO(String id, String courseCode, int year, String period, int numStudents, double hp) {
            this.id = id;
            this.courseCode = courseCode;
            this.year = year;
            this.period = period;
            this.numStudents = numStudents;
            this.hp = hp;
        }
    }

    public static class TeacherDTO {
        public final int id;
        public final String name;

        public TeacherDTO(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class CostReportDTO {
        public final CourseInstanceDTO instance;
        public final List<ActivityCost> activities = new ArrayList<>();
        public double totalPlannedCost;
        public double totalActualCost;
        public double avgSalary;

        public CostReportDTO(CourseInstanceDTO instance, double avgSalary) {
            this.instance = instance;
            this.avgSalary = avgSalary;
        }

        public static class ActivityCost {
            public final ActivityDTO activity;
            public final double plannedHours;
            public final double plannedCost;
            public final List<AllocationDTO> allocations;
            public double actualCost;

            public ActivityCost(ActivityDTO activity, double plannedHours, double plannedCost,
                    List<AllocationDTO> allocations) {
                this.activity = activity;
                this.plannedHours = plannedHours;
                this.plannedCost = plannedCost;
                this.allocations = allocations;
                this.actualCost = allocations.stream().mapToDouble(a -> a.allocatedHours * a.hourlySalary).sum();
            }
        }
    }
}
