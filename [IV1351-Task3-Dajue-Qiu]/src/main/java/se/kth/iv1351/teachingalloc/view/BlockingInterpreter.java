/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 *
 * Based on the JDBC Bank example by Leif Lindbäck.
 */

package se.kth.iv1351.teachingalloc.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import se.kth.iv1351.teachingalloc.controller.Controller;
import se.kth.iv1351.teachingalloc.model.CostSummaryDTO;
import se.kth.iv1351.teachingalloc.model.CourseInstanceDTO;
import se.kth.iv1351.teachingalloc.model.TeachingActivityDTO;
import se.kth.iv1351.teachingalloc.model.TeachingAllocationDTO;
import se.kth.iv1351.teachingalloc.model.ActivityAllocationDTO;

/**
 * Reads and interprets user commands. This command interpreter is blocking, the
 * user
 * interface does not react to user input while a command is being executed.
 */
public class BlockingInterpreter {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private Controller ctrl;
    private boolean keepReceivingCmds = false;

    /**
     * Creates a new instance that will use the specified controller for all
     * operations.
     * 
     * @param ctrl The controller used by this instance.
     */
    public BlockingInterpreter(Controller ctrl) {
        this.ctrl = ctrl;
    }

    /**
     * Stops the command interpreter.
     */
    public void stop() {
        keepReceivingCmds = false;
    }

