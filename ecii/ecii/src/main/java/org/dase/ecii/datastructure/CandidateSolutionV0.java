package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/


import org.dase.ecii.core.HashMapUtility;
import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Heuristics;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.*;

/**
 * <pre>
 * Candidate solution consists of multiple candidate classes. Multiple candidate class are grouped by owlObjectproperty.
 * Each groups are combined by AND/Intersection.
 *
 * Inside the group candidate classes will be combined by OR/Disjunction. No matter what the objectProperty is.
 *
 *  Example:
 *   Candidate solution is of the form:
 *
 *   l
 * A ⊓ 􏰃∃Ri.Ci,
 *   i=1
 *
 *    which can  also be written as:
 *
 *   k3       k2
 * A ⊓ 􏰃∃Ri. 􏰀(⊔(Bji ⊓¬(D1 ⊔...⊔Dji)))
 *   i=1    j=1
 *
 *   here,
 *
 *   k3 = limit of object properties considered. = ConfigParams.objPropsCombinationLimit
 *   k2 = limit of horn clauses. = ConfigParams.hornClauseLimit.
 *
 * Implementation note:
 * Need to follow exactly same procedure in all 3
 *      1. Printing getAsString
 *      2. Calculating accuracy
 *      3. Making getAsOWLClassExpression
 *   methods to preserve accuracy.
 *
 * Inside the group, we always do OR/Disjunction.
 * Code:
 *      directTypePortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(directCandidateClassesAsOWLClassExpression);
 *
 * Bare types group must be printed first.
 * </pre>
 */
public class CandidateSolutionV0 extends CandidateSolution {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * ArrayList of Candidate Class.
     * Limit K3 = limit of object properties considered = ConfigParams.objPropsCombinationLimit.
     * <p>
     * Implementation note:
     * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
     */
    public ArrayList<CandidateClassV0> candidateClasses;

    /**
     * Candidate classes as group.
     */
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV0>> groupedCandidateClasses;

    public CandidateSolutionV0(OWLReasoner _reasoner, OWLOntology _ontology) {
        super(_reasoner, _ontology);
        this.candidateClasses = new ArrayList<>();
    }

    public CandidateSolutionV0(CandidateSolutionV0 anotherCandidateSolution, OWLOntology _ontology) {
        super(anotherCandidateSolution, _ontology);
        if (null != anotherCandidateSolution && null != anotherCandidateSolution.candidateClasses)
            this.candidateClasses = new ArrayList<>(anotherCandidateSolution.candidateClasses);
    }

    /**
     * Getter
     *
     * @return ArrayList<CandidateClass>
     */
    public ArrayList<CandidateClassV0> getCandidateClasses() {
        return candidateClasses;
    }

    /**
     * setter
     *
     * @param candidateClasses: ArrayList<CandidateClass>
     */
    public void setCandidateClasses(ArrayList<CandidateClassV0> candidateClasses) {
        solutionChanged = true;
        this.candidateClasses = candidateClasses;
    }

    /**
     * Adder
     *
     * @param candidateClass
     */
    public void addCandidateClass(CandidateClassV0 candidateClass) {
        solutionChanged = true;
        this.candidateClasses.add(candidateClass);
    }

    /**
     * Create group
     */
    public void createGroup() {
        groupedCandidateClasses = new HashMap<>();

        candidateClasses.forEach(candidateClass -> {
            if (groupedCandidateClasses.containsKey(candidateClass.getOwlObjectProperty())) {
                groupedCandidateClasses.get(candidateClass.getOwlObjectProperty()).add(candidateClass);
            } else {
                ArrayList<CandidateClassV0> candidateClassArrayList = new ArrayList<>();
                candidateClassArrayList.add(candidateClass);
                groupedCandidateClasses.put(candidateClass.getOwlObjectProperty(), candidateClassArrayList);
            }
        });
    }

    /**
     * Getter. It dont't have any public setter. Now we are adding public setter/adder to support interactive ecii
     *
     * @return
     */
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV0>> getGroupedCandidateClasses() {
        if (null == groupedCandidateClasses) {
            createGroup();
        }
        solutionChanged = false;
        return groupedCandidateClasses;
    }

