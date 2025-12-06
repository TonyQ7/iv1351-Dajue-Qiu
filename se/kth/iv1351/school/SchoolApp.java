package se.kth.iv1351.school;

import java.util.Scanner;
import java.util.List;
import se.kth.iv1351.school.SchoolModel.*;

public class SchoolApp {
    public static void main(String[] args) {
        try {
            Controller ctrl = new Controller();
            SchoolInterpreter interpreter = new SchoolInterpreter(ctrl);
            interpreter.handleCmds();
        } catch (SchoolDBException e) {
            System.out.println("Could not start school client.");
            e.printStackTrace();
        }
    }
}

class SchoolInterpreter {
    private final Controller ctrl;
    private final Scanner scanner = new Scanner(System.in);

    public SchoolInterpreter(Controller ctrl) {
        this.ctrl = ctrl;
    }

    public void handleCmds() {
        System.out.println("School DB Client started. commands: cost, update, allocate, deallocate, exercise, quit");
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                System.out.print("> ");
                String input = scanner.nextLine();
                if (input.trim().isEmpty())
                    continue;

                String[] parts = input.split(" ");
                String cmd = parts[0];

                switch (cmd) {
                    case "cost":
                        if (parts.length != 4) {
                            System.out.println("Usage: cost <courseCode> <year> <period>");
                            break;
                        }
                        try {
                            CostReportDTO report = ctrl.computeCost(parts[1], Integer.parseInt(parts[2]), parts[3]);
                            if (report == null)
                                System.out.println("Course not found.");
                            else
                                printReport(report);
                        } catch (NumberFormatException e) {
                            System.out.println("Year must be a number.");
                        }
                        break;
                    case "update":
                        if (parts.length != 4) {
                            System.out.println("Usage: update <courseCode> <year> <period>");
                            break;
                        }
                        try {
                            CostReportDTO[] reports = ctrl.addStudentsAndRecompute(parts[1], Integer.parseInt(parts[2]),
                                    parts[3]);
                            if (reports == null)
                                System.out.println("Course not found.");
                            else {
                                System.out.println("=== BEFORE UPDATE ===");
                                printReport(reports[0]);
                                System.out.println("\n=== AFTER UPDATE ===");
                                printReport(reports[1]);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Year must be a number.");
                        }
                        break;
                    case "allocate":
                        if (parts.length != 6) {
                            System.out
                                    .println("Usage: allocate <teacherName> <courseCode> <activityId> <period> <year>");
                            break;
                        }
                        try {
                            ctrl.allocateTeacher(parts[1], parts[2], Integer.parseInt(parts[3]), parts[4],
                                    Integer.parseInt(parts[5]));
                            System.out.println("Allocation successful.");
                        } catch (NumberFormatException e) {
                            System.out.println("Activity ID and year must be numbers.");
                        }
                        break;
                    case "deallocate":
                        if (parts.length != 2) {
                            System.out.println("Usage: deallocate <allocationId>");
                            break;
                        }
                        try {
                            ctrl.deallocateTeacher(Integer.parseInt(parts[1]));
                            System.out.println("Deallocation successful.");
                        } catch (NumberFormatException e) {
                            System.out.println("Allocation ID must be a number.");
                        }
                        break;
                    case "exercise":
                        if (parts.length != 5) {
                            System.out.println("Usage: exercise <courseCode> <year> <period> <hours>");
                            break;
                        }
                        try {
                            ctrl.addExerciseActivity(parts[1], Integer.parseInt(parts[2]), parts[3],
                                    Double.parseDouble(parts[4]));
                            System.out.println("Exercise activity added.");
                        } catch (NumberFormatException e) {
                            System.out.println("Year and hours must be numbers.");
                        }
                        break;
                    case "quit":
                        keepRunning = false;
                        break;
                    default:
                        System.out.println("Unknown command");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private void printReport(CostReportDTO report) {
        System.out.printf("Course %s (%s) - %s %d%n", report.instance.courseCode, report.instance.id,
                report.instance.period, report.instance.year);
        System.out.println("Students: " + report.instance.numStudents);
        System.out.println("Activities:");
        for (CostReportDTO.ActivityCost ac : report.activities) {
            System.out.printf("  - %s (ID %d, %.1f h): planned cost %.2f%n", ac.activity.name, ac.activity.id,
                    ac.plannedHours, ac.plannedCost);
            if (ac.allocations.isEmpty())
                System.out.println("    No teacher allocations (actual cost: 0.00)");
            else {
                for (AllocationDTO alloc : ac.allocations) {
                    double cost = alloc.allocatedHours * alloc.hourlySalary;
                    System.out.printf("    * %s at %.2f/h -> %.2f%n", alloc.teacherName, alloc.hourlySalary, cost);
                }
            }
        }
        System.out.printf("Total planned cost (avg salary %.2f): %.2f%n", report.avgSalary, report.totalPlannedCost);
        System.out.printf("Total actual cost: %.2f%n", report.totalActualCost);
    }
}

class Controller {
    private final SchoolDAO db;
    private final TeacherAllocationService allocationService;

    public Controller() throws SchoolDBException {
        this.db = new SchoolDAO();
        this.allocationService = new TeacherAllocationService();
    }

    public CostReportDTO computeCost(String courseCode, int year, String period) throws SchoolDBException {
        try {
            CourseInstanceDTO instance = db.readCourseInstance(courseCode, year, period, false);
            if (instance == null) {
                db.rollback();
                return null;
            }
            CostReportDTO report = generateCostReport(instance);
            db.commit();
            return report;
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    public CostReportDTO[] addStudentsAndRecompute(String courseCode, int year, String period)
            throws SchoolDBException {
        try {
            CourseInstanceDTO instance = db.readCourseInstance(courseCode, year, period, true);
            if (instance == null) {
                db.rollback();
                return null;
            }
            CostReportDTO beforeReport = generateCostReport(instance);
            int newCount = instance.numStudents + 100;
            db.updateCourseInstanceStudents(instance, newCount); // Use DTO
            CourseInstanceDTO updatedInstance = db.readCourseInstanceById(instance.id);
            scaleAllocations(updatedInstance);
            CostReportDTO afterReport = generateCostReport(updatedInstance);
            db.commit();
            return new CostReportDTO[] { beforeReport, afterReport };
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    public void allocateTeacher(String teacherName, String courseCode, int activityId, String currentPeriod,
            int currentYear) throws SchoolDBException {
        try {
            TeacherDTO teacher = db.readTeacherByName(teacherName);
            if (teacher == null)
                throw new SchoolDBException("Teacher not found");
            CourseInstanceDTO instance = db.readCourseInstance(courseCode, currentYear, currentPeriod, true);
            if (instance == null)
                throw new SchoolDBException("Course instance not found");
            int currentLoad = db.readTeacherInstanceCountInPeriod(teacher, currentPeriod, currentYear); // Use DTO
            allocationService.validateAllocationLimit(teacherName, currentLoad);
            db.createAllocation(instance, activityId, teacher); // Use DTOs
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    public void deallocateTeacher(int allocationId) throws SchoolDBException {
        try {
            db.deleteAllocation(allocationId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    public void addExerciseActivity(String courseCode, int year, String period, double hours) throws SchoolDBException {
        try {
            CourseInstanceDTO instance = db.readCourseInstance(courseCode, year, period, true);
            if (instance == null)
                throw new SchoolDBException("Instance not found");
            db.createActivity(instance.id, "Exercise", hours);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    private CostReportDTO generateCostReport(CourseInstanceDTO instance) throws SchoolDBException {
        double avgSalary = db.readAverageTeacherSalary();
        CostReportDTO report = new CostReportDTO(instance, avgSalary);
        List<ActivityDTO> activities = db.readActivitiesForInstance(instance.id);
        for (ActivityDTO act : activities) {
            double plannedHours = act.calculateEffectiveHours(instance.numStudents, instance.hp);
            double plannedCost = plannedHours * avgSalary;
            report.totalPlannedCost += plannedCost;
            List<AllocationDTO> allocs = db.readAllocationsForActivity(instance.id, act.id);
            CostReportDTO.ActivityCost ac = new CostReportDTO.ActivityCost(act, plannedHours, plannedCost, allocs);
            report.totalActualCost += ac.actualCost;
            report.activities.add(ac);
        }
        return report;
    }

    private void scaleAllocations(CourseInstanceDTO instance) throws SchoolDBException {
        List<ActivityDTO> activities = db.readActivitiesForInstance(instance.id);
        for (ActivityDTO activity : activities) {
            double targetHours = activity.calculateEffectiveHours(instance.numStudents, instance.hp);
            List<AllocationDTO> allocations = db.readAllocationsForActivity(instance.id, activity.id);
            double currentTotal = allocations.stream().mapToDouble(a -> a.allocatedHours).sum();
            if (currentTotal > 0) {
                double scale = targetHours / currentTotal;
                for (AllocationDTO alloc : allocations) {
                    db.updateAllocationHours(alloc.id, alloc.allocatedHours * scale);
                }
            }
        }
    }
}

class TeacherAllocationService {
    public void validateAllocationLimit(String teacherName, int currentLoad) throws SchoolDBException {
        if (currentLoad >= 4) {
            throw new SchoolDBException("Allocation rejected: Teacher " + teacherName +
                    " is already assigned to " + currentLoad + " instances in this period.");
        }
    }
}
