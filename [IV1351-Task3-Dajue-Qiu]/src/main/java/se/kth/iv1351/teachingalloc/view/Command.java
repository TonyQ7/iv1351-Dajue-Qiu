/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.view;

/**
 * Defines all commands that can be performed by a user of the teaching
 * allocation application.
 */
public enum Command {
    /**
     * Compute teaching cost for a course instance.
     */
    COST,
    /**
     * Increase student count for a course instance.
     */
    INCREASE,
    /**
     * Allocate teaching to an employee.
     */
    ALLOCATE,
    /**
     * Deallocate teaching from an employee.
     */
    DEALLOCATE,
    /**
     * List entities (instances, activities, allocations).
     */
    LIST,
    /**
     * Create a new teaching activity.
     */
    NEWACTIVITY,
    /**
     * Associate an activity with a course instance.
     */
    ASSOCIATE,
    /**
     * Show allocations for a specific activity (e.g., Exercise for Task A req 4).
     */
    SHOWACTIVITY,
    /**
     * Lists all commands.
     */
    HELP,
    /**
     * Leave the application.
     */
    QUIT,
    /**
     * None of the valid commands above was specified.
     */
    ILLEGAL_COMMAND
}
