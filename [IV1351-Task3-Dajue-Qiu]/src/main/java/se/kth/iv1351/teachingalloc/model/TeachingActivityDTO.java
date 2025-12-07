/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * Specifies a read-only view of a teaching activity.
 */
public interface TeachingActivityDTO {
    /**
     * @return The activity ID.
     */
    int getActivityId();

    /**
     * @return The activity name.
     */
    String getActivityName();

    /**
     * @return The factor multiplier for hours.
     */
    BigDecimal getFactor();

    /**
     * @return True if this is a derived activity.
     */
    boolean isDerived();
}
