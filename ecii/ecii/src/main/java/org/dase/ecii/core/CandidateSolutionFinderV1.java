package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV1;
import org.dase.ecii.datastructure.CandidateSolutionV1;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV1V2;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.dase.ecii.util.ConfigParams;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")


/**
 * Find solution using the algorithm mentioned in the paper. This solves the problem of creating combination of disjunctions..
 * Algorithm version: V4
 *
 */
public class CandidateSolutionFinderV1 {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int solutionCounter = 0;

    /**
     * This is temporary hashMap used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> hornClausesMap;

    /**
     * This is temporary hashSet used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV1>> candidateClassesMap;

    public OWLOntology ontology;
    public OWLDataFactory owlDataFactory;
    public OWLOntologyManager owlOntologyManager;
    public OWLReasoner reasoner;
    public PrintStream out;
    public Monitor monitor;
    protected transient volatile int o1Length = 0;
    protected transient volatile int o2Length = 0;
    transient volatile protected int nrOfTotalIndividuals;
    transient volatile protected int nrOfPositiveIndividuals;
    transient volatile protected int nrOfNegativeIndividuals;

    /**
     * Constructor
     *
     * @param _reasoner
     * @param _ontology
     */
    public CandidateSolutionFinderV1(OWLReasoner _reasoner, OWLOntology _ontology, PrintStream _printStream, Monitor _monitor) {
        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
        this.out = _printStream;
        this.monitor = _monitor;
        this.hornClausesMap = new HashMap<>();
        this.candidateClassesMap = new HashMap<>();
    }

    //@formatter:off
    /**
     *
     *
     * @param tolerance
     * @param combinationLimit
     */
    //@formatter:on
    public void findConcepts(double tolerance, int combinationLimit) {

        // find Object Types for each of the object property
        logger.info("extractObjectTypes started...............");
        for (Map.Entry<OWLObjectProperty, Double> entry : SharedDataHolder.objProperties.entrySet()) {
            logger.info("Extracting objectTypes using objectProperty: " + Utility.getShortName(entry.getKey()));
            extractObjectTypes(tolerance, entry.getKey());
        }
        logger.info("extractObjectTypes finished.");
//        debugExtractObjectTypes();

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
        saveInitialSolutionsCustom();
        logger.info("saveInitialSolutions finished");

    }

