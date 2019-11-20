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
 * Candidate class is multiple conjunctive horn clause with a single Object Property.
 * 1. When we have none ObjectProperty or bare types, then horn clauses will be combined by AND/Intersection
 * 2. When we have proper ObjectProperty, then hornclauses will be combined with OR/Disjunction
 * <p>
 * Probable Solution:
 * Solution = (A1 ¬(D1)) ⊓ (A2 ¬(D1))  ⊓  R1.((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )
 * <p>
 * Here,
 * (A1 ¬(D1)) ⊓ (A2 ¬(D1)) can be a candidate class
 * (A1 ¬(D1)) can be a candidate class
 * ((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )  can be a candidate class
 * (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)  can be a candidate class
 *
 * <pre>
 *       K2
 * C = 􏰀  ⊔  (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk))
 *      j=1
 *
 *  Here, (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) is a conjunctive HornClause.
 *
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

    private OWLClassExpression candidateClassAsOWLClassEXpression;

    private String candidateClassAsString;

    private boolean solutionChanged = false;

    /**
     * Public constructor
     *
     * @param owlObjectProperty
     */
    public CandidateClassV1(OWLObjectProperty owlObjectProperty) {
        this.owlObjectProperty = owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>();
        solutionChanged = true;
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateClass
     */
    public CandidateClassV1(CandidateClassV1 anotherCandidateClass) {
        this.owlObjectProperty = anotherCandidateClass.owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);
        solutionChanged = true;
    }

    /**
     * owlObjectProperty getter
     *
     * @return OWLObjectProperty
     */
    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

//    /**
//     * we should not allow to set owlObjectProperty
//     * @param owlObjectProperty
//     */
//    public void setOwlObjectProperty(OWLObjectProperty owlObjectProperty) {
//        this.owlObjectProperty = owlObjectProperty;
//        solutionChanged = true;
//    }

    /**
     * @return ArrayList<ConjunctiveHornClauseV1>
     */
    public ArrayList<ConjunctiveHornClauseV1> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    /**
     * @param conjunctiveHornClauses
     */
    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClauseV1> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
        solutionChanged = true;
    }

    /**
     * @param conjunctiveHornClause
     */
    public void addConjunctiveHornClauses(ConjunctiveHornClauseV1 conjunctiveHornClause) {
        this.conjunctiveHornClauses.add(conjunctiveHornClause);
        solutionChanged = true;
    }

    /**
     * @return
     */
    public Score getScore() {
        return score;
    }

    /**
     * @param score
     */
    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * multiple horclauses are conjuncted when we have hare object property, and unioned when we have proper object property.
     * v0,v1 both okay.
     *
     * @return OWLClassExpression
     */
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        if (!solutionChanged && null != candidateClassAsOWLClassEXpression)
            return candidateClassAsOWLClassEXpression;

        solutionChanged = false;

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV1 conjunctiveHornClause : this.conjunctiveHornClauses) {
                conjunctiveHornClausesClassExpression.add(conjunctiveHornClause.getConjunctiveHornClauseAsOWLClassExpression());
            }
            ArrayList<OWLClassExpression> conjunctiveHornClausesClassExpressionAList = new ArrayList<>(conjunctiveHornClausesClassExpression);

            if (conjunctiveHornClausesClassExpressionAList.size() > 0) {
                if (conjunctiveHornClausesClassExpressionAList.size() == 1) {
                    owlClassExpression = conjunctiveHornClausesClassExpressionAList.get(0);
                } else {
                    // bug-fix: for multiple conjunctivehornClause
                    // todo: (zaman): how do we handle the solution of ((SouthAsia) ⊔ (DevelopedAsia)) !!!!!!!!!
                    if (this.owlObjectProperty != SharedDataHolder.noneOWLObjProp) {
                        // make union of them
                        owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(conjunctiveHornClausesClassExpression);
                    } else {
                        // make conjunction of them
                        // todo(zaman): verify!!!!!!
                        owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(conjunctiveHornClausesClassExpression);
                    }
                }
            }
        }

        candidateClassAsOWLClassEXpression = owlClassExpression;
        return candidateClassAsOWLClassEXpression;
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
