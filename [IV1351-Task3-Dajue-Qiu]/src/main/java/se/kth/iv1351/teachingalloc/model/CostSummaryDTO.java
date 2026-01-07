package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * Specifies a read-only view of a cost summary.
 */
public interface CostSummaryDTO {
    /**
     * @return The course code.
     */
    String getCourseCode();

    /**
     * @return The course instance ID.
     */
    String getInstanceId();

    /**
     * @return The study period.
     */
    String getPeriod();

    /**
     * @return The planned cost in KSEK.
     */
    BigDecimal getPlannedCostKSEK();

    /**
     * @return The actual cost in KSEK.
     */
    BigDecimal getActualCostKSEK();
}

