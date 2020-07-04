package org.dase.ecii.datastructure;

import org.dase.ecii.core.Score;
import org.semanticweb.owlapi.model.OWLClassExpression;

public interface ICandidateSolution {

    Score getScore();

    void setScore(Score score);

    OWLClassExpression getSolutionAsOWLClassExpression();

    String getSolutionAsString(boolean includePrefix);

    Score calculateAccuracyComplexCustom();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