    /**
     * Init the variables
     */
    public void initVariables() {

        nrOfPositiveIndividuals = SharedDataHolder.posIndivs.size();
        nrOfNegativeIndividuals = SharedDataHolder.negIndivs.size();
        nrOfTotalIndividuals = nrOfPositiveIndividuals + nrOfNegativeIndividuals;
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
     * Utility/Helper method to add solution to solutionsSet.
     *
     * @param candidateSolutionV1
     */
    private boolean addToSolutions(CandidateSolutionV1 candidateSolutionV1) {

        if (!SharedDataHolder.CandidateSolutionSetV1.contains(candidateSolutionV1)) {
            // calculate score
            Score accScore = candidateSolutionV1.calculateAccuracyComplexCustom();
            if (accScore.getDefaultScoreValue() > 0) {
                candidateSolutionV1.setScore(accScore);
                // save to shared data holder
                SharedDataHolder.CandidateSolutionSetV1.add(candidateSolutionV1);
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * a combination is valid if and only if it doesn't have self subClass.
     * todo(zaman): This is now a hard problem. now we are doing this using reasoner call, which is so expensive. need to formulate idea to solve this.
     *
     * @param aList
     * @return
     */
    private boolean isValidCombinationOfCandidateClasses(ArrayList<CandidateClassV1> aList) {
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
     * a combination is valid if and only if it doesn't have self subClass.
     * This is now a hard problem. It can be easily analyzed if the hornClause only have positive type, but no idea, if it also contains negative types.
     * todo(zaman): need to formulate idea to solve this.
     *
     * @param aList
     * @return
     */
    private boolean isValidCombinationOfHornClauses(ArrayList<ConjunctiveHornClauseV1V2> aList) {
        boolean isValid = false;
        boolean shouldSkip = false;

        // if the list contains self subClassOF relation then discard it.
        for (int j = 0; j < aList.size(); j++) {
            OWLClassExpression owlClassExpression1 = aList.get(j).getConjunctiveHornClauseAsOWLClassExpression();
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
        }

        isValid = !shouldSkip;

        return isValid;
    }

    /**
     * // a combination is valid if and only if it doesn't have self subClass.
     *
     * @param aList
     * @return
     */
    private boolean isValidCombinationOfSubClasses(ArrayList<OWLClassExpression> aList) {
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
     * save the initial solutions into SharedDataHolder.candidateSolutionV0Set object.
     */
    public void saveInitialSolutionsCustom() {

        // for rfilled types and for bare types. for no object property/direct/bare types we used SharedDataHolder.noneOWLObjProp

        // create solution using just one class expression.

        // solution using only a single positive type
        logger.info("\nSolution using only a single positive type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                //create conjunctive horn clause and add positive part and no negative part initially
                ConjunctiveHornClauseV1V2 conjunctiveHornClauseV1V2 = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                conjunctiveHornClauseV1V2.addPosObjectType(posOwlClassExpression);

                // create candidate class
                CandidateClassV1 candidateClassV1 = new CandidateClassV1(owlObjectProperty, reasoner, ontology);
                candidateClassV1.addConjunctiveHornClauses(conjunctiveHornClauseV1V2);

                // create candidate solution
                CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1(reasoner, ontology);
                candidateSolutionV1.addCandidateClass(candidateClassV1);
                boolean added = addToSolutions(candidateSolutionV1);
                if (added) {
                    // save temporarily for combination
                    Score hornClauseScore = conjunctiveHornClauseV1V2.calculateAccuracyComplexCustom();
                    conjunctiveHornClauseV1V2.setScore(hornClauseScore);
                    HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1V2);

                    Score candidateClassScore = candidateClassV1.calculateAccuracyComplexCustom();
                    candidateClassV1.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV1);
                }
            });
        });
        logger.info("solution using only a single positive type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        // should we use only negative type without a single positive type in Conjunctive Horn Clauses?
        // no. not compatible with v1,v2 hornclauses
        // essentially ecii_v1 and ecii_v2 ssolutions

        // create solution using both positive and negative of class expressions.
        // single positive and single negative.
        logger.info("solution using only a single positive and single negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressions = new ArrayList<>(
                        reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));

                posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {
                    if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                        // if subclass of this class is included in the negative type
                        if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)) {

                            //create conjunctive horn clause and add positive part and negative part too
                            ConjunctiveHornClauseV1V2 conjunctiveHornClause = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                            conjunctiveHornClause.addPosObjectType(posOwlClassExpression);
                            conjunctiveHornClause.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty, reasoner, ontology);
                            candidateClass.addConjunctiveHornClauses(conjunctiveHornClause);

                            // create candidate solution
                            CandidateSolutionV1 candidateSolution = new CandidateSolutionV1(reasoner, ontology);
                            candidateSolution.addCandidateClass(candidateClass);
                            boolean added = addToSolutions(candidateSolution);
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClause.calculateAccuracyComplexCustom();
                                conjunctiveHornClause.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);

