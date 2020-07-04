package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/

import org.dase.ecii.core.HashMapUtility;
import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.Heuristics;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.OR;


/**
 * Candidate class is multiple conjunctive horn clause combined with a single Object Property.
 * <p>
 * In V0 of Candidate Class horn clauses will be combined by OR, no matter what the object property is
 *
 * <pre>
 *       K2
 * C = 􏰀  ⊔  (Bj ⊓¬(D1 ⊔...⊔Djk))
 *      j=1
 *
 * There is a limit  on how many conjunctive horn clauses may be added.
 * That is called K2.
 * k2 = limit of horn clauses = ConfigParams.hornClauseLimit.
 * </pre>
 */
public class CandidateClassV0 extends CandidateClass {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Multiple conjunctive horn clause.
     */
    public ArrayList<ConjunctiveHornClauseV0> conjunctiveHornClauses;

    /**
     * @param owlObjectProperty
     * @param _reasoner
     * @param _ontology
     */
    public CandidateClassV0(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        super(owlObjectProperty, _reasoner, _ontology);
        this.conjunctiveHornClauses = new ArrayList<>();
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateClass
     * @param _ontology
     */
    public CandidateClassV0(CandidateClassV0 anotherCandidateClass, OWLOntology _ontology) {
        super(anotherCandidateClass, _ontology);
        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);
    }

    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    public ArrayList<ConjunctiveHornClauseV0> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClauseV0> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
    }

    public void addConjunctiveHornClauses(ConjunctiveHornClauseV0 conjunctiveHornClauseV0) {
        this.conjunctiveHornClauses.add(conjunctiveHornClauseV0);
    }

    /**
     * Return the candidate class without adding the property.
     *
     * @return
     */
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        if (!solutionChanged && null != candidateClassAsOWLClassExpression)
            return candidateClassAsOWLClassExpression;

        solutionChanged = false;

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV0 conjunctiveHornClauseV0 : this.conjunctiveHornClauses) {
                conjunctiveHornClausesClassExpression.add(
                        conjunctiveHornClauseV0.getConjunctiveHornClauseAsOWLClassExpression());
            }
            ArrayList<OWLClassExpression> conjunctiveHornClausesClassExpressionAList
                    = new ArrayList<>(conjunctiveHornClausesClassExpression);

            // make union of them
            if (conjunctiveHornClausesClassExpressionAList.size() > 0) {
                if (conjunctiveHornClausesClassExpressionAList.size() == 1) {
                    owlClassExpression = conjunctiveHornClausesClassExpressionAList.get(0);
                } else {
                    // V1 of Candidate Class, always union between hornClauses
                    owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(conjunctiveHornClausesClassExpression);
                }
            }
        } else {
            logger.error("ERROR!!!!!! conjunctiveHornClauses is null, program exiting");
            System.exit(-1);
        }

        candidateClassAsOWLClassExpression = owlClassExpression;
        return owlClassExpression;
    }

    /**
     * Print candidate class as String
     *
     * @return
     */
    public String getCandidateClassAsString(boolean includePrefix) {

        solutionChanged = false;

        StringBuilder sb = new StringBuilder();

        if (null != this) {

            if (this.getConjunctiveHornClauses().size() > 0) {
                if (this.getConjunctiveHornClauses().size() == 1) {
                    sb.append("(");
                    sb.append(this.getConjunctiveHornClauses().get(0).getHornClauseAsString(includePrefix));
                    sb.append(")");
                } else {

                    sb.append("(");

                    sb.append("(");
                    sb.append(this.getConjunctiveHornClauses().get(0).getHornClauseAsString(includePrefix));
                    sb.append(")");

                    for (int i = 1; i < this.getConjunctiveHornClauses().size(); i++) {
                        // should we use OR or AND between multiple
                        // hornClauses of same object property
                        // always OR for V0
                        sb.append(" " + OR.toString() + " ");
                        sb.append("(");
                        sb.append(this.getConjunctiveHornClauses().get(i).getHornClauseAsString(includePrefix));
                        sb.append(")");
                    }

                    sb.append(")");
                }
            }
        }

        this.candidateClassAsString = sb.toString();
        return sb.toString();
    }

    /**
     * Determine whether this owlnamedIndividual contained within  any of the hornclause of this hornClauses.
     *
     * @param hornClauses
     * @param owlNamedIndividual
     * @return
     */
    public boolean isContainedInHornClauses(ArrayList<ConjunctiveHornClauseV0> hornClauses, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {

        boolean contained = false;

//        if (hornClauses.size() < 1) {
//            logger.error("Arraylist of hornClauses contains 0 hornClause.", true);
//            return false;
//        }
        OWLObjectProperty owlObjectProperty = hornClauses.get(0).getOwlObjectProperty();


        HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                individualHasObjectTypes.get(owlNamedIndividual);

        if (null != objPropsMap && objPropsMap.containsKey(owlObjectProperty)) {
            // if any horn clause of this group contains this individual then the full arraylist<hornclauses> contains this individual.
            for (ConjunctiveHornClauseV0 hornClause : hornClauses) {
                if (owlObjectProperty.equals(hornClause.getOwlObjectProperty())) {
                    if (hornClause.isContainedInHornClause(owlNamedIndividual, isPosIndiv)) {
                        contained = true;
                        return contained;
                    }
                }
            }
        }

        return contained;
    }

    /**
     * Determine whether this owlnamedIndividual contained within this candidate class.
     * This means that if any one horn clause of the horn clauses (connected to this cnadidate class)
     * contain this individual then this candidate class contain this individual.
     *
     * @param owlNamedIndividual
     * @param isPosIndiv         boolean
     * @return
     */
    public boolean isContainedInCandidateClass(OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {
        return isContainedInHornClauses(this.getConjunctiveHornClauses(), owlNamedIndividual, isPosIndiv);
    }

    /**
     * Calculate accuracy of a candidateClassV0.
     *
     * @return
     */
    public Score calculateAccuracyComplexCustom() {

        /**
         * Individuals covered by all parts of solution
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by all parts of solution
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        /**
         * For positive individuals, a individual must be contained within each AND section to be added as a coveredIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {

            if (this.isContainedInCandidateClass(
                    thisOwlNamedIndividual, true)) {
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within any single section to be added as a excludedIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            if (this.isContainedInCandidateClass(
                    thisOwlNamedIndividual, false)) {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
            }
        }

        nrOfPositiveClassifiedAsPositive = coveredPosIndividualsMap.size();
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;
        nrOfNegativeClassifiedAsNegative = excludedNegIndividualsMap.size();
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsPositive = SharedDataHolder.negIndivs.size() - nrOfNegativeClassifiedAsNegative;

        double precision = Heuristics.getPrecision(nrOfPositiveClassifiedAsPositive, nrOfNegativeClassifiedAsPositive);
        double recall = Heuristics.getRecall(nrOfPositiveClassifiedAsPositive, nrOfPositiveClassifiedAsNegative);
        double f_measure = Heuristics.getFScore(recall, precision);
        double coverage = Heuristics.getCoverage(nrOfPositiveClassifiedAsPositive, SharedDataHolder.posIndivs.size(),
                nrOfNegativeClassifiedAsNegative, SharedDataHolder.negIndivs.size());

        Score accScore = new Score();
        accScore.setPrecision(precision);
        accScore.setRecall(recall);
        accScore.setF_measure(f_measure);
        accScore.setCoverage(coverage);

        return accScore;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateClassV0 that = (CandidateClassV0) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauses), new HashSet<>(that.conjunctiveHornClauses));
    }

    @Override
    public int hashCode() {

        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauses));
    }
}
