package org.dase.ecii.core;

import org.semanticweb.owlapi.model.OWLObjectProperty;

public interface ICandidateSolutionFinder {
    public void removeCommonTypesFromPosAndNeg(OWLObjectProperty owlObjectProperty);
}
