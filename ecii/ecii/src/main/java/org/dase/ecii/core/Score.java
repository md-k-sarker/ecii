package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/2/18.
*/

public class Score {

    double precision;
    double recall;

    double f_measure;

    double coverage;

    double precision_by_reasoner;
    double recall_by_reasoner;

    double f_measure_by_reasoner;

    double coverage_by_reasoner;

    public Score() {

    }

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
