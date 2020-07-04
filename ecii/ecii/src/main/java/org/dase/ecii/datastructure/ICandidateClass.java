package org.dase.ecii.datastructure;

import org.dase.ecii.core.Score;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public interface ICandidateClass {

    OWLObjectProperty getOwlObjectProperty();

    Score getScore();

    void setScore(Score score);

    OWLClassExpression getCandidateClassAsOWLClassExpression();

    String getCandidateClassAsString(boolean includePrefix);

    Score calculateAccuracyComplexCustom();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
