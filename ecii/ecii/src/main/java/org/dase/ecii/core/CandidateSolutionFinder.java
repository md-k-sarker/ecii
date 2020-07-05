package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/29/20.
*/

import org.dase.ecii.datastructure.CandidateClass;
import org.dase.ecii.datastructure.CandidateSolution;
import org.dase.ecii.datastructure.ConjunctiveHornClause;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Super class of many different versions of CandidateSolutionFinder
 */
public abstract class CandidateSolutionFinder implements ICandidateSolutionFinder {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public int solutionCounter = 0;
    public OWLOntology ontology;
    public OWLDataFactory owlDataFactory;
    public OWLOntologyManager owlOntologyManager;
    public OWLReasoner reasoner;
    public PrintStream out;
    public Monitor monitor;
    transient volatile protected int nrOfTotalIndividuals;
    transient volatile protected int nrOfPositiveIndividuals;
    transient volatile protected int nrOfNegativeIndividuals;
    /**
     * Required in subclasses to do the sorting
     */
    protected transient volatile int o1Length = 0;
    protected transient volatile int o2Length = 0;

    /**
     * public constructor
     */
    public CandidateSolutionFinder(OWLReasoner _reasoner, OWLOntology _ontology, PrintStream _printStream, Monitor _monitor) {
        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
        this.out = _printStream;
        this.monitor = _monitor;
    }

    /**
     * Init the variables
     */
    @Override
    public void initVariables() {

        nrOfPositiveIndividuals = SharedDataHolder.posIndivs.size();
        nrOfNegativeIndividuals = SharedDataHolder.negIndivs.size();
        nrOfTotalIndividuals = nrOfPositiveIndividuals + nrOfNegativeIndividuals;
    }

    //@formatter:off
    /**
     *
     *
     * @param tolerance
     * @param combinationLimit
     */
    //@formatter:on
    @Override
    public void findConcepts(double tolerance, int combinationLimit) {

        // find Object Types for each of the object property
        logger.info("extractObjectTypes started...............");
        for (Map.Entry<OWLObjectProperty, Double> entry : SharedDataHolder.objProperties.entrySet()) {
            logger.info("Extracting objectTypes using objectProperty: " + Utility.getShortName(entry.getKey()));
            extractObjectTypes(tolerance, entry.getKey());
        }
        logger.info("extractObjectTypes finished.");
        //debugExtractObjectTypes();

        if (ConfigParams.removeCommonTypes) {
            logger.info("Remove common types from positive and negative started.............");
            for (Map.Entry<OWLObjectProperty, Double> entry : SharedDataHolder.objProperties.entrySet()) {
                logger.debug("Removing common types using object proeprty: " + Utility.getShortName(entry.getKey()));
                removeCommonTypesFromPosAndNeg(entry.getKey());
            }
            logger.info("Remove common types from positive and negative finished.");
        }

        // create combination of objectproperties
        logger.info("createCombination of Objectproperties started...............");
        SharedDataHolder.objPropertiesCombination = createCombinationOfObjectProperties();
        logger.info("createCombination of Objectproperties finished.");

        // init variables
        initVariables();

        // save initial solutions
        logger.info("saveInitialSolutions started...............");
        createAndSaveSolutions();
        logger.info("saveInitialSolutions finished");

        // using candidatesolutionfinder.createAndSaveSolutions() we are creating all initial combination.
        // as we have used psoitive type and it's super classes and negative types and it's super classes, we are only left with refining with subClass.
        // For negative type no need to refine with subClass.
        // For positive type we can refine with subClass. TODO.

        // refining with subclass may be helpful especially for residue project.
    }

