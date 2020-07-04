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
 * Inside the group (Significantly different than V0):
 *   Candidate classes will be:
 *      * 1. Combined by AND/Intersection when we have none objectProperty or bare types
 *      * 2. Combined with OR/Disjunction when we have proper objectProperty
 *
 * In CandidateSolutionV0:
 *      Always OR/Disjunction
 * In CandidateSolutionV1:
 *      Conjunction (none objProp) or Disjunction (proper objProp)
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
 *  *       Inside of CandidateClassV1:
 *  *           multiple horclauses are conjuncted when we have hare object property, and
 *                                      unioned when we have proper object property.
 *  *       Inside of CandidateClassV2:
 *  *           multiple horclauses are conjuncted always.
 *
 * Implementation note:
 * Need to follow exactly same procedure in all 3 methods to preserve accuracy
 * * For empty objectProperty,
 *      we are doing AND/Intersection
 *  * For proper objectProperty
 *      we are doing OR/Disjunction
 *      1. Printing getAsString
 *              for (int i = 1; i < candidateClasses.size(); i++) {
 *                                 sb.append(" " + OR.toString() + " ");
 *                                 sb.append(candidateClasses.get(i).getCandidateClassAsString(includePrefix));
 *                             }
 *      2. calculating accuracy
 *              union between candidateClasses of a group
 *      3. making getAsOWLClassExpression
 *          rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(
 *                                      rFilledcandidateClassesAsOWLClassExpression);
 *
 *
 * Bare types group must be printed first.
 *  </pre>
 */
public class CandidateSolutionV1 extends CandidateSolution {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * ArrayList of Candidate Class.
     * Limit K3 = limit of object properties considered = ConfigParams.objPropsCombinationLimit.
     * <p>
     * Implementation note:
     * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
     */
    public ArrayList<CandidateClassV1> candidateClasses;

    /**
     * Candidate classes as group.
     */
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> groupedCandidateClasses;