    /**
     * Interprets and performs user commands. This method will not return until the
     * UI has been stopped. The UI is stopped either when the user gives the
     * "quit" command, or when the method <code>stop()</code> is called.
     */
    public void handleCmds() {
        keepReceivingCmds = true;
        System.out.println("Teaching Allocation System - Type 'help' for commands");
        while (keepReceivingCmds) {
            try {
                CmdLine cmdLine = new CmdLine(readNextLine());
                switch (cmdLine.getCmd()) {
                    case HELP:
                        printHelp();
                        break;
                    case QUIT:
                        keepReceivingCmds = false;
                        break;
                    case LIST:
                        handleList(cmdLine);
                        break;
                    case COST:
                        handleCost(cmdLine);
                        break;
                    case INCREASE:
                        handleIncrease(cmdLine);
                        break;
                    case ALLOCATE:
                        handleAllocate(cmdLine);
                        break;
                    case DEALLOCATE:
                        handleDeallocate(cmdLine);
                        break;
                    case NEWACTIVITY:
                        handleNewActivity(cmdLine);
                        break;
                    case ASSOCIATE:
                        handleAssociate(cmdLine);
                        break;
                    case SHOWACTIVITY:
                        handleShowActivity(cmdLine);
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help                                           - Show this help");
        System.out.println("  quit                                           - Exit the application");
        System.out.println("  list instances                                 - List all course instances");
        System.out.println("  list instances <year>                          - List instances for year (Task A)");
        System.out.println("  list activities                                - List all teaching activities");
        System.out.println("  list allocations <instance_id>                 - List allocations for instance");
        System.out.println("  list teacher <emp_id> <period> <year>          - List allocations for teacher");
        System.out.println("  cost <instance_id>                             - Compute teaching cost");
        System.out.println("  increase <instance_id> <count>                 - Increase student count");
        System.out.println("  allocate <emp_id> <instance_id> <act_id> <hrs> - Allocate teaching");
        System.out.println("  deallocate <emp_id> <instance_id> <act_id>     - Remove allocation");
        System.out.println("  newactivity <name> <factor>                    - Create teaching activity");
        System.out.println("  associate <instance_id> <act_id> <hours>       - Add planned activity");
        System.out
                .println("  showactivity <activity_name>                   - Show activity allocations (Task A req 4)");
        System.out.println();
    }

    private void handleList(CmdLine cmdLine) throws Exception {
        String subCmd = cmdLine.getParameter(0);
        if (subCmd == null) {
            System.out.println("Usage: list <instances|activities|allocations|teacher|instances <year>>");
            return;
        }

        switch (subCmd.toLowerCase()) {
            case "instances":
                // Check if year filter provided: list instances <year>
                String yearParam = cmdLine.getParameter(1);
                List<? extends CourseInstanceDTO> instances;
                if (yearParam != null) {
                    int year = Integer.parseInt(yearParam);
                    instances = ctrl.getCourseInstancesByYear(year);
                    System.out.println("\nCourse Instances for " + year + ":");
                } else {
                    instances = ctrl.getAllCourseInstances();
                    System.out.println("\nCourse Instances:");
                }
                System.out.println("--------------------------------------------------------------------------------");
                System.out.printf("%-15s %-10s %-30s %-6s %-4s %-8s%n",
                        "Instance ID", "Code", "Name", "Year", "Per", "Students");
                System.out.println("--------------------------------------------------------------------------------");
                for (CourseInstanceDTO inst : instances) {
                    System.out.printf("%-15s %-10s %-30s %-6d %-4s %-8d%n",
                            inst.getInstanceId(),
                            inst.getCourseCode(),
                            truncate(inst.getCourseName(), 30),
                            inst.getStudyYear(),
                            inst.getStudyPeriod(),
                            inst.getNumStudents());
                }
                System.out.println();
                break;

            case "activities":
                List<? extends TeachingActivityDTO> activities = ctrl.getAllTeachingActivities();
                System.out.println("\nTeaching Activities:");
                System.out.println("--------------------------------------------------------");
                System.out.printf("%-5s %-25s %-10s %-10s%n", "ID", "Name", "Factor", "Derived");
                System.out.println("--------------------------------------------------------");
                for (TeachingActivityDTO act : activities) {
                    System.out.printf("%-5d %-25s %-10.2f %-10s%n",
                            act.getActivityId(),
                            act.getActivityName(),
                            act.getFactor(),
                            act.isDerived() ? "Yes" : "No");
                }
                System.out.println();
                break;

            case "allocations":
                String instanceId = cmdLine.getParameter(1);
                if (instanceId == null) {
                    System.out.println("Usage: list allocations <instance_id>");
                    return;
                }
                List<? extends TeachingAllocationDTO> allocations = ctrl.getInstanceAllocations(instanceId);
                printAllocations(allocations, "Instance " + instanceId);
                break;

            case "teacher":
                if (cmdLine.getParamCount() < 4) {
                    System.out.println("Usage: list teacher <emp_id> <period> <year>");
                    return;
                }
                int empId = Integer.parseInt(cmdLine.getParameter(1));
                String period = cmdLine.getParameter(2);
                int year = Integer.parseInt(cmdLine.getParameter(3));
                List<? extends TeachingAllocationDTO> teacherAllocs = ctrl.getTeacherAllocations(empId, period, year);
                printAllocations(teacherAllocs, "Employee " + empId + " in " + period + " " + year);
                break;

            default:
                System.out.println("Unknown list type. Use: instances, activities, allocations, teacher");
        }
    }

    private void printAllocations(List<? extends TeachingAllocationDTO> allocations, String context) {
        System.out.println("\nAllocations for " + context + ":");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-8s %-15s %-8s %-20s %-10s%n",
                "Emp ID", "Instance", "Act ID", "Activity", "Hours");
        System.out.println("--------------------------------------------------------------------------------");
        for (TeachingAllocationDTO alloc : allocations) {
            System.out.printf("%-8d %-15s %-8d %-20s %-10.2f%n",
                    alloc.getEmployeeId(),
                    alloc.getCourseInstanceId(),
                    alloc.getActivityId(),
                    truncate(alloc.getActivityName(), 20),
                    alloc.getAllocatedHours());
        }
        System.out.println();
    }

    private void handleCost(CmdLine cmdLine) throws Exception {
        String instanceId = cmdLine.getParameter(0);
        if (instanceId == null) {
            System.out.println("Usage: cost <instance_id>");
            return;
        }

        CostSummaryDTO cost = ctrl.computeTeachingCost(instanceId);
        System.out.println("\nTeaching Cost Summary:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-12s %-15s %-8s %-18s %-18s%n",
                "Course Code", "Instance", "Period", "Planned (KSEK)", "Actual (KSEK)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-12s %-15s %-8s %-18.2f %-18.2f%n",
                cost.getCourseCode(),
                cost.getInstanceId(),
                cost.getPeriod(),
                cost.getPlannedCostKSEK(),
                cost.getActualCostKSEK());
        System.out.println();
    }

    private void handleIncrease(CmdLine cmdLine) throws Exception {
        if (cmdLine.getParamCount() < 2) {
            System.out.println("Usage: increase <instance_id> <count>");
            return;
        }

        String instanceId = cmdLine.getParameter(0);
        int count = Integer.parseInt(cmdLine.getParameter(1));

        ctrl.increaseStudentCount(instanceId, count);
        System.out.println("Student count increased by " + count + " for instance " + instanceId);

        // Show updated cost
        CostSummaryDTO cost = ctrl.computeTeachingCost(instanceId);
        System.out.println("New planned cost: " + cost.getPlannedCostKSEK() + " KSEK");
    }

    private void handleAllocate(CmdLine cmdLine) throws Exception {
        if (cmdLine.getParamCount() < 4) {
            System.out.println("Usage: allocate <emp_id> <instance_id> <activity_id> <hours>");
            return;
        }

        int empId = Integer.parseInt(cmdLine.getParameter(0));
        String instanceId = cmdLine.getParameter(1);
        int activityId = Integer.parseInt(cmdLine.getParameter(2));
        BigDecimal hours = new BigDecimal(cmdLine.getParameter(3));

        ctrl.allocateTeaching(empId, instanceId, activityId, hours);
        System.out.println("Successfully allocated " + hours + " hours to employee " + empId);
    }

    private void handleDeallocate(CmdLine cmdLine) throws Exception {
        if (cmdLine.getParamCount() < 3) {
            System.out.println("Usage: deallocate <emp_id> <instance_id> <activity_id>");
            return;
        }

        int empId = Integer.parseInt(cmdLine.getParameter(0));
        String instanceId = cmdLine.getParameter(1);
        int activityId = Integer.parseInt(cmdLine.getParameter(2));

        ctrl.deallocateTeaching(empId, instanceId, activityId);
        System.out.println("Successfully deallocated teaching from employee " + empId);
    }

    private void handleNewActivity(CmdLine cmdLine) throws Exception {
        if (cmdLine.getParamCount() < 2) {
            System.out.println("Usage: newactivity <name> <factor>");
            return;
        }

        String name = cmdLine.getParameter(0);
        BigDecimal factor = new BigDecimal(cmdLine.getParameter(1));

        ctrl.createTeachingActivity(name, factor);
        System.out.println("Successfully created teaching activity: " + name);
    }

    private void handleAssociate(CmdLine cmdLine) throws Exception {
        if (cmdLine.getParamCount() < 3) {
            System.out.println("Usage: associate <instance_id> <activity_id> <hours>");
            return;
        }

        String instanceId = cmdLine.getParameter(0);
        int activityId = Integer.parseInt(cmdLine.getParameter(1));
        BigDecimal hours = new BigDecimal(cmdLine.getParameter(2));

        ctrl.associateActivityWithInstance(instanceId, activityId, hours);
        System.out.println("Successfully associated activity " + activityId + " with instance " + instanceId);
    }

    /**
     * Handles the showactivity command for Task A requirement 4.
     * Displays all allocations for a specific activity (e.g., Exercise).
     */
    private void handleShowActivity(CmdLine cmdLine) throws Exception {
        String activityName = cmdLine.getParameter(0);
        if (activityName == null) {
            System.out.println("Usage: showactivity <activity_name>");
            System.out.println("Example: showactivity Exercise");
            return;
        }

        List<? extends ActivityAllocationDTO> allocations = ctrl.getAllocationsByActivityName(activityName);

        if (allocations.isEmpty()) {
            System.out.println("No allocations found for activity: " + activityName);
            return;
        }

        System.out.println("\nAllocations for Activity '" + activityName + "':");
        System.out.println(
                "--------------------------------------------------------------------------------------------");
        System.out.printf("%-8s %-20s %-15s %-25s %-10s %-6s %-4s%n",
                "Emp ID", "Teacher Name", "Instance", "Course", "Hours", "Year", "Per");
        System.out.println(
                "--------------------------------------------------------------------------------------------");
        for (ActivityAllocationDTO alloc : allocations) {
            System.out.printf("%-8d %-20s %-15s %-25s %-10.2f %-6d %-4s%n",
                    alloc.getEmployeeId(),
                    truncate(alloc.getEmployeeName(), 20),
                    alloc.getCourseInstanceId(),
                    truncate(alloc.getCourseName(), 25),
                    alloc.getAllocatedHours(),
                    alloc.getStudyYear(),
                    alloc.getStudyPeriod());
        }
        System.out.println();
    }

    private String truncate(String s, int maxLen) {
        if (s == null)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    private String readNextLine() {
        System.out.print(PROMPT);
        return console.nextLine();
    }
}
