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

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.AND;


/**
 * <pre>
 * Candidate class is multiple conjunctive horn clause with a single Object Property.
 * Difference between V2 and V1:
 * In V2 horn clauses will be combined by AND/Intersection always,
 * no matter what condition we have........
 *      1. When we have none ObjectProperty or bare types, or
 *      2. When we have proper ObjectProperty, it doesn't matter
 *
 * In V1 of Candidate Class we have AND for none and OR for proper object property
 *
 * Probable Solution:
 * Solution = (A1 ¬(D1)) ⊓ (A2 ¬(D1))  ⊓  R1.((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊓ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )
 *
 * Here,
 * (A1 ¬(D1)) ⊓ (A2 ¬(D1)) can be a candidate class
 * (A1 ¬(D1)) can be a candidate class
 * ((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊓ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )  can be a candidate class
 * (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)  can be a candidate class
 *
 *
 *       K2
 * C = 􏰀  ⊔  (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk))
 *      j=1
 *
 *  Here, (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) is a conjunctive HornClause.
 *
 *
 * There is a limit on how many conjunctive horn clauses may be added.
 * That is called K2.
 * k2 = limit of horn clauses = ConfigParams.hornClauseLimit.
 * </pre>
 */
public class CandidateClassV2 extends CandidateClass {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Multiple conjunctive horn clause.
     */
    public ArrayList<ConjunctiveHornClauseV1V2> conjunctiveHornClauses;

    /**
     * Public constructor
     *
     * @param owlObjectProperty
     */
    public CandidateClassV2(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        super(owlObjectProperty, _reasoner, _ontology);

        this.conjunctiveHornClauses = new ArrayList<>();
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateClass
     */
    public CandidateClassV2(CandidateClassV2 anotherCandidateClass, OWLOntology _ontology) {
        super(anotherCandidateClass, _ontology);

        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);
    }

