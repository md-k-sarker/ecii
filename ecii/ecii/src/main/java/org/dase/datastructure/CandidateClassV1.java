package org.dase.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/

import org.dase.core.Score;
import org.dase.core.SharedDataHolder;
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
public class CandidateClassV1 {

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    private OWLObjectProperty owlObjectProperty;
    /**
     * Multiple conjunctive horn clause.
     */
    private ArrayList<ConjunctiveHornClauseV1> conjunctiveHornClauses;

    /**
     * Score associated with this CandidateClass. This score is used to select best n candidateClass (limit K6), which will be used on combination.
     */
    private Score score;

    public CandidateClassV1(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>();
    }

    public CandidateClassV1(CandidateClassV1 anotherCandidateClass) {
        this.owlObjectProperty = anotherCandidateClass.owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);
    }

    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    public void setOwlObjectProperty(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
    }

    public ArrayList<ConjunctiveHornClauseV1> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClauseV1> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
    }

    public void addConjunctiveHornClauses(ConjunctiveHornClauseV1 conjunctiveHornClause) {
        this.conjunctiveHornClauses.add(conjunctiveHornClause);
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * v0,v1 both okay.
     * @return
     */
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV1 conjunctiveHornClause : this.conjunctiveHornClauses) {
                conjunctiveHornClausesClassExpression.add(conjunctiveHornClause.getConjunctiveHornClauseAsOWLClassExpression());
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
        CandidateClassV1 that = (CandidateClassV1) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauses), new HashSet<>(that.conjunctiveHornClauses));
    }

    @Override
    public int hashCode() {

        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauses));
    }
}