    /**
     * public constructor
     */
    public CandidateSolutionV1(OWLReasoner _reasoner, OWLOntology _ontology) {
        super(_reasoner, _ontology);
        this.candidateClasses = new ArrayList<>();
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateSolution
     */
    public CandidateSolutionV1(CandidateSolutionV1 anotherCandidateSolution, OWLOntology _ontology) {
        super(anotherCandidateSolution, _ontology);
        if (null != anotherCandidateSolution && null != anotherCandidateSolution.candidateClasses)
            this.candidateClasses = new ArrayList<>(anotherCandidateSolution.candidateClasses);
    }

    /**
     * Getter
     *
     * @return ArrayList<CandidateClass>
     */
    public ArrayList<CandidateClassV1> getCandidateClasses() {
        return candidateClasses;
    }

    /**
     * setter
     *
     * @param candidateClasses: ArrayList<CandidateClass>
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
     * Create group
     */
    public void createGroup() {
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
    @Override
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
                    for (CandidateClass candidateClass : candidateClasses) {
                        directCandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                    }
                    // convert to list to get the single item, so that we don't need to make union
                    ArrayList<OWLClassExpression> directCandidateClassesAsOWLClassExpressionAList = new ArrayList<>(directCandidateClassesAsOWLClassExpression);
                    if (directCandidateClassesAsOWLClassExpressionAList.size() > 0) {
                        if (directCandidateClassesAsOWLClassExpressionAList.size() == 1) {
                            directTypePortion = directCandidateClassesAsOWLClassExpressionAList.get(0);
                        } else {
                            // make AND between candidate classes for direct Type.
                            // we may create multiple candidate class with same objectproperty. Although that is not desired, but that's it!!
                            // ecii creates this kind of solution also. We can use filter or simplify those classexpression.
                            // example class expression:
                            // (Human and Mammal) and (Human and Plant)
                            // it can be combined into (Human and Mammal and Plant) easily
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

                        for (CandidateClass candidateClass : candidateClasses) {
                            rFilledcandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                        }

                        // convert to list to get the single item, so that we don't need to make union
                        ArrayList<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpressionAList = new ArrayList<>(rFilledcandidateClassesAsOWLClassExpression);
                        if (rFilledcandidateClassesAsOWLClassExpressionAList.size() > 0) {
                            if (rFilledcandidateClassesAsOWLClassExpressionAList.size() == 1) {
                                rFilledPortionForThisGroup = rFilledcandidateClassesAsOWLClassExpressionAList.get(0);
                            } else {
                                // make OR between candidate classes  of proper objectProperty.
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
     * solutionStringTypIsSame: with prefix or without prefix
     * Will measure whether the subsequent call to this getSolutionAsString() method are calling with includePrefix or not.
     * This means it can cache the only 1 version of the solution string, can not cache both without prefix and with prefix.
     */
//    public boolean cachedSolutionStringTypIsSame = true;

    /**
     * Get the solution as String
     *
     * @return String
     */
    @Override
    public String getSolutionAsString(boolean includePrefix) {

//        cachedSolutionStringTypIsSame = cachedSolutionStringTypIsSame && includePrefix ? false : true;
//
//        if (!solutionChanged && null != candidateSolutionAsString && !cachedSolutionStringTypIsSame)
//            return candidateSolutionAsString;

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
                        sb.append(candidateClasses.get(0).getCandidateClassAsString(includePrefix));
                    } else {
                        sb.append("(");
                        sb.append(candidateClasses.get(0).getCandidateClassAsString(includePrefix));

                        for (int i = 1; i < candidateClasses.size(); i++) {
                            // ecii extension-- making it AND instead of OR, same as the getASOWLClassExpression
                            sb.append(" " + AND.toString() + " ");
                            sb.append(candidateClasses.get(i).getCandidateClassAsString(includePrefix));
                        }
                        sb.append(")");
                    }
                }
            }
        }

        int rFilledSize = 0;
        // print r filled type then
        // problematic or view problem: ∃ :imageContains.((:Unsolved_problems_in_physics ⊓ :Materials) ⊓ (:Unsolved_problems_in_physics ⊓ :Phases_of_matter))
        // can we make it: ∃ :imageContains.((:Unsolved_problems_in_physics ⊓ :Materials ⊓ :Phases_of_matter) )
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
                        if (includePrefix)
                            sb.append(EXISTS + " " + Utility.getShortNameWithPrefix(owlObjectProperty) + ".");
                        else sb.append(EXISTS + " " + Utility.getShortName(owlObjectProperty) + ".");
                        if (candidateClasses.size() == 1) {
                            sb.append(candidateClasses.get(0).getCandidateClassAsString(includePrefix));
                        } else {
                            sb.append("(");
                            sb.append(candidateClasses.get(0).getCandidateClassAsString(includePrefix));

                            for (int i = 1; i < candidateClasses.size(); i++) {
                                sb.append(" " + OR.toString() + " ");
                                sb.append(candidateClasses.get(i).getCandidateClassAsString(includePrefix));
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

        logger.debug("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " by reasoner.........");

        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        // this solution is already r filled, so we dont need to r fill again.
        OWLClassExpression owlClassExpression = this.getSolutionAsOWLClassExpression();

        // if we have calculated previously then just retrieve it from cache and return it.
        if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
            if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {
                coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                logger.debug("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " found in cache.");
                logger.debug("\t covered all individuals size: " + coveredIndividuals.size());
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
                        CandidateClass candidateClass = owlObjectPropertyArrayListHashMap.getValue().get(0);
                        if (candidateClass instanceof CandidateClassV2)
                            coveredIndividualsInThisGroup = ((CandidateClassV2) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                        else if (candidateClass instanceof CandidateClassV1)
                            coveredIndividualsInThisGroup = ((CandidateClassV1) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
//                        coveredIndividualsInThisGroup = owlObjectPropertyArrayListHashMap.getValue().get(0).individualsCoveredByThisCandidateClassByReasoner();
                        // each candidateclass are concatenated
                        for (int i = 1; i < owlObjectPropertyArrayListHashMap.getValue().size(); i++) {
                            // retainAll do set intersection
                            HashSet<OWLNamedIndividual> owlNamedIndividualHashSet = new HashSet<>();
                            candidateClass = owlObjectPropertyArrayListHashMap.getValue().get(i);
                            if (candidateClass instanceof CandidateClassV2)
                                owlNamedIndividualHashSet = ((CandidateClassV2) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                            else if (candidateClass instanceof CandidateClassV1)
                                owlNamedIndividualHashSet = ((CandidateClassV1) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                            coveredIndividualsInThisGroup.retainAll(owlNamedIndividualHashSet);
//                            coveredIndividualsInThisGroup.retainAll(owlObjectPropertyArrayListHashMap.getValue().get(i).individualsCoveredByThisCandidateClassByReasoner());
                        }
                    }
                } else {
                    if (owlObjectPropertyArrayListHashMap.getValue().size() > 0) {
                        logger.debug("debug: " + owlObjectPropertyArrayListHashMap.getKey());
                        logger.debug("debug: " + owlObjectPropertyArrayListHashMap.getValue());
                        logger.debug("debug: " + owlObjectPropertyArrayListHashMap.getValue().get(0).getCandidateClassAsString(true));
                        CandidateClass candidateClass = owlObjectPropertyArrayListHashMap.getValue().get(0);
                        if (candidateClass instanceof CandidateClassV2)
                            coveredIndividualsInThisGroup = ((CandidateClassV2) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                        else if (candidateClass instanceof CandidateClassV1)
                            coveredIndividualsInThisGroup = ((CandidateClassV1) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
//                        coveredIndividualsInThisGroup = owlObjectPropertyArrayListHashMap.getValue().get(0).individualsCoveredByThisCandidateClassByReasoner();
                        // each candidateclass are unioned
                        for (int i = 1; i < owlObjectPropertyArrayListHashMap.getValue().size(); i++) {
                            // addAll for union
                            HashSet<OWLNamedIndividual> owlNamedIndividualHashSet = new HashSet<>();
                            candidateClass = owlObjectPropertyArrayListHashMap.getValue().get(i);
                            if (candidateClass instanceof CandidateClassV2)
                                owlNamedIndividualHashSet = ((CandidateClassV2) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                            else if (candidateClass instanceof CandidateClassV1)
                                owlNamedIndividualHashSet = ((CandidateClassV1) candidateClass).individualsCoveredByThisCandidateClassByReasoner();
                            coveredIndividualsInThisGroup.addAll(owlNamedIndividualHashSet);
//                            coveredIndividualsInThisGroup.addAll(owlObjectPropertyArrayListHashMap.getValue().get(i).individualsCoveredByThisCandidateClassByReasoner());
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

        logger.debug("calculating covered individuals by candidateSolution " + this.getSolutionAsOWLClassExpression() + " by reasoner finished");
        logger.debug("\t covered all individuals size:  " + coveredIndividuals.size());

        return coveredIndividuals;

    }

    /**
     * Calculate accuracy of a solution.
     * This essentially do the calculation by using reasoner, but reasoner
     * is only being used to claculate the hornClause score. Then we are using set calculation for
     * candidateClass and candidateSolution's score.
     * So significantly faster than using reasoner all the time.
     * <p>
     * Reasoner chokes up/takes long time if ontology contains lot of individuals.
     * even though the individuals are not related to our experiment!!!!!!!!!!!
     *
     * @return
     */
    @Override
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

        logger.debug("solution: " + this.getSolutionAsString(true));
        logger.debug("\tcoveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.debug("\tcoveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.debug("\texcludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.debug("\texcludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());
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
    @Override
    public void calculateAccuracyByReasoner() {

        // add this class expression to ontology and reinitiate reasoner.
        OWLClassExpression owlClassExpression = this.getSolutionAsOWLClassExpression();
        // create a unique name
        OWLClass owlClass = SharedDataHolder.owlDataFactory.getOWLClass(Utility.getUniqueIRI());
        OWLAxiom eqAxiom = SharedDataHolder.owlDataFactory.getOWLEquivalentClassesAxiom(owlClass, owlClassExpression);
        ChangeApplied ca = SharedDataHolder.owlOntologyManager.addAxiom(SharedDataHolder.owlOntologyOriginal, eqAxiom);
        logger.debug("Adding candidateSolution.getSolutionAsOWLClassExpression to ontology Status: " + ca.toString());
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
        nrOfNegativeClassifiedAsPositive = excludedNegIndividualsMap.size();

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
        //candidateSolution.getScore()
        this.getScore().setPrecision_by_reasoner(precision);
        this.getScore().setRecall_by_reasoner(recall);
        this.getScore().setF_measure_by_reasoner(f_measure);
        this.getScore().setCoverage_by_reasoner(coverage);
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
