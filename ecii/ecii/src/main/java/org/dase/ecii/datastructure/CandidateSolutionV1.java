package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/


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
 * Inside the group:
 *  * 1. When we have none ObjectProperty or bare types, then candidate classes will be combined by AND/Intersection
 *  * 2. When we have proper ObjectProperty, then candidate classes will be combined with OR/Disjunction
 *
 *  Example Solution
 *  Candidate solution is of the form:
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
 *   k3 = limit of object properties considered. = ConfigParams.objPropsCombinationLimit
 *   k2 = limit of horn clauses. = ConfigParams.hornClauseLimit.
 *
 * An Example
 *  *   Solution = (A1 ⊓ ¬(D1)) ⊓ (A2 ⊓ ¬(D1))  ⊓  R1.( (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) ) ⊓ R2.(..)...
 *  *      here, we have 3 groups.
 *  *       group1: with bare objectProperty: (A1 ⊓ ¬(D1)) ⊓ (A2 ⊓ ¬(D1))
 *  *       group2: with R1 objectProperty:   R1.( (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) )
 *  *       group3: with R2 objectProperty:   R2.(..)
 *
 *  *   Inside of a group, we have multiple candidateClass
 *  *       multiple candidateClass are conjuncted when we have hare object property, and
 *                                      unioned when we have proper object property.
 *  *       Inside of CandidateClass:
 *  *           multiple horclauses are conjuncted when we have hare object property, and
 *                                      unioned when we have proper object property.
 *
 * * Implementation note:
 * * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
 * * it must be the same procedure in both
 *      1. Printing getAsString(
 *      2. calculating accuracy,
 *      3. making getAsOWLClassExpression(
 *  * We will have a single group for a single objectProperty.
 *
 * OLD-stuff:
 * CandidateSolution consists of atomic classes and candidate classes.
 * Atomic Class and candidate class will be concatenated by AND/Conjunction.
 * Atomic Class must be printed first.
 *  </pre>
 */
public class CandidateSolutionV1 {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    /**
     * ArrayList of Candidate Class.
     * Limit K3 = limit of object properties considered = ConfigParams.objPropsCombinationLimit.
     * <p>
     * Implementation note:
     * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
     */
    private ArrayList<CandidateClassV1> candidateClasses;

    /**
     * Candidate classes as group.
     */
    private HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> groupedCandidateClasses;

    /**
     * candidate solution
     */
    private OWLClassExpression candidateSolutionAsOWLClass = null;

    /**
     * candidate solution as String
     */
    private String candidateSolutionAsString = null;

    private boolean solutionChanged = true;

    // Score associated with this solution
    private Score score;

    // use double to ensure when dividing we are getting double result not integer.
    transient volatile protected double nrOfPositiveClassifiedAsPositive;
    /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
    transient volatile protected double nrOfPositiveClassifiedAsNegative;
    transient volatile protected double nrOfNegativeClassifiedAsNegative;
    /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
    transient volatile protected double nrOfNegativeClassifiedAsPositive;

    /**
     * Bad design should fix it
     */
    private final OWLOntology ontology;
    private final OWLDataFactory owlDataFactory;
    private final OWLOntologyManager owlOntologyManager;
    private OWLReasoner reasoner;

    /**
     * public constructor
     */
    public CandidateSolutionV1(OWLReasoner _reasoner, OWLOntology _ontology) {
        solutionChanged = true;
        this.candidateClasses = new ArrayList<>();

        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateSolution
     */
    public CandidateSolutionV1(CandidateSolutionV1 anotherCandidateSolution, OWLOntology _ontology) {

        this.candidateClasses = new ArrayList<>(anotherCandidateSolution.candidateClasses);

        if (null != anotherCandidateSolution.candidateSolutionAsOWLClass) {
            this.candidateSolutionAsOWLClass = anotherCandidateSolution.candidateSolutionAsOWLClass;
        }
        if (null != anotherCandidateSolution.candidateSolutionAsString) {
            this.candidateSolutionAsString = anotherCandidateSolution.candidateSolutionAsString;
        }
        if (null != anotherCandidateSolution.score) {
            this.score = anotherCandidateSolution.score;
        }
        solutionChanged = false;

        this.reasoner = anotherCandidateSolution.reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
    }

    /**
     * Getter
     *
     * @return ArrayList<CandidateClassV1>
     */
    public ArrayList<CandidateClassV1> getCandidateClasses() {
        return candidateClasses;
    }

    /**
     * setter
     *
     * @param ArrayList<CandidateClassV1> candidateClasses
     */
    public void setCandidateClasses(ArrayList<CandidateClassV1> candidateClasses) {
        solutionChanged = true;
        this.candidateClasses = candidateClasses;
    }

    /**
     * Adder
     *
     * @param candidateClass
     */
    public void addCandidateClass(CandidateClassV1 candidateClass) {
        solutionChanged = true;
        this.candidateClasses.add(candidateClass);
    }

    /**
     * Getter
     *
     * @return
     */
    public Score getScore() {
        return score;
    }

    /**
     * Score setter
     *
     * @param score
     */
    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * Getter. It dont't have any public setter. Now we are adding public setter/adder to support interactive ecii
     *
     * @return
     */
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> getGroupedCandidateClasses() {
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
     * @param candidateClassV1s
     * @return
     */
    public boolean addGroupedCandidateClass(OWLObjectProperty owlObjectProperty, ArrayList<CandidateClassV1> candidateClassV1s) {
        if (null == owlObjectProperty || null == candidateClassV1s)
            return false;

        boolean added = false;
        if (null == groupedCandidateClasses) {
            groupedCandidateClasses = new HashMap<>();
        }

        if (groupedCandidateClasses.containsKey(owlObjectProperty)) {
            groupedCandidateClasses.get(owlObjectProperty).addAll(candidateClassV1s);
        } else {
            groupedCandidateClasses.put(owlObjectProperty, candidateClassV1s);
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

        // bare portion
        OWLClassExpression directTypePortion = null;
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV1> candidateClasses = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);

            if (null != candidateClasses) {
                if (candidateClasses.size() > 0) {
                    HashSet<OWLClassExpression> directCandidateClassesAsOWLClassExpression = new HashSet<>();
                    for (CandidateClassV1 candidateClass : candidateClasses) {
                        directCandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                    }
                    // convert to list to get the single item, so that we don't need to make union
                    ArrayList<OWLClassExpression> directCandidateClassesAsOWLClassExpressionAList = new ArrayList<>(directCandidateClassesAsOWLClassExpression);
                    if (directCandidateClassesAsOWLClassExpressionAList.size() > 0) {
                        if (directCandidateClassesAsOWLClassExpressionAList.size() == 1) {
                            directTypePortion = directCandidateClassesAsOWLClassExpressionAList.get(0);
                        } else {
                            // make AND between candidate classes for direct Type.
                            // TODO(zaman): this is conflicting, need to verify, why we are using union for multiple bare types.
                            //  the thing is we should not have multiple candidate class for bare types. --- because for single objectproperty there will be a single candidate class, no no, no
                            //  becuase we may create multiple candidate class with same objectproperty. so if this happens for bare type we need to make it intersected.
                            logger.info("This code must not be executed!!!!, need to check CandidateSolutionV1.getSolutionAsOWLClassExpression() ");
                            directTypePortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(directCandidateClassesAsOWLClassExpression);
                        }
                    }
                }
            }
        }

        // rFilled portion
        OWLClassExpression rFilledPortion = null;
        HashSet<OWLClassExpression> rFilledPortionForAllGroups = new HashSet<>();
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV1> candidateClasses = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClasses) {
                    if (candidateClasses.size() > 0) {

                        OWLClassExpression rFilledPortionForThisGroup = null;
                        HashSet<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpression = new HashSet<>();

                        for (CandidateClassV1 candidateClass : candidateClasses) {
                            rFilledcandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                        }

                        // convert to list to get the single item, so that we don't need to make union
                        ArrayList<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpressionAList = new ArrayList<>(rFilledcandidateClassesAsOWLClassExpression);
                        if (rFilledcandidateClassesAsOWLClassExpressionAList.size() > 0) {
                            if (rFilledcandidateClassesAsOWLClassExpressionAList.size() == 1) {
                                rFilledPortionForThisGroup = rFilledcandidateClassesAsOWLClassExpressionAList.get(0);
                            } else {
                                // make OR between candidate classes
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
                // make AND between multiple object Property.
                // bug-fix : it should be owlObjectIntersectionOf instead of owlObjectUnionOf
                rFilledPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(rFilledPortionForAllGroups);
            }
        }

        // make AND between multiple object Property.
        //rFilledPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(rFilledPortionForAllGroups);

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
        return complexClassExpression;
    }

    /**
     * Get the solution as String
     *
     * @return String
     */
    public String getSolutionAsString() {

        if (!solutionChanged && null != candidateSolutionAsString)
            return candidateSolutionAsString;

        solutionChanged = false;

        if (null == groupedCandidateClasses) {
            createGroup();
        }

        StringBuilder sb = new StringBuilder();
        int bareTypeSize = 0;


        // print bare type at first
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV1> candidateClasses = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);
            if (null != candidateClasses) {
                if (candidateClasses.size() > 0) {
                    // we expect atomic class size will be one but it is not the case always.
                    bareTypeSize = candidateClasses.size();
                    if (candidateClasses.size() == 1) {
                        sb.append(candidateClasses.get(0).getCandidateClassAsString());
                    } else {
                        sb.append("(");
                        sb.append(candidateClasses.get(0).getCandidateClassAsString());

                        for (int i = 1; i < candidateClasses.size(); i++) {
                            // ecii extension-- making it AND instead of OR, same as the getASOWLClassExpression
                            sb.append(" " + AND.toString() + " ");
                            sb.append(candidateClasses.get(i).getCandidateClassAsString());
                        }
                        sb.append(")");
                    }
                }
            }
        }

        int rFilledSize = 0;
        // print r filled type then
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV1> candidateClasses = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClasses) {
                    if (candidateClasses.size() > 0) {
                        rFilledSize++;

                        if (bareTypeSize > 0 || rFilledSize > 1) {
                            sb.append(" " + AND.toString() + " ");
                        }
                        sb.append(EXISTS +" "+ Utility.getShortName(owlObjectProperty) + ".");
                        if (candidateClasses.size() == 1) {
                            sb.append(candidateClasses.get(0).getCandidateClassAsString());
                        } else {
                            sb.append("(");
                            sb.append(candidateClasses.get(0).getCandidateClassAsString());

                            for (int i = 1; i < candidateClasses.size(); i++) {
                                sb.append(" " + OR.toString() + " ");
                                sb.append(candidateClasses.get(i).getCandidateClassAsString());
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
     * This will return all individuals covered by this complex concept from the ontology using reasoner,
     * large number of individuals may be returned.
     *
     * @return
     */
    public HashSet<OWLNamedIndividual> individualsCoveredByThisCandidateSolutionByReasoner() {

        logger.info("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " by reasoner.........");

        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        // this solution is already r filled, so we dont need to r fill again.
        OWLClassExpression owlClassExpression = this.getSolutionAsOWLClassExpression();

        // if we have calculated previously then just retrieve it from cache and return it.
        if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
            if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {
                coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                logger.info("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " found in cache.");
                logger.info("\t covered all individuals size: " + coveredIndividuals.size());
                return coveredIndividuals;
            }
        }

        // not found in cache, now expensive reasoner calls through the candidateClass.
        int groupNo = 0;
        if (getGroupedCandidateClasses().size() > 0) {

            // here if we have multiple group the each group need to be concatenated.
            for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> owlObjectPropertyArrayListHashMap : getGroupedCandidateClasses().entrySet()) {

                HashSet<OWLNamedIndividual> coveredIndividualsInThisGroup = new HashSet<>();
                OWLObjectProperty key = owlObjectPropertyArrayListHashMap.getKey();
                if (key.equals(SharedDataHolder.noneOWLObjProp)) {
                    if (owlObjectPropertyArrayListHashMap.getValue().size() > 0) {
                        coveredIndividualsInThisGroup = owlObjectPropertyArrayListHashMap.getValue().get(0).individualsCoveredByThisCandidateClassByReasoner();
                        // each candidateclass are concatenated
                        for (int i = 1; i < owlObjectPropertyArrayListHashMap.getValue().size(); i++) {
                            // retainAll do set intersection
                            coveredIndividualsInThisGroup.retainAll(owlObjectPropertyArrayListHashMap.getValue().get(i).individualsCoveredByThisCandidateClassByReasoner());
                        }
                    }
                } else {
                    if (owlObjectPropertyArrayListHashMap.getValue().size() > 0) {
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getKey());
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getValue());
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getValue().get(0).getCandidateClassAsString());
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getValue().get(0).getConjunctiveHornClauses().size());
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getValue().get(0).getConjunctiveHornClauses().get(0).getPosObjectTypes().size());
//                        logger.info("debug: "+ owlObjectPropertyArrayListHashMap.getValue().get(0).getConjunctiveHornClauses().get(0).getPosObjectTypes().get(0));
                        // null pointer exception is here
                        coveredIndividualsInThisGroup = owlObjectPropertyArrayListHashMap.getValue().get(0).individualsCoveredByThisCandidateClassByReasoner();
                        // each candidateclass are unioned
                        for (int i = 1; i < owlObjectPropertyArrayListHashMap.getValue().size(); i++) {
                            // addAll for union
                            coveredIndividualsInThisGroup.addAll(owlObjectPropertyArrayListHashMap.getValue().get(i).individualsCoveredByThisCandidateClassByReasoner());
                        }
                    }
                }
                if (groupNo == 0) {
                    coveredIndividuals = coveredIndividualsInThisGroup;
                } else {
                    // make intersection between group 0 and all others
                    coveredIndividuals.retainAll(coveredIndividuals);
                }
                groupNo++;
            }
        }

        // save it to cache
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

        logger.info("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " by reasoner finished");
        logger.info("\t covered all individuals size:  " + coveredIndividuals.size());

        return coveredIndividuals;

    }


    /**
     * Calculate accuracy of a solution, according to the new method.
     * TODO(Zaman) : need to fix the accuracy calculation for v1
     *
     * @return
     */
    public Score calculateAccuracyComplexCustom() {

        /**
         * Individuals covered by this solution
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by this solution
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        HashSet<OWLNamedIndividual> allCoveredIndividuals = this.individualsCoveredByThisCandidateSolutionByReasoner();
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

//        logger.info("Calculating accuracy of candidateSolution: "+ candidateSolutionV1.getSolutionAsString());
//        logger.info("Excluded negative Indivs:");

        // TODO(zaman): it should be logger.debug instead of logger.info
        logger.info("solution: " + this.getSolutionAsString());
        logger.info("\tcoveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.info("\tcoveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.info("\texcludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.info("\texcludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());


//        assert 2 == 1;
        assert excludedNegIndividualsMap.size() == nrOfNegativeClassifiedAsNegative;
        assert coveredPosIndividualsMap.size() == nrOfPositiveClassifiedAsPositive;

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
        ChangeApplied ca = SharedDataHolder.owlOntologyManager.addAxiom(SharedDataHolder.owlOntology, eqAxiom);
        logger.info("Adding candidateSolution.getSolutionAsOWLClassExpression to ontology Status: " + ca.toString());
        reasoner = Utility.initReasoner(ConfigParams.reasonerName, SharedDataHolder.owlOntology, null);

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

            Set<OWLNamedIndividual> posIndivsByReasoner = reasoner.getInstances(owlClassExpression, false).getFlattened();

            if (posIndivsByReasoner.contains(thisOwlNamedIndividual)) {
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
//                logger.info("Good");
            } else {
//                logger.info("not found. size: " + posIndivsByReasoner.size());
            }
        }

        /**
         * For negative individuals, a individual must be contained within each AND section to be added as a excludedInvdividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            Set<OWLNamedIndividual> negIndivsByReasoner = reasoner.getInstances(owlClassExpression, false).getFlattened();

            if (negIndivsByReasoner.contains(thisOwlNamedIndividual)) {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
                logger.info("\t" + Utility.getShortName(thisOwlNamedIndividual) + " is contained by this concept using reasoner: " + owlClassExpression);
            } else {
//                logger.info("not found. size: " + negIndivsByReasoner.size());
            }
        }

        logger.info("solution: " + this.getSolutionAsString());
        logger.info("coveredPosIndividuals_by_reasoner: " + coveredPosIndividualsMap.keySet());
        logger.info("coveredPosIndividuals_by_reasoner size: " + coveredPosIndividualsMap.size());
        logger.info("coveredNegIndividuals_by_reasoner: " + excludedNegIndividualsMap.keySet());
        logger.info("coveredNegIndividuals_by_reasoner size: " + excludedNegIndividualsMap.size());

        nrOfPositiveClassifiedAsPositive = coveredPosIndividualsMap.size();
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;
        // TODO(zaman): need to verify this one, most probably the excludedNegIndividuals are the covered ones' by this concept, so we need to make inverse of it. for now use the exact one it, but later we have to fix it or verify it.
        nrOfNegativeClassifiedAsNegative = SharedDataHolder.negIndivs.size() - excludedNegIndividualsMap.size();
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsPositive = excludedNegIndividualsMap.size();

        logger.info("nrOfPositiveClassifiedAsPositive size by reasoner: " + nrOfPositiveClassifiedAsPositive);
        logger.info("nrOfNegativeClassifiedAsNegative size by reasoner: " + nrOfNegativeClassifiedAsNegative);

        double precision = Heuristics.getPrecision(nrOfPositiveClassifiedAsPositive, nrOfNegativeClassifiedAsPositive);
        double recall = Heuristics.getRecall(nrOfPositiveClassifiedAsPositive, nrOfPositiveClassifiedAsNegative);
        double f_measure = Heuristics.getFScore(recall, precision);
        double coverage = Heuristics.getCoverage(nrOfPositiveClassifiedAsPositive, SharedDataHolder.posIndivs.size(),
                nrOfNegativeClassifiedAsNegative, SharedDataHolder.negIndivs.size());

        logger.info("precision size by reasoner: " + precision);
        logger.info("recall size by reasoner: " + recall);

        //Score accScore = new Score();
        //candidateSolution.getScore()
        this.getScore().setPrecision_by_reasoner(precision);
        this.getScore().setRecall_by_reasoner(recall);
        this.getScore().setF_measure_by_reasoner(f_measure);
        this.getScore().setCoverage_by_reasoner(coverage);


    }


    /**
     * Create group
     */
    private void createGroup() {
        groupedCandidateClasses = new HashMap<>();

        candidateClasses.forEach(candidateClass -> {
            if (groupedCandidateClasses.containsKey(candidateClass.getOwlObjectProperty())) {
                groupedCandidateClasses.get(candidateClass.getOwlObjectProperty()).add(candidateClass);
            } else {
                ArrayList<CandidateClassV1> candidateClassArrayList = new ArrayList<>();
                candidateClassArrayList.add(candidateClass);
                groupedCandidateClasses.put(candidateClass.getOwlObjectProperty(), candidateClassArrayList);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateSolutionV1 that = (CandidateSolutionV1) o;
        return Objects.equals(new HashSet<>(candidateClasses), new HashSet<>(that.candidateClasses));
    }

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(candidateClasses));
    }
}
