package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV2;
import org.dase.ecii.datastructure.CandidateSolutionV2;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV1V2;
import org.dase.ecii.datastructure.HashMapUtility;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@SuppressWarnings("Duplicates")


/**
 * Find solution using the algorithm mentioned in the paper. This solves the problem of creating combination of disjunctions..
 * Algorithm version: V4
 *
 */
public class CandidateSolutionFinderV2 {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OWLOntology ontology;
    private final OWLDataFactory owlDataFactory;
    private final OWLOntologyManager owlOntologyManager;
    private OWLReasoner reasoner;
    private final PrintStream out;
    private final Monitor monitor;

    private int solutionCounter = 0;

    /**
     * This is temporary hashMap used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> hornClausesMap;

    /**
     * This is temporary hashSet used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV2>> candidateClassesMap;

    /**
     * Constructor
     *
     * @param _reasoner
     * @param _ontology
     */
    public CandidateSolutionFinderV2(OWLReasoner _reasoner, OWLOntology _ontology, PrintStream _printStream, Monitor _monitor) {
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

        // using candidatesolutionfinder.saveInitialSolutionsCustom() we are creating all initial combination.
        // as we have used psoitive type and it's super classes and negative types and it's super classes, we are only left with refining with subClass.
        // For negative type no need to refine with subClass.
        // For positive type we can refine with subClass. TODO.

        // refining with subclass may be helpful especially for residue project.

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
     * Utility/Helper method to add solution to solutionsSet.
     *
     * @param CandidateSolutionV2
     */
    private boolean addToSolutions(CandidateSolutionV2 CandidateSolutionV2) {

        if (!SharedDataHolder.CandidateSolutionSetV2.contains(CandidateSolutionV2)) {
            // calculate score
            Score accScore = CandidateSolutionV2.calculateAccuracyComplexCustom();
            if (accScore.getDefaultScoreValue() > 0) {
                CandidateSolutionV2.setScore(accScore);
                // save to shared data holder
                SharedDataHolder.CandidateSolutionSetV2.add(CandidateSolutionV2);
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
    private boolean isValidCombinationOfCandidateClasses(ArrayList<CandidateClassV2> aList) {
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

        // solution using only a single positive type
        logger.info("\nSolution using only a single positive type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {

            logger.info(" \t size of typeOfObjectsInPosIndivs hashMap: " + hashMap.size());
            if (hashMap.size() > ConfigParams.posClassListMaxSize) {
                hashMap = new HashMap<>(hashMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .limit(ConfigParams.posClassListMaxSize * ConfigParams.multiplicationConstant)
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
                logger.info(" \t size of typeOfObjectsInPosIndivs hashMap after limiting: " + hashMap.size());
            }

            hashMap.forEach((posOwlClassExpression, integer) -> {

                //create conjunctive horn clause and add positive part and no negative part initially
                ConjunctiveHornClauseV1V2 conjunctiveHornClauseV1V2 = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                conjunctiveHornClauseV1V2.addPosObjectType(posOwlClassExpression);

                // create candidate class
                CandidateClassV2 candidateClassV2 = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                candidateClassV2.addConjunctiveHornClauses(conjunctiveHornClauseV1V2);

                // create candidate solution
                CandidateSolutionV2 candidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                candidateSolutionV2.addCandidateClass(candidateClassV2);
                boolean added = addToSolutions(candidateSolutionV2);
                if (added) {
                    // save temporarily for combination
                    Score hornClauseScore = conjunctiveHornClauseV1V2.calculateAccuracyComplexCustom();
                    conjunctiveHornClauseV1V2.setScore(hornClauseScore);
                    HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1V2);

                    Score candidateClassScore = candidateClassV2.calculateAccuracyComplexCustom();
                    candidateClassV2.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV2);
                }

            });
        });
        logger.info("solution using only a single positive type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());


        // create solution using both positive and negative of class expressions.
        // single positive and single negative.
        logger.info("solution using only a single positive and single negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {

            logger.info(" \t size of typeOfObjectsInPosIndivs hashMap: " + hashMap.size());
            if (hashMap.size() > ConfigParams.posClassListMaxSize) {
                hashMap = new HashMap<>(hashMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .limit(ConfigParams.posClassListMaxSize * ConfigParams.multiplicationConstant)
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
                logger.info(" \t size of typeOfObjectsInPosIndivs hashMap after limiting: " + hashMap.size());
            }

            logger.info(" \t size of typeOfObjectsInPosIndivs hashMap after limiting: " + hashMap.size());
            hashMap.forEach((posOwlClassExpression, integer) -> {
                logger.info("posOwlClassExpression: "+ posOwlClassExpression);
                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressions = new ArrayList<>(
                        reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));

                posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {


                    if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                        // if subclass of this class is included in the negative type
                        if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)
                                && SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).get(subClassOwlClassExpression) > ConfigParams.typeOfObjectsInNegIndivsMinSize) {

                            //create conjunctive horn clause and add positive part and negative part too
                            ConjunctiveHornClauseV1V2 conjunctiveHornClause = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                            conjunctiveHornClause.addPosObjectType(posOwlClassExpression);
                            conjunctiveHornClause.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV2 candidateClass = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                            candidateClass.addConjunctiveHornClauses(conjunctiveHornClause);

                            // create candidate solution
                            CandidateSolutionV2 candidateSolution = new CandidateSolutionV2(reasoner, ontology);
                            candidateSolution.addCandidateClass(candidateClass);
                            ////////////////////////////////////////
                            ///// taking long time
                            logger.info("addToSolutions() started.............");
                            boolean added = addToSolutions(candidateSolution);
                            logger.info("addToSolutions() finished");
                            /////////////////////////////////////
                            logger.info("creating candidate solution finished: "+ candidateSolution.getSolutionAsString(false));
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClause.calculateAccuracyComplexCustom();
                                conjunctiveHornClause.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);

                                Score candidateClassScore = candidateClass.calculateAccuracyComplexCustom();
                                candidateClass.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                            }
                            logger.info("adding candidate solution finished: "+ candidateClassesMap.size());
                        }
                    }
                });
            });
        });
        logger.info("solution using only a single positive and single negative type finished." +
                " Total Solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());


        logger.info("solution using multiple positive and multiple negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.entrySet().stream()
                .filter(owlObjectPropertyHashMapEntry ->
                        owlObjectPropertyHashMapEntry.getValue().entrySet().size() > 0)
                .forEach(owlObjectPropertyHashMapEntry -> {


                    OWLObjectProperty owlObjectProperty = owlObjectPropertyHashMapEntry.getKey();
                    HashMap<OWLClassExpression, Integer> hashMap = new HashMap<>(
                            owlObjectPropertyHashMapEntry.getValue().entrySet()
                                    .stream().filter(e -> e.getValue() > ConfigParams.typeOfObjectsInPosIndivsMinSize)
                                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

                    logger.info("\t HashMap<OWLClassExpression, Integer> size before limiting:  " + hashMap.size());
                    ArrayList<OWLClassExpression> limitedPosTypes = new ArrayList<>(hashMap.entrySet()
                            .stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .limit(ConfigParams.posClassListMaxSize * ConfigParams.multiplicationConstant)
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet());

                    logger.info("\t HashMap<OWLClassExpression, Integer> size after limiting:  " + limitedPosTypes.size());

                    // positive portion
                    ArrayList<ArrayList<OWLClassExpression>> listCombinationOfPosClassesForPosPortion = new ArrayList<>();
                    // making a list of 2 item means it will consist of single item, this is to reduce the code from uppper portions.
                    if (ConfigParams.conceptLimitInPosExpr >= 2)
                        listCombinationOfPosClassesForPosPortion = Utility.combinationHelper(limitedPosTypes, 2);
                    // combination of 3 to the limit
                    for (int combinationCounter = 3; combinationCounter < ConfigParams.conceptLimitInPosExpr; combinationCounter++) {
                        listCombinationOfPosClassesForPosPortion.addAll(Utility.combinationHelper(limitedPosTypes, combinationCounter));
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

                    // negated portion
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
                                if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)
                                        && SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty)
                                        .get(subClassOwlClassExpression) > ConfigParams.typeOfObjectsInNegIndivsMinSize) {
                                    posTypeOwlSubClassExpressionsForCombination.add(subClassOwlClassExpression);
                                }
                            }
                        });
                        // recover memory
                        posTypeOwlSubClassExpressions = null;

                        // todo-zaman: way to improve: we can use the top performing negative types instead of the subclass of pisitive types
                        ArrayList<ArrayList<OWLClassExpression>> listCombinationOfSubClassesForNegPortion;
                        // combination of 1, starting from 1 for negtypes. posTypes are at-least 2 here.
                        listCombinationOfSubClassesForNegPortion = Utility
                                .combinationHelper(posTypeOwlSubClassExpressionsForCombination, 1);
                        // combination from 2 to upto ccombinationLimit
                        for (int combinationCounter = 2; combinationCounter <= ConfigParams.conceptLimitInNegExpr; combinationCounter++) {
                            // combination of combinationCounter
                            listCombinationOfSubClassesForNegPortion.
                                    addAll(Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, combinationCounter));
                        }
                        logger.debug("listCombinationOfSubClassesForNegPortion size: " + listCombinationOfSubClassesForNegPortion.size());

                        // keep only valid listCombinationOfSubClassesForNegPortion.
                        // a combination is valid if and only if it doesn't have self subClass.
                        // TODO: check with pascal. --- Okay
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
                            CandidateClassV2 CandidateClassV2 = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                            CandidateClassV2.addConjunctiveHornClauses(conjunctiveHornClauseV1V2);

                            // create candidate solution
                            CandidateSolutionV2 CandidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                            CandidateSolutionV2.addCandidateClass(CandidateClassV2);
                            boolean added = addToSolutions(CandidateSolutionV2);
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClauseV1V2.calculateAccuracyComplexCustom();
                                conjunctiveHornClauseV1V2.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1V2);

                                Score candidateClassScore = CandidateClassV2.calculateAccuracyComplexCustom();
                                CandidateClassV2.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, CandidateClassV2);
                            }
                        });
                    });
                });
        logger.info("solution using multiple positive and multiple negative type finished. Total Solutions: "
                + SharedDataHolder.CandidateSolutionSetV2.size());

        /**
         * Select top k5 hornClauses to make combination. This function reduces the hornClauseMap size.
         */
        sortAndFilterHornClauseMap(ConfigParams.hornClausesListMaxSize);

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
            //  TODO: check with pascal. -- Okay -- Pascal said okay, but it seems not okay really.
            ArrayList<ArrayList<ConjunctiveHornClauseV1V2>> validListCombinationOfHornClauses = new ArrayList<>();
            listCombinationOfHornClauses.forEach(classExpressions -> {
                if (isValidCombinationOfHornClauses(classExpressions)) {
                    validListCombinationOfHornClauses.add(classExpressions);
                }
            });
            logger.info("validListCombinationOfHornClauses size: " + validListCombinationOfHornClauses.size());

            validListCombinationOfHornClauses.forEach(conjunctiveHornClausesCombination -> {
                //create candidate class
                CandidateClassV2 candidateClass = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                candidateClass.setConjunctiveHornClauses(conjunctiveHornClausesCombination);

                // create candidate solution
                CandidateSolutionV2 CandidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                CandidateSolutionV2.addCandidateClass(candidateClass);
                boolean added = addToSolutions(CandidateSolutionV2);
                if (added) {
                    // save temporarily for combination
                    Score candidateClassScore = candidateClass.calculateAccuracyComplexCustom();
                    candidateClass.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                }
            });
            logger.info("\tcombination of horn clause using object property " +
                    Utility.getShortName(owlObjectProperty) + " finished. " +
                    "Total solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());

        });
        logger.info("solution using combination of horn clause finished. " +
                "Total solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());


        /**
         * Select top k6 CandidateClasses to make combination. This function reduces the candidate Classes size.
         */
        sortAndFilterCandidateClassMap(ConfigParams.candidateClassesListMaxSize);

        /**
         * combination of candidateClass/objectproperties. (upto K3/objPropsCombinationLimit limit)
         *
         */
        logger.info("solution using combination of object proeprties/candidateClass started...............");
        SharedDataHolder.objPropertiesCombination.forEach(owlObjectProperties -> {

            List<Collection<CandidateClassV2>> origList = new ArrayList<>();
            candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
                if (owlObjectProperties.contains(owlObjectProperty)) {
                    origList.add(candidateClasses);
                }
            });
            Collection<List<CandidateClassV2>> objPropsCombination = Utility.restrictedCombinationHelper(origList);

            //  Valid combination of ObjectProperties.
            //  TODO: check with pascal. -- Okay.
            objPropsCombination.forEach(candidateClasses -> {

                // create candidate solution
                CandidateSolutionV2 candidateSolution = new CandidateSolutionV2(reasoner, ontology);
                candidateSolution.setCandidateClasses(new ArrayList<>(candidateClasses));
                addToSolutions(candidateSolution);
            });
        });
        logger.info("solution using combination of object proeprties/candidateClass finished. " +
                "Total solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());
    }

    /**
     * extract all objects contains in the images and find their types.
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
                logger.info("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual.");
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

                logger.info("Below concepts are type/supertype of positive "
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
                logger.info("Below concepts are type/supertype of negative " + negIndiv.getIRI().toString() + " individual.");
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
                logger.info("Below concepts are type/supertype of negative " +
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
     * Remove common types which appeared both in positive types and negative types.
     *
     * @param owlObjectProperty
     */
    private void removeCommonTypesFromPosAndNeg(OWLObjectProperty owlObjectProperty) {

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

        // remove those and owl:Thing and owl:Nothing
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().removeAll(removeFromPosType);
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLThing());
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLNothing());
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
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

    /**
     * @param typeOfPosIndivs
     * @param typeOfNegIndivs
     */
    private void restoreBackup(HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> typeOfPosIndivs,
                               HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> typeOfNegIndivs) {
        logger.info("\nBefore restoring, SharedDataHolder.typeOfObjectsInPosIndivs.size(): " + SharedDataHolder.typeOfObjectsInPosIndivs.size());
        logger.info("\nBefore restoring, SharedDataHolder.typeOfObjectsInPosIndivs.size(): " + SharedDataHolder.typeOfObjectsInNegIndivs.size());

        // restore the backup versions to use in the negType replacement.
        SharedDataHolder.typeOfObjectsInPosIndivs = typeOfPosIndivs;
        SharedDataHolder.typeOfObjectsInNegIndivs = typeOfNegIndivs;
        logger.info("\nAfter restoring, SharedDataHolder.typeOfObjectsInPosIndivs.size(): " + SharedDataHolder.typeOfObjectsInPosIndivs.size());
        logger.info("\nAfter restoring, SharedDataHolder.typeOfObjectsInNegIndivs.size(): " + SharedDataHolder.typeOfObjectsInNegIndivs.size());
    }


    transient volatile protected int nrOfTotalIndividuals;
    transient volatile protected int nrOfPositiveIndividuals;
    transient volatile protected int nrOfNegativeIndividuals;


    /**
     * @param K6
     */
    public void calculateAccuracyOfTopK6ByReasoner(int K6) {
        if (SharedDataHolder.SortedCandidateSolutionListV2.size() < K6) {
            SharedDataHolder.SortedCandidateSolutionListV2.forEach(candidateSolution -> {
                candidateSolution.calculateAccuracyByReasoner();
            });
        } else {
            for (int i = 0; i < K6; i++) {
                SharedDataHolder.SortedCandidateSolutionListV2.get(i).calculateAccuracyByReasoner();
            }
        }
    }


    transient volatile private int o1Length = 0;
    transient volatile private int o2Length = 0;

    // temporary variables for using inside lambda
    transient volatile private List<ConjunctiveHornClauseV1V2> conjunctiveHornClausesList = new ArrayList<>();
    transient volatile private List<CandidateClassV2> candidateClassesList = new ArrayList<>();

    /**
     * Select top k5 hornCluases from the hornClausesMap.
     *
     * @param limit
     * @return
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> sortAndFilterHornClauseMap(int limit) {

        // make a list
        conjunctiveHornClausesList.clear();
        hornClausesMap.forEach((owlObjectProperty, conjunctiveHornClauses) -> {
            conjunctiveHornClausesList.addAll(conjunctiveHornClauses);
            logger.info("conjunctiveHornClauses size:  " + conjunctiveHornClauses.size());
        });

        if (conjunctiveHornClausesList.size() > 0) {

            // sort the list
            // todo(zaman): need to use the unified sorting algorithm, instead of this local one
            logger.info("horn clauses map  will be filtered, initial size: " + conjunctiveHornClausesList.size());
            conjunctiveHornClausesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length = 0;
                    o2Length = 0;

                    if (null != o1.getPosObjectTypes())
                        o1Length += o1.getPosObjectTypes().size();
                    if (null != o1.getNegObjectTypes()) {
                        o1Length += o1.getNegObjectTypes().size();
                    }
                    if (null != o2.getPosObjectTypes())
                        o2Length += o2.getPosObjectTypes().size();
                    if (null != o2.getNegObjectTypes()) {
                        o2Length += o2.getNegObjectTypes().size();
                    }
                    if (o1Length - o2Length > 0) {
                        return 1;
                    }
                    if (o1Length == o2Length) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // todo(zaman): there is significant error in coverage score of a conjunctivehornclause. for china vs syria experiment developedAsia(China) shows coverage score of 0.5, but it must be 1.0 -- fixed
            // test sorting
            logger.info("Score of first hornClause:  " + conjunctiveHornClausesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last hornClause:  " + conjunctiveHornClausesList.get(conjunctiveHornClausesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (conjunctiveHornClausesList.size() > limit + 1) {
                conjunctiveHornClausesList = conjunctiveHornClausesList.subList(0, limit + 1);
            }

            // make group again.
            hornClausesMap.clear();
            conjunctiveHornClausesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(hornClausesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (conjunctiveHornClausesList.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + conjunctiveHornClausesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!");
                monitor.stopSystem("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!", true);
            }
        } else {
            logger.info("No filtering done. hornClause map empty.");
        }
        return hornClausesMap;
    }

    /**
     * Select top k6 CandidateClassV0 from the candidateClassMap.
     *
     * @param limit
     * @return
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV2>> sortAndFilterCandidateClassMap(int limit) {
        // make a list
        candidateClassesList.clear();
        candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
            candidateClassesList.addAll(candidateClasses);
        });

        if (candidateClassesList.size() > 0) {
            // sort the list
            logger.info("candidate classes map  will be filtered. initial size: " + candidateClassesList.size());
            candidateClassesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length = 0;
                    o2Length = 0;

                    o1.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o1Length += conjunctiveHornClause.getPosObjectTypes().size();
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o1Length += conjunctiveHornClause.getNegObjectTypes().size();
                    });

                    o2.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o2Length += conjunctiveHornClause.getPosObjectTypes().size();
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o2Length += conjunctiveHornClause.getNegObjectTypes().size();
                    });

                    if (o1Length - o2Length > 0) {
                        return 1;
                    }
                    if (o1Length == o2Length) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // test sorting
            logger.info("Score of first candidate class:  " + candidateClassesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last candidate class:  " + candidateClassesList.get(candidateClassesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (candidateClassesList.size() > limit + 1) {
                candidateClassesList = candidateClassesList.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesList.size() <= limit + 1) {
                logger.info("candidate classes map filtered and now size: " + candidateClassesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!candidate classes map didn't filter perfectly. !!!!!!!!!!!!!");
                monitor.stopSystem("!!!!!!!!!!!!!candidate classes map didn't filter perfectly. !!!!!!!!!!!!!", true);
            }
        } else {
            logger.info("No filtering done. candidateClasses map empty");
        }

        return candidateClassesMap;
    }

    /**
     * Sort the solutions
     *
     * @param ascendingOfStringLength
     * @return
     */
    public boolean sortSolutionsCustom(boolean ascendingOfStringLength) {

        ArrayList<CandidateSolutionV2> solutionList = new ArrayList<>(
                SharedDataHolder.CandidateSolutionSetV2);

        solutionList.sort(new Comparator<CandidateSolutionV2>() {
            @Override
            public int compare(CandidateSolutionV2 o1, CandidateSolutionV2 o2) {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                }
                if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length = 0;
                    o2Length = 0;

                    //o1Length += o1.getAtomicPosOwlClasses().size();
                    o1.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o1Length += conjunctiveHornClause.getPosObjectTypes().size();
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o1Length += conjunctiveHornClause.getNegObjectTypes().size();
                        });
                    });
                    //o2Length += o2.getAtomicPosOwlClasses().size();
                    o2.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o2Length += conjunctiveHornClause.getPosObjectTypes().size();
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o2Length += conjunctiveHornClause.getNegObjectTypes().size();
                        });
                    });
                    // small to large of solution length
                    if (ascendingOfStringLength) {
                        if (o1Length - o2Length > 0) {
                            return 1;
                        }
                        if (o1Length == o2Length) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else {
                        if (o1Length - o2Length > 0) {
                            return -1;
                        }
                        if (o1Length == o2Length) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                } else {
                    return 1;
                }
            }
        });

        // save in shared data holder
        SharedDataHolder.SortedCandidateSolutionListV2 = solutionList;

        return true;
    }


    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("####################Solutions####################:");
        monitor.writeMessage("\n####################Solutions####################:");
        solutionCounter = 0;

        SharedDataHolder.SortedCandidateSolutionListV2.forEach((solution) -> {

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

        logger.info("Total solutions found using raw list: " + SharedDataHolder.SortedCandidateSolutionListV2.size());
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