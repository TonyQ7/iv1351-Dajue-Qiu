/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * A teaching activity type in the system.
 */
public class TeachingActivity implements TeachingActivityDTO {
    private int activityId;
    private String activityName;
    private BigDecimal factor;
    private boolean isDerived;

    /**
     * Creates a teaching activity with all fields.
     */
    public TeachingActivity(int activityId, String activityName, BigDecimal factor, boolean isDerived) {
        this.activityId = activityId;
        this.activityName = activityName;
        this.factor = factor;
        this.isDerived = isDerived;
    }

    /**
     * Creates a new teaching activity (without ID, for creation).
     */
    public TeachingActivity(String activityName, BigDecimal factor, boolean isDerived) {
        this(0, activityName, factor, isDerived);
    }

    @Override
    public int getActivityId() {
        return activityId;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    @Override
    public BigDecimal getFactor() {
        return factor;
    }

    @Override
    public boolean isDerived() {
        return isDerived;
    }

    @Override
    public String toString() {
        return "Activity[id=" + activityId + ", name=" + activityName +
                ", factor=" + factor + ", derived=" + isDerived + "]";
    }
}
