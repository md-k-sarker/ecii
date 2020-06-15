package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/2/18.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


public class Score {


    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Default Score type. Over the runtime of a single execution this score type must be same.
     */
    public static ScoreType defaultScoreType;


    /**
     * default is -1
     *
     * @return
     */
    public double getDefaultScoreValue() {
        if (null == defaultScoreType) {
            logger.error("defaultScoreType is null!!!!!!!!!!!!. Need to set defaultScoreType first.");
            return -1;
        }
        double value;
        logger.debug("Default Score type: " + defaultScoreType);
        switch (defaultScoreType) {
            case HYBRID:
                value = precision + recall + coverage;
                break;
            case PRECISION:
                value = precision;
                break;
            case RECALL:
                value = recall;
                break;
            case F_MEASURE:
                value = f_measure;
                break;
            case COVERAGE:
                value = coverage;
                break;
            case PRECISION_by_REASONER:
                value = precision_by_reasoner;
                break;
            case RECALL_by_REASONER:
                value = recall_by_reasoner;
                break;
            case F_MEASURE_by_REASONER:
                value = f_measure_by_reasoner;
                break;
            case COVERAGE_by_REASONER:
                value = coverage_by_reasoner;
                break;
            default:
                value = -1;
        }
        return value;
    }

    double precision;
    double recall;

    double f_measure;

    double coverage;

    double precision_by_reasoner;
    double recall_by_reasoner;

    double f_measure_by_reasoner;

    double coverage_by_reasoner;


    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getF_measure() {
        return f_measure;
    }

    public void setF_measure(double f_measure) {
        this.f_measure = f_measure;
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public double getPrecision_by_reasoner() {
        return precision_by_reasoner;
    }

    public void setPrecision_by_reasoner(double precision_by_reasoner) {
        this.precision_by_reasoner = precision_by_reasoner;
    }

    public double getRecall_by_reasoner() {
        return recall_by_reasoner;
    }

    public void setRecall_by_reasoner(double recall_by_reasoner) {
        this.recall_by_reasoner = recall_by_reasoner;
    }

    public double getF_measure_by_reasoner() {
        return f_measure_by_reasoner;
    }

    public void setF_measure_by_reasoner(double f_measure_by_reasoner) {
        this.f_measure_by_reasoner = f_measure_by_reasoner;
    }

    public double getCoverage_by_reasoner() {
        return coverage_by_reasoner;
    }

    public void setCoverage_by_reasoner(double coverage_by_reasoner) {
        this.coverage_by_reasoner = coverage_by_reasoner;
    }

}
