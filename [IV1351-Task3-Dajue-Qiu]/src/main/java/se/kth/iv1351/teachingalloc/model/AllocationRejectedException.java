package se.kth.iv1351.teachingalloc.model;

/**
 * Thrown when an allocation or deallocation operation is rejected due to
 * business rule violations (e.g., exceeding the 4-course limit).
 */
public class AllocationRejectedException extends Exception {

    /**
     * Create a new instance thrown because of the specified reason.
     *
     * @param reason Why the exception was thrown.
     */
    public AllocationRejectedException(String reason) {
        super(reason);
    }

    /**
     * Create a new instance thrown because of the specified reason and exception.
     */
    public AllocationRejectedException(String reason, Throwable rootCause) {
        super(reason, rootCause);
    }
}

