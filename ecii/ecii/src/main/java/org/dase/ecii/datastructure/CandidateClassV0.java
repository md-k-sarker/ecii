package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;


/**
 * Candidate class is multiple conjunctive horn clause combined with OR/Disjunction with a single Object Property.
 *
 * <pre>
 *       K2
 * C = 􏰀  ⊔  (Bj ⊓¬(D1 ⊔...⊔Djk))
 *      j=1
 *
 * There is a limit  on how many conjunctive horn clauses may be added.
 * That is called K2.
 * k2 = limit of horn clauses. = ConfigParams.hornClauseLimit.
 * </pre>
 */
public class CandidateClassV0 {

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    private OWLObjectProperty owlObjectProperty;
    /**
     * Multiple conjunctive horn clause.
     */
    private ArrayList<ConjunctiveHornClauseV0> conjunctiveHornClauseV0s;

    /**
     * Score associated with this CandidateClassV0. This score is used to select best n candidateClass (limit K6), which will be used on combination.
     */
    private Score score;

    public CandidateClassV0(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
        this.conjunctiveHornClauseV0s = new ArrayList<>();
    }

    public CandidateClassV0(CandidateClassV0 anotherCandidateClassV0) {
        this.owlObjectProperty = anotherCandidateClassV0.owlObjectProperty;
        this.conjunctiveHornClauseV0s = new ArrayList<>(anotherCandidateClassV0.conjunctiveHornClauseV0s);
    }

    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    public void setOwlObjectProperty(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
    }

    public ArrayList<ConjunctiveHornClauseV0> getConjunctiveHornClauseV0s() {
        return conjunctiveHornClauseV0s;
    }

    public void setConjunctiveHornClauseV0s(ArrayList<ConjunctiveHornClauseV0> conjunctiveHornClauseV0s) {
        this.conjunctiveHornClauseV0s = conjunctiveHornClauseV0s;
    }

    public void addConjunctiveHornClauses(ConjunctiveHornClauseV0 conjunctiveHornClauseV0) {
        this.conjunctiveHornClauseV0s.add(conjunctiveHornClauseV0);
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * @return
     */
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauseV0s) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV0 conjunctiveHornClauseV0 : this.conjunctiveHornClauseV0s) {
                conjunctiveHornClausesClassExpression.add(conjunctiveHornClauseV0.getConjunctiveHornClauseAsOWLClassExpression());
            }
            ArrayList<OWLClassExpression> conjunctiveHornClausesClassExpressionAList = new ArrayList<>(conjunctiveHornClausesClassExpression);

            // make union of them
            if (conjunctiveHornClausesClassExpressionAList.size() > 0) {
                if (conjunctiveHornClausesClassExpressionAList.size() == 1) {
                    owlClassExpression = conjunctiveHornClausesClassExpressionAList.get(0);
                } else {
                    owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(conjunctiveHornClausesClassExpression);
                }
            }
        }

        return owlClassExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateClassV0 that = (CandidateClassV0) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauseV0s), new HashSet<>(that.conjunctiveHornClauseV0s));
    }

    @Override
    public int hashCode() {

        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauseV0s));
    }
}
