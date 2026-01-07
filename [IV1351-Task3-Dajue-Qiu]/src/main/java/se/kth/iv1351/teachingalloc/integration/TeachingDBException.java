package se.kth.iv1351.teachingalloc.integration;

/**
 * Thrown when a call to the teaching database fails.
 */
public class TeachingDBException extends Exception {

    /**
     * Create a new instance thrown because of the specified reason.
     */
    public TeachingDBException(String reason) {
        super(reason);
    }

    /**
     * Create a new instance thrown because of the specified reason and exception.
     */
    public TeachingDBException(String reason, Throwable rootCause) {
        super(reason, rootCause);
    }
}

