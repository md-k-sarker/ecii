package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateSolution;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.ArrayList;

public interface ICandidateSolutionFinder {
    abstract void initVariables();

    abstract void findConcepts(double tolerance, int combinationLimit);

    void extractObjectTypes(double tolerance, OWLObjectProperty owlObjectProperty);

    abstract void removeCommonTypesFromPosAndNeg(OWLObjectProperty owlObjectProperty);

    void createAndSaveSolutions();

    void calculateAccuracyOfTopK6ByReasoner(ArrayList<? extends CandidateSolution> sortedCandidateSolutionList, int K6);

    void printSolutions(int K6);
}
