package org.dase.ecii.datastructure;

import org.dase.ecii.core.Score;
import org.semanticweb.owlapi.model.OWLClassExpression;

public interface IConjunctiveHornClause {

    Score getScore();

    void setScore(Score score);

    OWLClassExpression getConjunctiveHornClauseAsOWLClassExpression();

    String getHornClauseAsString(boolean includePrefix);

    public abstract Score calculateAccuracyComplexCustom();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