    /**
     * Adder to support interactive ecii
     *
     * @param owlObjectProperty
     * @param candidateClassV0s
     * @return
     */
    public boolean addGroupedCandidateClass(OWLObjectProperty owlObjectProperty, ArrayList<CandidateClassV0> candidateClassV0s) {
        if (null == owlObjectProperty || null == candidateClassV0s)
            return false;

        boolean added = false;
        if (null == groupedCandidateClasses) {
            groupedCandidateClasses = new HashMap<>();
        }

        if (groupedCandidateClasses.containsKey(owlObjectProperty)) {
            groupedCandidateClasses.get(owlObjectProperty).addAll(candidateClassV0s);
        } else {
            groupedCandidateClasses.put(owlObjectProperty, candidateClassV0s);
        }
        solutionChanged = true;
        added = true;
        return added;
    }

    /**
     * @return OWLClassExpression
     */
    public OWLClassExpression getSolutionAsOWLClassExpression() {

        if (!solutionChanged && null != candidateSolutionAsOWLClass)
            return candidateSolutionAsOWLClass;

        solutionChanged = false;
        if (null == groupedCandidateClasses) {
            createGroup();
        }

        OWLClassExpression directTypePortion = null;
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV0> candidateClassV0s = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);

            if (null != candidateClassV0s) {
                if (candidateClassV0s.size() > 0) {
                    HashSet<OWLClassExpression> directCandidateClassesAsOWLClassExpression = new HashSet<>();
                    for (CandidateClassV0 candidateClassV0 : candidateClassV0s) {
                        directCandidateClassesAsOWLClassExpression.add(candidateClassV0.getCandidateClassAsOWLClassExpression());
                    }
                    // convert to list to get the single item, so that we don't need to make union
                    ArrayList<OWLClassExpression> directCandidateClassesAsOWLClassExpressionAList = new ArrayList<>(directCandidateClassesAsOWLClassExpression);
                    if (directCandidateClassesAsOWLClassExpressionAList.size() > 0) {
                        if (directCandidateClassesAsOWLClassExpressionAList.size() == 1) {
                            directTypePortion = directCandidateClassesAsOWLClassExpressionAList.get(0);
                        } else {
                            // make OR between candidate classes for direct Type.
                            directTypePortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(directCandidateClassesAsOWLClassExpression);
                        }
                    }
                }
            }
        }

        OWLClassExpression rFilledPortion = null;
        HashSet<OWLClassExpression> rFilledPortionForAllGroups = new HashSet<>();
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV0> candidateClassV0s = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClassV0s) {
                    if (candidateClassV0s.size() > 0) {

                        OWLClassExpression rFilledPortionForThisGroup = null;
                        HashSet<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpression = new HashSet<>();

                        for (CandidateClassV0 candidateClassV0 : candidateClassV0s) {
                            rFilledcandidateClassesAsOWLClassExpression.add(candidateClassV0.getCandidateClassAsOWLClassExpression());
                        }

                        // convert to list to get the single item, so that we don't need to make union
                        ArrayList<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpressionAList = new ArrayList<>(rFilledcandidateClassesAsOWLClassExpression);
                        if (rFilledcandidateClassesAsOWLClassExpressionAList.size() > 0) {
                            if (rFilledcandidateClassesAsOWLClassExpressionAList.size() == 1) {
                                rFilledPortionForThisGroup = rFilledcandidateClassesAsOWLClassExpressionAList.get(0);
                            } else {
                                // make OR between candidate classes of proper objectProperty.
                                rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(rFilledcandidateClassesAsOWLClassExpression);
                            }
                        }

                        // use rFill
                        if (null != rFilledPortionForThisGroup) {
                            rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, rFilledPortionForThisGroup);
                            if (null != rFilledPortionForThisGroup) {
                                rFilledPortionForAllGroups.add(rFilledPortionForThisGroup);
                            }
                        }
                    }
                }
            }
        }

        // convert to list to get the single item, so that we don't need to make union
        ArrayList<OWLClassExpression> rFilledPortionForAllGroupsAList = new ArrayList<>(rFilledPortionForAllGroups);
        if (rFilledPortionForAllGroupsAList.size() > 0) {
            if (rFilledPortionForAllGroupsAList.size() == 1) {
                rFilledPortion = rFilledPortionForAllGroupsAList.get(0);
            } else {
                rFilledPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(rFilledPortionForAllGroups);
            }
        }


        OWLClassExpression complexClassExpression = null;

        // make AND between direct type and rFilled type.
        if (null != directTypePortion && null != rFilledPortion) {
            complexClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(directTypePortion, rFilledPortion);
        } else if (null != directTypePortion) {
            complexClassExpression = directTypePortion;
        } else if (null != rFilledPortion) {
            complexClassExpression = rFilledPortion;
        }

        this.candidateSolutionAsOWLClass = complexClassExpression;
        return this.candidateSolutionAsOWLClass;
    }

    /**
     * Get the solution as String
     *
     * @return
     */
    public String getSolutionAsString(boolean includePrefix) {
        if (null == groupedCandidateClasses) {
            createGroup();
        }

        StringBuilder sb = new StringBuilder();
        int bareTypeSize = 0;


        // print bare type at first
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV0> candidateClassV0s = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);
            if (null != candidateClassV0s) {
                if (candidateClassV0s.size() > 0) {
                    // we expect atomic class size will be one but it is not the case always.
                    bareTypeSize = candidateClassV0s.size();
                    if (candidateClassV0s.size() == 1) {
                        sb.append(candidateClassV0s.get(0).getCandidateClassAsString(includePrefix));
                    } else {
                        sb.append("(");
                        sb.append(candidateClassV0s.get(0).getCandidateClassAsString(includePrefix));

                        for (int i = 1; i < candidateClassV0s.size(); i++) {
                            sb.append(" " + OR.toString() + " ");
                            sb.append(candidateClassV0s.get(i).getCandidateClassAsString(includePrefix));
                        }
                        sb.append(")");
                    }
                }
            }
        }

        int rFilledSize = 0;
        // print r filled type then
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV0> candidateClassV0s = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClassV0s) {
                    if (candidateClassV0s.size() > 0) {
                        rFilledSize++;

                        if (bareTypeSize > 0 || rFilledSize > 1) {
                            sb.append(" " + AND.toString() + " ");
                        }
                        if (includePrefix)
                            sb.append(EXISTS + " " + Utility.getShortNameWithPrefix(owlObjectProperty) + ".");
                        else sb.append(EXISTS + " " + Utility.getShortName(owlObjectProperty) + ".");
                        if (candidateClassV0s.size() == 1) {
                            sb.append(candidateClassV0s.get(0).
                                    getCandidateClassAsString(includePrefix));
                        } else {
                            sb.append("(");
                            sb.append(candidateClassV0s.get(0).
                                    getCandidateClassAsString(includePrefix));

                            for (int i = 1; i < candidateClassV0s.size(); i++) {
                                sb.append(" " + OR.toString() + " ");
                                sb.append(candidateClassV0s.get(i).
                                        getCandidateClassAsString(includePrefix));
                            }
                            sb.append(")");
                        }
                    }
                }
            }
        }

        this.candidateSolutionAsString = sb.toString();
        return this.candidateSolutionAsString;
    }

    /**
     * Determine whether this owlnamedIndividual contained within any of the candidate class of this candidateClassV0s.
     * precondition:
     * 1. all object property of this candidateclasses must be same.
     * 2. only works for candidateClassV0s
     *
     * @param candidateClassV0s
     * @param owlNamedIndividual
     * @return boolean
     */
    public boolean isContainedInCandidateClasses(ArrayList<CandidateClassV0> candidateClassV0s, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {
        boolean contained = false;

        OWLObjectProperty owlObjectProperty = candidateClassV0s.get(0).getOwlObjectProperty();

        HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                individualHasObjectTypes.get(owlNamedIndividual);


        if (null != objPropsMap && objPropsMap.containsKey(owlObjectProperty)) {
            // if any candidate class of this group contains this individual
            // then the full group contains this individual.
            for (CandidateClassV0 candidateClassV0 : candidateClassV0s) {
                if (owlObjectProperty.equals(candidateClassV0.getOwlObjectProperty())) {
                    if (candidateClassV0.getConjunctiveHornClauses().size() > 0) {
                        if (candidateClassV0.isContainedInCandidateClass(
                                owlNamedIndividual, isPosIndiv)) {
                            contained = true;
                            return contained;
                        }
                    }
                }
            }
        }

        return contained;
    }

    /**
     * Calculate accuracy of a solution.
     * This does not call the reasoner, do all the calculation by using set theory
     *
     * @return
     */
    public Score calculateAccuracyComplexCustom() {

        HashMap<OWLObjectProperty, ArrayList<CandidateClassV0>> groupedCandidateClasses = this.getGroupedCandidateClasses();

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
        nextPosIndivIter:
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {

            // it must be contained in each group of the candidate classes.
            int containedInTotalGroups = 0;

            for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {
                // each group will be concatenated by AND.
                OWLObjectProperty owlObjectProperty = entry.getKey();
                ArrayList<CandidateClassV0> candidateClasses = entry.getValue();
                if (candidateClasses.size() > 0) {
                    if (!isContainedInCandidateClasses(candidateClasses, thisOwlNamedIndividual, true)) {
                        // this individual is not contained in this arraylist of candidate classes.
                        // so this individual is not covered.
                        // we need to start iterating with next individual
                        continue nextPosIndivIter;
                    } else {
                        containedInTotalGroups++;
                    }
                }
            }
            if (containedInTotalGroups == groupedCandidateClasses.size()) {
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within each AND section to be added as a excludedInvdividuals.
         * I.e. each
         */
        nextNegIndivIter:
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            int containedInTotalGroups = 0;

            for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {
                // each group will be concatenated by AND.
                OWLObjectProperty owlObjectProperty = entry.getKey();
                // not passing object property here, because we can recover object property from candidate class
                ArrayList<CandidateClassV0> candidateClasses = entry.getValue();
                if (candidateClasses.size() > 0) {
                    if (!isContainedInCandidateClasses(candidateClasses, thisOwlNamedIndividual, false)) {
                        // this individual is not contained in this arraylist of candidate classes.
                        // so this individual is not covered.
                        // we need to start iterating with next individual
                        continue nextNegIndivIter;
                    } else {
                        containedInTotalGroups++;
                    }
                }
            }
            if (containedInTotalGroups == groupedCandidateClasses.size()) {
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

    /**
     * Calculate accuracy of a solution.
     *
     * @return
     */
    public void calculateAccuracyByReasoner() {

        // add this class expression to ontology and reinitiate reasoner.
        OWLClassExpression owlClassExpression = this.getSolutionAsOWLClassExpression();
        // create a unique name
        OWLClass owlClass = SharedDataHolder.owlDataFactory.getOWLClass(Utility.getUniqueIRI());
        OWLAxiom eqAxiom = SharedDataHolder.owlDataFactory.getOWLEquivalentClassesAxiom(owlClass, owlClassExpression);
        ChangeApplied ca = SharedDataHolder.owlOntologyManager.addAxiom(SharedDataHolder.owlOntologyOriginal, eqAxiom);
        logger.debug("Adding candidateSolutionV0.getSolutionAsOWLClassExpression to ontology Status: " + ca.toString());
        reasoner = Utility.initReasoner(ConfigParams.reasonerName, SharedDataHolder.owlOntologyOriginal, null);

        /**
         * Individuals covered by all parts of solution
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by all parts of solution
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        Set<OWLNamedIndividual> posIndivsByReasoner = reasoner.getInstances(owlClassExpression, false).getFlattened();
        /**
         * For positive individuals, a individual must be contained within each AND section to be added as a coveredIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {
            if (posIndivsByReasoner.contains(thisOwlNamedIndividual)) {
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
                logger.debug("Pos indiv " + Utility.getShortNameWithPrefix(thisOwlNamedIndividual)
                        + " covered by owlClass: " + this.getSolutionAsString(true)
                        + " by owlreasoner. \n\tsize of covered indivs by this class: " + posIndivsByReasoner.size());
            } else {
                logger.debug("Pos indiv " + Utility.getShortNameWithPrefix(thisOwlNamedIndividual)
                        + " --not-- covered by owlClass: " + this.getSolutionAsString(true) +
                        " by owlreasoner. \n\tsize of covered indivs by this class: " + posIndivsByReasoner.size());
            }
        }

        Set<OWLNamedIndividual> negIndivsByReasoner = reasoner.getInstances(owlClassExpression, false).getFlattened();
        /**
         * For negative individuals, a individual must be contained within each AND section to be added as a excludedInvdividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {
            if (negIndivsByReasoner.contains(thisOwlNamedIndividual)) {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
                logger.debug("Neg indiv " + Utility.getShortNameWithPrefix(thisOwlNamedIndividual)
                        + " covered by owlClass: " + this.getSolutionAsString(true)
                        + " by owlreasoner. \n\tsize of covered indivs by this class: " + negIndivsByReasoner.size());
            } else {
                logger.debug("Neg indiv " + Utility.getShortNameWithPrefix(thisOwlNamedIndividual)
                        + " --not-- covered by owlClass: " + this.getSolutionAsString(true) +
                        " by owlreasoner. \n\tsize of covered indivs by this class: " + negIndivsByReasoner.size());
            }
        }

        logger.debug("solution: " + this.getSolutionAsString(true));
        logger.debug("coveredPosIndividuals_by_reasoner: " + coveredPosIndividualsMap.keySet());
        logger.debug("coveredPosIndividuals_by_reasoner size: " + coveredPosIndividualsMap.size());
        logger.debug("coveredNegIndividuals_by_reasoner: " + excludedNegIndividualsMap.keySet());
        logger.debug("coveredNegIndividuals_by_reasoner size: " + excludedNegIndividualsMap.size());

        nrOfPositiveClassifiedAsPositive = coveredPosIndividualsMap.size();
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;
        // TODO(zaman): need to verify this one, most probably the excludedNegIndividuals are the covered ones' by this concept, so we need to make inverse of it. for now use the exact one it, but later we have to fix it or verify it.
        nrOfNegativeClassifiedAsNegative = SharedDataHolder.negIndivs.size() - excludedNegIndividualsMap.size();
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsPositive = nrOfNegativeClassifiedAsNegative;

        logger.debug("nrOfPositiveClassifiedAsPositive size by reasoner: " + nrOfPositiveClassifiedAsPositive);
        logger.debug("nrOfNegativeClassifiedAsNegative size by reasoner: " + nrOfNegativeClassifiedAsNegative);

        double precision = Heuristics.getPrecision(nrOfPositiveClassifiedAsPositive, nrOfNegativeClassifiedAsPositive);
        double recall = Heuristics.getRecall(nrOfPositiveClassifiedAsPositive, nrOfPositiveClassifiedAsNegative);
        double f_measure = Heuristics.getFScore(recall, precision);
        double coverage = Heuristics.getCoverage(nrOfPositiveClassifiedAsPositive, SharedDataHolder.posIndivs.size(),
                nrOfNegativeClassifiedAsNegative, SharedDataHolder.negIndivs.size());

        logger.debug("precision size by reasoner: " + precision);
        logger.debug("recall size by reasoner: " + recall);

        //Score accScore = new Score();
        //candidateSolutionV0.getScore()
        this.getScore().setPrecision_by_reasoner(precision);
        this.getScore().setRecall_by_reasoner(recall);
        this.getScore().setF_measure_by_reasoner(f_measure);
        this.getScore().setCoverage_by_reasoner(coverage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateSolutionV0 that = (CandidateSolutionV0) o;
        return Objects.equals(new HashSet<>(candidateClasses), new HashSet<>(that.candidateClasses));
    }

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(candidateClasses));
    }
}