    /**
     * Extract all objects referred by the direct individuals, find their types and
     * save those reference to corresponding SharedDataHolder variable.
     * <p>
     * Specifically it saves those information in:
     * SharedDataHolder.typeOfObjectsInPosIndivs
     * SharedDataHolder.typeOfObjectsInNegIndivs
     * SharedDataHolder.individualHasObjectTypes
     *
     * <p>
     * Some ontology dont have object properties. So we need to use direct types without r filler for that case.
     * Implementation note: for no object property or direct/bare types we used SharedDataHolder.noneOWLObjProp.
     *
     * @param tolerance
     * @param owlObjectProperty
     */
    @Override
    public void extractObjectTypes(double tolerance, OWLObjectProperty owlObjectProperty) {
        logger.info("\tGiven obj property: " + Utility.getShortName(owlObjectProperty));


        // find the indivs and corresponding types of indivs which appeared in the positive individuals
        logger.info("\tSharedDataHolder.posIndivs.size(): " + SharedDataHolder.posIndivs.size());
        for (OWLNamedIndividual posIndiv : SharedDataHolder.posIndivs) {
            //bare type/direct type --------- indiv type using obj-prop
            if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                //for no object property or direct types we used SharedDataHolder.noneOWLObjProp
                logger.debug("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual.");
                HashSet<OWLClass> classHashSet = new HashSet<>(reasoner.getTypes(posIndiv, false).getFlattened());
                logger.debug("object count: " + classHashSet.size());
                classHashSet.forEach(posType -> {
                    logger.debug("posType: " + posType.toString());
                    if (!posType.equals(owlDataFactory.getOWLThing()) && !posType.equals(owlDataFactory.getOWLNothing())) {
                        // insert into individualObject's type count
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInPosIndivs, owlObjectProperty, posType);

                        //insert into individualObject to individualObject type mapping
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, posIndiv, owlObjectProperty, posType);
                    }
                });
            } else {

                logger.debug("Below concepts are type/supertype of positive "
                        + posIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI());
                HashSet<OWLNamedIndividual> objectsHashSet = new HashSet<>(
                        reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened());
                logger.debug("object count: " + objectsHashSet.size());
                objectsHashSet.forEach(eachIndi -> {
                    logger.debug("\tindi: " + eachIndi.getIRI());

                    // insert into individuals count
                    HashMapUtility.insertIntoHashMap(SharedDataHolder.objectsInPosIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(posType -> {
                        logger.debug("\t\tposType: " + posType.toString());
                        if (!posType.equals(owlDataFactory.getOWLThing()) && !posType.equals(owlDataFactory.getOWLNothing())) {
                            // insert into individualObject's type count
                            HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInPosIndivs, owlObjectProperty, posType);

                            //insert into individualObject to individualObject type mapping
                            HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, posIndiv, owlObjectProperty, posType);
                        }
                    });
                });
            }
        }

        // find the indivs and corresponding types of indivs which appeared in the negative images
        for (OWLNamedIndividual negIndiv : SharedDataHolder.negIndivs) {

            if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                //for no object property or direct types we used SharedDataHolder.noneOWLObjProp
                logger.debug("Below concepts are type/supertype of negative " + negIndiv.getIRI().toString() + " individual.");
                HashSet<OWLClass> classHashSet = new HashSet<>(reasoner.getTypes(negIndiv, false).getFlattened());
                logger.debug("object count: " + classHashSet.size());
                classHashSet.forEach(negType -> {
                    logger.debug("\t\tnegType: " + negType.toString());
                    if (!negType.equals(owlDataFactory.getOWLThing()) && !negType.equals(owlDataFactory.getOWLNothing())) {
                        // insert into individualObject's type count
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInNegIndivs, owlObjectProperty, negType);

                        //insert into individualObject to individualObject type mapping
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, negIndiv, owlObjectProperty, negType);
                    }
                });
            } else {
                logger.debug("Below concepts are type/supertype of negative " +
                        negIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI());
                HashSet<OWLNamedIndividual> objectsHashSet = new HashSet<>
                        (reasoner.getObjectPropertyValues(negIndiv, owlObjectProperty).getFlattened());
                logger.debug("object count: " + objectsHashSet.size());
                objectsHashSet.forEach(eachIndi -> {

                    // insert into individualObject count
                    HashMapUtility.insertIntoHashMap(SharedDataHolder.objectsInNegIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(negType -> {
                        logger.debug("\t\tnegType: " + negType.toString());
                        if (!negType.equals(owlDataFactory.getOWLThing()) && !negType.equals(owlDataFactory.getOWLNothing())) {
                            //insert into individualObject's type count
                            HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInNegIndivs, owlObjectProperty, negType);

                            // individualObject to individualObject type mapping
                            HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, negIndiv, owlObjectProperty, negType);
                        }
                    });
                });
            }
        }
    }

    /**
     * Test/Debug
     */
    private void debugExtractObjectTypes() {
        logger.debug("Testing extractObjectTypes:");
        logger.debug("posTypes:");
        SharedDataHolder.typeOfObjectsInPosIndivs.entrySet().forEach(entry -> {
            logger.debug("Object Property: " + Utility.getShortName(entry.getKey()) + " has related types:");
            entry.getValue().forEach((owlClassExpression, integer) -> {
                logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpression));
            });
        });
        logger.debug("negTypes:");
        SharedDataHolder.typeOfObjectsInNegIndivs.entrySet().forEach(entry -> {
            logger.debug("Object Property: " + Utility.getShortName(entry.getKey()) + " has related types:");
            entry.getValue().forEach((owlClassExpression, integer) -> {
                logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpression));
            });
        });
    }

    /**
     * Remove common types which appeared both in positive types and negative types.
     * Specifically, if removeCommonTypesFromOneSideOnly== true
     * remove from either pos-set or neg-set which have less individuals
     * not removing from both
     * else
     * remove from both side
     *
     * @param owlObjectProperty
     */
    @Override
    public void removeCommonTypesFromPosAndNeg(OWLObjectProperty owlObjectProperty) {

        logger.info("Before removing types (types which appeared in both pos and neg images): ");
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            logger.info("pos types: ");
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            logger.info("neg types: ");
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }

        logger.debug("Removing types which appeared both in pos and neg: ");
        HashSet<OWLClassExpression> removeFromPosType = new HashSet<>();
        HashSet<OWLClassExpression> removeFromNegType = new HashSet<>();
        HashSet<OWLClassExpression> removeFromBothType = new HashSet<>();

        //HashSet<OWLClassExpression> typesBothInPosAndNeg = new HashSet<>();
        // remove those posTypes which also appeared in negTypes using some kind of accuracy/tolerance measure.
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            for (OWLClassExpression owlClassExpr : SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet()) {
                // use tolerance measure
                // need to exclude types which appear in neg images
                if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                    if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(owlClassExpr)) {

                        // remove from that which have less individuals
                        double posRatio = SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).
                                get(owlClassExpr) / SharedDataHolder.posIndivs.size();
                        double negRatio = SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).
                                get(owlClassExpr) / SharedDataHolder.negIndivs.size();

                        if (posRatio >= negRatio) {
                            // remove from negative
                            removeFromNegType.add(owlClassExpr);
                            logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpr) + " will be removed from negativeTypes");
                        } else {
                            // remove from positive
                            removeFromPosType.add(owlClassExpr);
                            logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpr) + " will be removed from positiveTypes");
                        }
                    }
                }
            }
        }

        if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
            removeFromBothType.addAll(removeFromPosType);
            removeFromBothType.addAll(removeFromNegType);
        }

        // remove those and owl:Thing and owl:Nothing
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
                SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().removeAll(removeFromBothType);
            } else
                SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().removeAll(removeFromPosType);
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLThing());
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLNothing());
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
                SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().removeAll(removeFromBothType);
            } else
                SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().removeAll(removeFromNegType);
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLThing());
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLNothing());
        }

        logger.info("After removing types (types which appeared in both pos and neg images): ");
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            logger.info("pos types: ");
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            logger.info("neg types: ");
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
    }

    /**
     * Create combination of object properties. this is done just one time.
     */
    protected ArrayList<ArrayList<OWLObjectProperty>> createCombinationOfObjectProperties() {
        ArrayList<OWLObjectProperty> objectPropertyArrayList = new ArrayList<>(SharedDataHolder.objProperties.keySet());

        // combination of all positiveType
        ArrayList<ArrayList<OWLObjectProperty>> listCombination = new ArrayList<>();
        // combination of 2
        if (ConfigParams.objPropsCombinationLimit >= 2)
            listCombination = Utility.combinationHelper(objectPropertyArrayList, 2);

        // combination from 3 to upto conceptsCombinationLimit or k3 limit
        for (int combinationCounter = 3; combinationCounter <= ConfigParams.objPropsCombinationLimit; combinationCounter++) {
            // combination of combinationCounter
            listCombination.addAll(Utility.combinationHelper(objectPropertyArrayList, combinationCounter));
        }
        return listCombination;
    }

    /**
     * // a combination is valid if and only if it doesn't have self subClass.
     *
     * @param aList
     * @return
     */
    protected boolean isValidCombinationOfSubClasses(ArrayList<OWLClassExpression> aList) {
        boolean isValid = false;
        boolean shouldSkip = false;

        // if the list contains self subClassOF relation then discard it.
        for (int j = 0; j < aList.size(); j++) {
            OWLClassExpression owlClassExpression1 = aList.get(j);
            List<OWLClassExpression> subClasses = reasoner.getSubClasses(owlClassExpression1, false).getFlattened().stream().collect(Collectors.toList());
            int k = 0;
            for (k = 0; k < aList.size(); k++) {
                OWLClassExpression owlClassExpression2 = aList.get(k);
                if (!owlClassExpression1.equals(owlClassExpression2)) {
                    if (subClasses.contains(owlClassExpression2)) {
                        shouldSkip = true;
                        break;
                    }
                }
            }
            if (shouldSkip) {
                break;
            }
        }

        isValid = !shouldSkip;

        return isValid;
    }

    /**
     * a combination is valid if and only if it doesn't have self subClass.
     * This is now a hard problem. It can be easily analyzed if the hornClause only have positive type, but no idea, if it also contains negative types.
     * todo(zaman): need to formulate idea to solve this.
     *
     * @param aList
     * @return
     */
    protected boolean isValidCombinationOfHornClauses(ArrayList<? extends ConjunctiveHornClause> aList) {
        boolean isValid = false;
        boolean shouldSkip = false;

        // if the list contains self subClassOF relation then discard it.
        for (int j = 0; j < aList.size(); j++) {
            OWLClassExpression owlClassExpression1 = aList.get(j).getConjunctiveHornClauseAsOWLClassExpression();
            if (null != owlClassExpression1) {
                // todo(zaman): this is runtime expensive to call reasoner here. deduce some method to find easy way.
                List<OWLClassExpression> subClasses = reasoner.getSubClasses(owlClassExpression1, false).getFlattened().stream().collect(Collectors.toList());
                int k = 0;
                for (k = 0; k < aList.size(); k++) {
                    OWLClassExpression owlClassExpression2 = aList.get(k).getConjunctiveHornClauseAsOWLClassExpression();
                    if (!owlClassExpression1.equals(owlClassExpression2)) {
                        if (subClasses.contains(owlClassExpression2)) {
                            shouldSkip = true;
                            break;
                        }
                    }
                }
                if (shouldSkip) {
                    break;
                }
            } else {
                shouldSkip = true;
                break;
            }
        }

        isValid = !shouldSkip;

        return isValid;
    }

    /**
     * a combination is valid if and only if it doesn't have self subClass.
     * todo(zaman): This is now a hard problem. now we are doing this using reasoner call, which is so expensive.
     * need to formulate idea to solve this.
     *
     * @param aList
     * @return
     */
    protected boolean isValidCombinationOfCandidateClasses(ArrayList<CandidateClass> aList) {
        boolean isValid = false;
        boolean shouldSkip = false;

        // if the list contains self subClassOF relation then discard it.
        for (int j = 0; j < aList.size(); j++) {
            OWLClassExpression owlClassExpression1 = aList.get(j).getCandidateClassAsOWLClassExpression();
            // todo(zaman): this is runtime expensive to call reasoner here. deduce some method to find easy way.
            List<OWLClassExpression> subClasses = reasoner.getSubClasses(owlClassExpression1, false).getFlattened().stream().collect(Collectors.toList());
            int k = 0;
            for (k = 0; k < aList.size(); k++) {
                OWLClassExpression owlClassExpression2 = aList.get(k).getCandidateClassAsOWLClassExpression();
                if (!owlClassExpression1.equals(owlClassExpression2)) {
                    if (subClasses.contains(owlClassExpression2)) {
                        shouldSkip = true;
                        break;
                    }
                }
            }
            if (shouldSkip) {
                break;
            }
        }

        isValid = !shouldSkip;

        return isValid;
    }

    /**
     * Calculate the accuracy of top K6 solutions by reasoner
     *
     * @param sortedCandidateSolutionList
     * @param K6
     */
    @Override
    public void calculateAccuracyOfTopK6ByReasoner(ArrayList<? extends CandidateSolution> sortedCandidateSolutionList, int K6) {

        if (sortedCandidateSolutionList.size() < K6) {
            sortedCandidateSolutionList.forEach(candidateSolution -> {
                candidateSolution.calculateAccuracyByReasoner();
            });
        } else {
            for (int i = 0; i < K6; i++) {
                sortedCandidateSolutionList.get(i).calculateAccuracyByReasoner();
            }
        }
    }


}