    /**
     * @return ArrayList<ConjunctiveHornClauseV1V2>
     */
    public ArrayList<ConjunctiveHornClauseV1V2> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    /**
     * @param conjunctiveHornClauses
     */
    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClauseV1V2> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
        solutionChanged = true;
    }

    /**
     * @param conjunctiveHornClause
     */
    public void addConjunctiveHornClauses(ConjunctiveHornClauseV1V2 conjunctiveHornClause) {
        this.conjunctiveHornClauses.add(conjunctiveHornClause);
        solutionChanged = true;
    }

    /**
     * Return the candidate class without adding the property.
     * <p>
     * Multiple horclauses are conjuncted always
     * <p>
     * Not filling the r filler/owlObjectProperty here.
     * v0,v1 both okay.
     *
     * @return OWLClassExpression
     */
    @Override
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        if (!solutionChanged && null != candidateClassAsOWLClassExpression)
            return candidateClassAsOWLClassExpression;

        solutionChanged = false;

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV1V2 conjunctiveHornClause : this.conjunctiveHornClauses) {
                conjunctiveHornClausesClassExpression.add(conjunctiveHornClause.getConjunctiveHornClauseAsOWLClassExpression());
            }
            ArrayList<OWLClassExpression> conjunctiveHornClausesClassExpressionAList = new ArrayList<>(conjunctiveHornClausesClassExpression);

            if (conjunctiveHornClausesClassExpressionAList.size() > 0) {
                if (conjunctiveHornClausesClassExpressionAList.size() == 1) {
                    owlClassExpression = conjunctiveHornClausesClassExpressionAList.get(0);
//                    this.candidateClassAsOWLClassEXpression = owlClassExpression;
                } else {
                    // V2 of Candidate Class
                    owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(conjunctiveHornClausesClassExpression);
                }
            }
        } else {
            logger.error("ERROR!!!!!! conjunctiveHornClauses is null, program exiting");
            System.exit(-1);
        }

        this.candidateClassAsOWLClassExpression = owlClassExpression;
        return this.candidateClassAsOWLClassExpression;
    }

    /**
     * get candidate class as String
     *
     * @return String
     */
    @Override
    public String getCandidateClassAsString(boolean includePrefix) {

//        if (!solutionChanged && null != candidateClassAsString)
//            return candidateClassAsString;

        solutionChanged = false;

        StringBuilder sb = new StringBuilder();

        if (null != this) {
            if (this.getConjunctiveHornClauses().size() > 0) {

                // V2 of candidate class, makes always AND
                String ANDOR = AND.toString();

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
                        // should we use OR or AND between multiple hornClauses of same object property ?
                        // This is especially important when we have only bare type, i.e. no R-Filled type.
                        // If changed here changes must reflect in accuracy measure too.
                        // TODO:  check with Pascal.
                        sb.append(" " + ANDOR + " ");
                        sb.append("(");
                        sb.append(this.getConjunctiveHornClauses().get(i).getHornClauseAsString(includePrefix));
                        sb.append(")");
                    }
                    sb.append(")");
                }
            }
        }

        this.candidateClassAsString = sb.toString();
        return candidateClassAsString;
    }

    /**
     * This will return all individuals covered by this complex concept from the ontology using reasoner,
     * large number of individuals may be returned.
     *
     * @return HashSet<OWLNamedIndividual>
     */
    public HashSet<OWLNamedIndividual> individualsCoveredByThisCandidateClassByReasoner() {

        logger.debug("Calculating covered individuals by candidateClass " + this.getCandidateClassAsOWLClassExpression() + " by reasoner.........");

        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        OWLClassExpression owlClassExpression = this.getCandidateClassAsOWLClassExpression();

        if (null != owlClassExpression) {
            if (!this.owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                // v2 of candidate class using getOWLObjectAllValuesFrom instead of getOWLObjectSomeValuesFrom
                owlClassExpression = owlDataFactory.getOWLObjectAllValuesFrom(owlObjectProperty, owlClassExpression);
            }


            // if we have calculated previously then just retrieve it from cache and return it.
            if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
                if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {
                    coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                    logger.debug("calculating covered individuals by candidateSolution " + this.getCandidateClassAsOWLClassExpression() + " found in cache.");
                    logger.debug("\t covered all individuals size: " + coveredIndividuals.size());
                    return coveredIndividuals;
                }
            }


            // not found in cache, now expensive reasoner calls through the conjunctiveHornClauses.
//        if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
            // all hornClause are conjuncted, need to make set intersection
            // V2 of candidate class
            coveredIndividuals = conjunctiveHornClauses.get(0).
                    individualsCoveredByThisHornClauseByReasoner();
            for (int i = 1; i < conjunctiveHornClauses.size(); i++) {
                coveredIndividuals.retainAll(conjunctiveHornClauses.get(i).
                        individualsCoveredByThisHornClauseByReasoner());
            }

            // save it to cache
            SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

        } else {
            logger.warn("ERROR!!!!!! owlClassExpression is null");
        }
        logger.debug("Calculating covered individuals by candidateClass " + this.getCandidateClassAsOWLClassExpression() + " by reasoner finished");
        logger.debug("\t Covered all individuals size: " + coveredIndividuals.size());

        return coveredIndividuals;
    }

    /**
     * Calculate accuracy of a candidateClass.
     *
     * @return
     */
    @Override
    public Score calculateAccuracyComplexCustom() {

        /**
         * Individuals covered by all parts of solution
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by all parts of solution
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        HashSet<OWLNamedIndividual> allCoveredIndividuals = this.
                individualsCoveredByThisCandidateClassByReasoner();
        // calculate how many positive individuals covered.
        int posCoveredCounter = 0;
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {
            if (allCoveredIndividuals.contains(thisOwlNamedIndividual)) {
                posCoveredCounter++;
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }
        nrOfPositiveClassifiedAsPositive = posCoveredCounter;
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;

        // calculate how many negative individuals covered.
        int negCoveredCounter = 0;
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {
            if (allCoveredIndividuals.contains(thisOwlNamedIndividual))
                negCoveredCounter++;
            else {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
            }
        }
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsNegative = SharedDataHolder.negIndivs.size() - negCoveredCounter;
        nrOfNegativeClassifiedAsPositive = negCoveredCounter;


        assert excludedNegIndividualsMap.size() == nrOfNegativeClassifiedAsNegative;
        assert coveredPosIndividualsMap.size() == nrOfPositiveClassifiedAsPositive;

        logger.debug("candidateClass: " + this.getCandidateClassAsString(true));
        logger.debug("\tcoveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.debug("\tcoveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.debug("\texcludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.debug("\texcludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());

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

        this.score = accScore;
        return this.score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateClassV2 that = (CandidateClassV2) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauses), new HashSet<>(that.conjunctiveHornClauses));
    }

    @Override
    public int hashCode() {
        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauses));
    }
}
