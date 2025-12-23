package se.kth.iv1351.teachingalloc.startup;

import se.kth.iv1351.teachingalloc.controller.Controller;
import se.kth.iv1351.teachingalloc.view.BlockingInterpreter;

/**
 * Starts the teaching allocation application.
 */
public class Main {
    public static void main(String[] args) {
        try {
            new BlockingInterpreter(new Controller()).handleCmds();
        } catch (Exception e) {
            System.out.println("Application error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