                                Score candidateClassScore = candidateClass.calculateAccuracyComplexCustom();
                                candidateClass.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                            }
                        }
                    }
                });
            });
        });
        logger.info("solution using only a single positive and single negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        logger.info("solution using multiple positive and multiple negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.entrySet().stream()
                .filter(owlObjectPropertyHashMapEntry ->
                        owlObjectPropertyHashMapEntry.getValue().entrySet().size() > 0)
                .forEach(owlObjectPropertyHashMapEntry -> {

                    OWLObjectProperty owlObjectProperty = owlObjectPropertyHashMapEntry.getKey();
                    HashMap<OWLClassExpression, Integer> hashMap = owlObjectPropertyHashMapEntry.getValue();
                    // ).forEach((owlObjectProperty, hashMap) ->
                    ArrayList<OWLClassExpression> allPosTypes = new ArrayList<>(hashMap.keySet());

                    ArrayList<ArrayList<OWLClassExpression>> listCombinationOfPosClassesForPosPortion = new ArrayList<>();
                    // making a list of 2 item means it will consist of single item, this is to reduce the code from uppper portions.
                    if (ConfigParams.conceptLimitInPosExpr >= 2)
                        listCombinationOfPosClassesForPosPortion = Utility.combinationHelper(allPosTypes, 2);
                    // combination of 3 to the limit
                    for (int combinationCounter = 3; combinationCounter < ConfigParams.conceptLimitInPosExpr; combinationCounter++) {
                        listCombinationOfPosClassesForPosPortion.addAll(Utility.combinationHelper(allPosTypes, combinationCounter));
                    }
                    logger.info("listCombinationOfPosClassesForPosPortion size: " + listCombinationOfPosClassesForPosPortion.size());

                    // keep only valid listCombinationOfPosClassesForPosPortion.
                    // a combination is valid if and only if it doesn't have self subClass.
                    ArrayList<ArrayList<OWLClassExpression>> validListCombinationOfPosClassesForPosPortion = new ArrayList<>();
                    listCombinationOfPosClassesForPosPortion.forEach(classExpressions -> {
//                logger.info("debug: classExpressions.size(): " + classExpressions.size());
                        if (isValidCombinationOfSubClasses(classExpressions)) {
                            validListCombinationOfPosClassesForPosPortion.add(classExpressions);
                        }
                    });
                    // recover memory
                    listCombinationOfPosClassesForPosPortion = null;
                    logger.info("validListCombinationOfPosClassesForPosPortion size: " + validListCombinationOfPosClassesForPosPortion.size());

                    validListCombinationOfPosClassesForPosPortion.forEach(posOwlClassExpressions -> {

                        HashSet<OWLClassExpression> posTypeOwlSubClassExpressions = new HashSet<>();

                        for (OWLClassExpression posOwlClassExpression : posOwlClassExpressions) {
                            posTypeOwlSubClassExpressions.addAll(
                                    reasoner.getSubClasses(posOwlClassExpression, false)
                                            .getFlattened().stream().collect(Collectors.toList()));
                        }

                        ArrayList<OWLClassExpression> posTypeOwlSubClassExpressionsForCombination = new ArrayList<>();

                        // create combination only those which are contained in the negative type.
                        // TODO(zaman): Can we remove this restriction?
                        //  Probably we can allow all concept without the superclasses of postype. reasoner.getsuperclasses()
                        posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {
                            if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                                // if subclass of this class is included in the negative type
                                if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty)
                                        .containsKey(subClassOwlClassExpression)) {
                                    posTypeOwlSubClassExpressionsForCombination.add(subClassOwlClassExpression);
                                }
                            }
                        });
                        // recover memory
                        posTypeOwlSubClassExpressions = null;


                        ArrayList<ArrayList<OWLClassExpression>> listCombinationOfSubClassesForNegPortion;
                        // combination of 1, starting from 1 for negtypes. posTypes are at-least 2 here.
                        listCombinationOfSubClassesForNegPortion = Utility
                                .combinationHelper(posTypeOwlSubClassExpressionsForCombination, 1);
                        // combination from 2 to upto ccombinationLimit
                        for (int combinationCounter = 2; combinationCounter <= ConfigParams.conceptLimitInNegExpr; combinationCounter++) {
                            // combination of combinationCounter
                            listCombinationOfSubClassesForNegPortion.addAll(Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, combinationCounter));
                        }
                        logger.debug("listCombinationOfSubClassesForNegPortion size: " + listCombinationOfSubClassesForNegPortion.size());

                        // keep only valid listCombinationOfSubClassesForNegPortion.
                        // a combination is valid if and only if it doesn't have self subClass.
                        ArrayList<ArrayList<OWLClassExpression>> validListCombinationOfSubClassesForNegPortion = new ArrayList<>();
                        listCombinationOfSubClassesForNegPortion.forEach(classExpressions -> {
                            if (isValidCombinationOfSubClasses(classExpressions)) {
                                validListCombinationOfSubClassesForNegPortion.add(classExpressions);
                            }
                        });
                        // recover memory
                        listCombinationOfSubClassesForNegPortion = null;
                        logger.debug("validListCombinationOfSubClassesForNegPortion size: " + validListCombinationOfSubClassesForNegPortion.size());

                        // now combine the postypes and negtypes
                        // null pointer because in the soutions the postypes is empty or 0
