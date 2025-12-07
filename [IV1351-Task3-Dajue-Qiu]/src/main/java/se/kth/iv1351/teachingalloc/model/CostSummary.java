/*
 * The MIT License (MIT)
 * Copyright (c) 2024 Dajue Qiu
 */

package se.kth.iv1351.teachingalloc.model;

import java.math.BigDecimal;

/**
 * A cost summary for a course instance.
 */
public class CostSummary implements CostSummaryDTO {
    private String courseCode;
    private String instanceId;
    private String period;
    private BigDecimal plannedCostKSEK;
    private BigDecimal actualCostKSEK;

    /**
     * Creates a cost summary with all fields.
     */
    public CostSummary(String courseCode, String instanceId, String period,
            BigDecimal plannedCostKSEK, BigDecimal actualCostKSEK) {
        this.courseCode = courseCode;
        this.instanceId = instanceId;
        this.period = period;
        this.plannedCostKSEK = plannedCostKSEK;
        this.actualCostKSEK = actualCostKSEK;
    }

    @Override
    public String getCourseCode() {
        return courseCode;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getPeriod() {
        return period;
    }

    @Override
    public BigDecimal getPlannedCostKSEK() {
        return plannedCostKSEK;
    }

    @Override
    public BigDecimal getActualCostKSEK() {
        return actualCostKSEK;
    }

    @Override
    public String toString() {
        return "CostSummary[code=" + courseCode + ", instance=" + instanceId +
                ", period=" + period + ", planned=" + plannedCostKSEK +
                " KSEK, actual=" + actualCostKSEK + " KSEK]";
    }
}
