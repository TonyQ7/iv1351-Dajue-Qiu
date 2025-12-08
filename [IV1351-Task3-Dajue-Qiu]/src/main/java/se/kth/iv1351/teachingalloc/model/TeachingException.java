package se.kth.iv1351.teachingalloc.model;

/**
 * Thrown when a teaching operation fails.
 */
public class TeachingException extends Exception {

    /**
     * Create a new instance thrown because of the specified reason.
     *
     * @param reason Why the exception was thrown.
     */
    public TeachingException(String reason) {
        super(reason);
    }

    /**
     * Create a new instance thrown because of the specified reason and exception.
     */
    public TeachingException(String reason, Throwable rootCause) {
        super(reason, rootCause);
    }
}

