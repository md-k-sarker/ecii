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
public class CandidateClass {

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    private OWLObjectProperty owlObjectProperty;
    /**
     * Multiple conjunctive horn clause.
     */
    private ArrayList<ConjunctiveHornClause> conjunctiveHornClauses;

    /**
     * Score associated with this CandidateClass. This score is used to select best n candidateClass (limit K6), which will be used on combination.
     */
    private Score score;

    public CandidateClass(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>();
    }

    public CandidateClass(CandidateClass anotherCandidateClass) {
        this.owlObjectProperty = anotherCandidateClass.owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);
    }

    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    public void setOwlObjectProperty(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
    }

    public ArrayList<ConjunctiveHornClause> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClause> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
    }

    public void addConjunctiveHornClauses(ConjunctiveHornClause conjunctiveHornClause) {
        this.conjunctiveHornClauses.add(conjunctiveHornClause);
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

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClause conjunctiveHornClause : this.conjunctiveHornClauses) {
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
        CandidateClass that = (CandidateClass) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauses), new HashSet<>(that.conjunctiveHornClauses));
    }

    @Override
    public int hashCode() {

        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauses));
    }
}