//                logger.info("debug: posOwlClassExpressions: " + posOwlClassExpressions.size());
                        validListCombinationOfSubClassesForNegPortion.forEach(subClasses -> {

                            // if every class of this combination is in negative types then include this combination otherwise skip this.
                            // this is trivially true as we are creating combination of those subclasses which are also contained in the negTypes.

                            //create conjunctive horn clause and add positive part and negative part too
                            ConjunctiveHornClauseV1V2 conjunctiveHornClauseV1V2 = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                            conjunctiveHornClauseV1V2.setPosObjectTypes(posOwlClassExpressions);
                            conjunctiveHornClauseV1V2.setNegObjectTypes(subClasses);

                            // create candidate class
                            CandidateClassV1 candidateClassV1 = new CandidateClassV1(owlObjectProperty, reasoner, ontology);
                            candidateClassV1.addConjunctiveHornClauses(conjunctiveHornClauseV1V2);

                            // create candidate solution
                            CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1(reasoner, ontology);
                            candidateSolutionV1.addCandidateClass(candidateClassV1);
                            boolean added = addToSolutions(candidateSolutionV1);
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClauseV1V2.calculateAccuracyComplexCustom();
                                conjunctiveHornClauseV1V2.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1V2);

                                Score candidateClassScore = candidateClassV1.calculateAccuracyComplexCustom();
                                candidateClassV1.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV1);
                            }
                        });

                    });
                });
        logger.info("solution using multiple positive and multiple negative type finished. Total Solutions: "
                + SharedDataHolder.CandidateSolutionSetV1.size());

        /**
         * Select top k5 hornClauses to make combination. This function reduces the hornClauseMap size.
         */
        SortingUtility.sortAndFilterHornClauseV1V2Map(hornClausesMap, ConfigParams.hornClausesListMaxSize);

        /**
         * combination of horn clause. (upto K2/hornClauseLimit limit).
         * Use previously created horn clauses.
         *
         *  valid combination of hornClause:
         *  1. One hornclause must not be subclass of another hornclause. Verifying this is hard when we have negative portion in hornclause.
         *  2. Satisfy the 2 different types mentioned below.
         *  Bare Types:
         *      1. HornClauses will be added by conjunction
         *  RFilled types:
         *      1. Hornclauses will be added by disjunction.
         *
         *  todo(zaman): need to modify the accuracy and print methods accordingly.
         */
        logger.info("solution using combination of horn clause started...............");
        hornClausesMap.forEach((owlObjectProperty, conjunctiveHornClauses) -> {
            logger.info("\tcombination of horn clause using object property " +
                    Utility.getShortName(owlObjectProperty) + " started...............");
            ArrayList<ConjunctiveHornClauseV1V2> hornClauseArrayList = new ArrayList<>(conjunctiveHornClauses);
            logger.info("\thorn clause size: " + hornClauseArrayList.size());

            ArrayList<ArrayList<ConjunctiveHornClauseV1V2>> listCombinationOfHornClauses = new ArrayList<>();
            // combination of 2
            if (ConfigParams.hornClauseLimit >= 2)
                listCombinationOfHornClauses = Utility.combinationHelper(hornClauseArrayList, 2);
            // combination from 3 to upto ccombinationLimit
            for (int combinationCounter = 3; combinationCounter <= ConfigParams.hornClauseLimit; combinationCounter++) {
                // combination of combinationCounter
                listCombinationOfHornClauses.addAll(Utility.combinationHelper(hornClauseArrayList, combinationCounter));
            }
            logger.info("listCombinationOfHornClauses size: " + listCombinationOfHornClauses.size());
            //  Valid combination of hornClauses.
            //  It's producing (Human and Mammal) And (Human and Animal)
            // which is weird, it can be reduced to (Human and Mammal and Animal)
            ArrayList<ArrayList<ConjunctiveHornClauseV1V2>> validListCombinationOfHornClauses = new ArrayList<>();
            listCombinationOfHornClauses.forEach(classExpressions -> {
                if (isValidCombinationOfHornClauses(classExpressions)) {
                    validListCombinationOfHornClauses.add(classExpressions);
                }
            });
            logger.info("validListCombinationOfHornClauses size: " + validListCombinationOfHornClauses.size());

            validListCombinationOfHornClauses.forEach(conjunctiveHornClausesCombination -> {
                //create candidate class
                CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty, reasoner, ontology);
                candidateClass.setConjunctiveHornClauses(conjunctiveHornClausesCombination);

                // create candidate solution
                CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1(reasoner, ontology);
                candidateSolutionV1.addCandidateClass(candidateClass);
                boolean added = addToSolutions(candidateSolutionV1);
                if (added) {
                    // save temporarily for combination
                    Score candidateClassScore = candidateClass.calculateAccuracyComplexCustom();
                    candidateClass.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                }
            });
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        });
        logger.info("solution using combination of horn clause finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());


        /**
         * Select top k6 CandidateClasses to make combination. This function reduces the candidate Classes size.
         */
        SortingUtility.sortAndFilterCandidateClassV1Map(candidateClassesMap, ConfigParams.candidateClassesListMaxSize);

        /**
         * combination of candidateClass/objectproperties. (upto K3/objPropsCombinationLimit limit)
         *
         */
        logger.info("solution using combination of object proeprties/candidateClass started...............");
        SharedDataHolder.objPropertiesCombination.forEach(owlObjectProperties -> {

            List<Collection<CandidateClassV1>> origList = new ArrayList<>();
            candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
                if (owlObjectProperties.contains(owlObjectProperty)) {
                    origList.add(candidateClasses);
                }
            });
            Collection<List<CandidateClassV1>> objPropsCombination = Utility.restrictedCombinationHelper(origList);

            //  Valid combination of ObjectProperties.
            objPropsCombination.forEach(candidateClasses -> {

                // create candidate solution
                CandidateSolutionV1 candidateSolution = new CandidateSolutionV1(reasoner, ontology);
                candidateSolution.setCandidateClasses(new ArrayList<>(candidateClasses));
                addToSolutions(candidateSolution);
            });
        });
        logger.info("solution using combination of object proeprties/candidateClass finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());
    }

    /**
     * Extract all objects reffered by the direct individuals and find their types.
     * <p>
     * Some ontology dont have object properties. So we need to use direct types without r filler for that case.
     * Implementation note: for no object property or direct/bare types we used SharedDataHolder.noneOWLObjProp.
     *
     * @param tolerance
     * @param owlObjectProperty
     */
    private void extractObjectTypes(double tolerance, OWLObjectProperty owlObjectProperty) {
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

                logger.debug("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI());
                HashSet<OWLNamedIndividual> objectsHashSet = new HashSet<>(reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened());
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
     * Create combination of object properties. this is done just one time.
     */
    private ArrayList<ArrayList<OWLObjectProperty>> createCombinationOfObjectProperties() {
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

//    transient volatile protected int nrOfTotalIndividuals;
//    transient volatile protected int nrOfPositiveIndividuals;
//    transient volatile protected int nrOfNegativeIndividuals;

    /**
     * @param K6
     */
    public void calculateAccuracyOfTopK6ByReasoner(int K6) {
        if (SharedDataHolder.SortedCandidateSolutionListV1.size() < K6) {
            SharedDataHolder.SortedCandidateSolutionListV1.forEach(candidateSolution -> {
                candidateSolution.calculateAccuracyByReasoner();
            });
        } else {
            for (int i = 0; i < K6; i++) {
                SharedDataHolder.SortedCandidateSolutionListV1.get(i).calculateAccuracyByReasoner();
            }
        }
    }

    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("####################Solutions####################:");
        monitor.writeMessage("\n####################Solutions####################:");
        solutionCounter = 0;

        SharedDataHolder.SortedCandidateSolutionListV1.forEach((solution) -> {

            if (solution.getGroupedCandidateClasses().size() > 0) {
                solutionCounter++;

                String solutionAsString = solution.getSolutionAsString(true);

                if (solutionAsString.length() > 0 && null != solution.getScore()) {
                    //logger.info("solution " + solutionCounter + ": " + solutionAsString);
                    monitor.writeMessage("solution " + solutionCounter + ": " + solutionAsString);
//                    DLSyntaxRendererExt dlRenderer = new DLSyntaxRendererExt();
//                    monitor.writeMessage("\tsolution pretty-printed by reasoner: " +
//                            dlRenderer.render(solution.getSolutionAsOWLClassExpression()));

                    if (solutionCounter < K6) {
                        //logger.info("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score_by_reasoner: " + solution.getScore().getCoverage_by_reasoner());

                        monitor.writeMessage("\t precision: " + solution.getScore().getPrecision());
                        monitor.writeMessage("\t precision_by_reasoner: " + solution.getScore().getPrecision_by_reasoner());

                        monitor.writeMessage("\t recall: " + solution.getScore().getRecall());
                        monitor.writeMessage("\t recall_by_reasoner: " + solution.getScore().getRecall_by_reasoner());

                        monitor.writeMessage("\t f_measure: " + solution.getScore().getF_measure());
                        monitor.writeMessage("\t f_measure_by_reasoner: " + solution.getScore().getF_measure_by_reasoner());
                    } else {
                        //logger.info("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score: " + solution.getScore().getCoverage());

                        monitor.writeMessage("\t precision: " + solution.getScore().getPrecision());
                        monitor.writeMessage("\t recall: " + solution.getScore().getRecall());
                        monitor.writeMessage("\t f_measure: " + solution.getScore().getF_measure());

                    }
                }
            }
        });

        logger.info("Total solutions found using raw list: " + SharedDataHolder.SortedCandidateSolutionListV1.size());
        logger.info("Total solutions found after removing empty solution: " + solutionCounter);
        monitor.writeMessage("\nTotal solutions found: " + solutionCounter);

    }


    /**
     * TestDLSyntaxRendering the functionality
     *
     * @param args
     * @throws OWLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("javadoc")
    public static void main(String[] args)
            throws OWLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

    }
}