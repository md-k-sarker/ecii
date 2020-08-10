package org.dase.ecii.core;

public enum ScoreType {
    HYBRID, PRECISION, RECALL, F_MEASURE, COVERAGE, PRECISION_by_REASONER, RECALL_by_REASONER, F_MEASURE_by_REASONER, COVERAGE_by_REASONER;


    @Override
    public String toString() {
        switch (this) {
            case HYBRID:
                return "Precision + Recall + Coverage";
            case PRECISION:
                return "PRECISION";
            case RECALL:
                return "RECALL";
            case F_MEASURE:
                return "F1_MEASURE";
            case COVERAGE:
                return "COVERAGE";
            case PRECISION_by_REASONER:
                return "Precision calculated by reasoner";
            case RECALL_by_REASONER:
                return "Recall calculated by reasoner";
            case F_MEASURE_by_REASONER:
                return "F_measure calculated by reasoner";
            case COVERAGE_by_REASONER:
                return "Coverage calculated by reasoner";
            default:
                throw new IllegalArgumentException();
        }
    }
}
