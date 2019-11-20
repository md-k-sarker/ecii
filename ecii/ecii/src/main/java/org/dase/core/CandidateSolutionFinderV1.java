package org.dase.core;

import org.dase.datastructure.*;
import org.dase.exceptions.MalFormedIRIException;
import org.dase.ontofactory.DLSyntaxRendererExt;
import org.dase.util.ConfigParams;
import org.dase.util.Heuristics;
import org.dase.util.Monitor;
import org.dase.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.NodeSet;
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
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1>> hornClausesMap;

    /**
     * This is temporary hashSet used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV1>> candidateClassesMap;

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
            logger.debug("Extracting objectTypes using objectProperty: " + Utility.getShortName(entry.getKey()));
            extractObjectTypes(tolerance, entry.getKey());
        }
        logger.info("extractObjectTypes finished.");
        debugExtractObjectTypes();

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
     * @param candidateSolutionV1
     */
    private boolean addToSolutions(CandidateSolutionV1 candidateSolutionV1) {

        if (!SharedDataHolder.CandidateSolutionSetV1.contains(candidateSolutionV1)) {
            // calculate score
            Score accScore = calculateAccuracy(candidateSolutionV1);
            if (accScore.getCoverage() > 0) {
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
    private boolean isValidCombinationOfHornClauses(ArrayList<ConjunctiveHornClauseV1> aList) {
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
     * save the initial solutions into SharedDataHolder.CandidateSolutionSet object.
     */
    public void saveInitialSolutionsCustom() {

        // for rfilled types and for bare types. for no object property/direct/bare types we used SharedDataHolder.noneOWLObjProp

        // create solution using just one class expression.

        // solution using only a single positive type
        logger.info("solution using only a single positive type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                //create conjunctive horn clause and add positive part and no negative part initially
                ConjunctiveHornClauseV1 conjunctiveHornClauseV1 = new ConjunctiveHornClauseV1(owlObjectProperty);
                conjunctiveHornClauseV1.addPosObjectType(posOwlClassExpression);

                // create candidate class
                CandidateClassV1 candidateClassV1 = new CandidateClassV1(owlObjectProperty);
                candidateClassV1.addConjunctiveHornClauses(conjunctiveHornClauseV1);

                // create candidate solution
                CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1();
                candidateSolutionV1.addCandidateClass(candidateClassV1);
                boolean added = addToSolutions(candidateSolutionV1);
                if (added) {
                    // save temporarily for combination
                    Score hornClauseScore = calculateAccuracyComplexCustom(conjunctiveHornClauseV1);
                    conjunctiveHornClauseV1.setScore(hornClauseScore);
                    insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1);

                    Score candidateClassScore = calculateAccuracyComplexCustom(candidateClassV1);
                    candidateClassV1.setScore(candidateClassScore);
                    insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV1);
                }
            });
        });
        logger.info("solution using only a single positive type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        // should we use only negative type without a single positive type in Conjunctive Horn Clauses?
        // TODO(zaman): as Pascal said, hornclause must have at-least 1 positive type and possibly more? ecii-extension
        // so this solution need to be chnaged.
        // ref: https://en.wikipedia.org/wiki/Horn_clause
        // solution using only a single negative type. --- OKay
//        logger.info("solution using only a single negative type started...............");
//        SharedDataHolder.typeOfObjectsInNegIndivs.forEach((owlObjectProperty, hashMap) -> {
//            hashMap.forEach((negOwlClassExpression, integer) -> {
//
//                // create conjunctive horn clause and add negative part and no positive part initially
//                ConjunctiveHornClauseV1 conjunctiveHornClause = new ConjunctiveHornClauseV1(owlObjectProperty);
//                conjunctiveHornClause.addNegObjectType(negOwlClassExpression);
//
//                // create candidate class
//                CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty);
//                candidateClass.addConjunctiveHornClauses(conjunctiveHornClause);
//
//                // create candidate solution
//                CandidateSolutionV1 candidateSolution = new CandidateSolutionV1();
//                candidateSolution.addCandidateClass(candidateClass);
//                boolean added = addToSolutions(candidateSolution);
//                if (added) {
//                    // save temporarily for combination
//                    Score hornClauseScore = calculateAccuracyComplexCustom(conjunctiveHornClause);
//                    conjunctiveHornClause.setScore(hornClauseScore);
//                    insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);
//
//                    Score candidateClassScore = calculateAccuracyComplexCustom(candidateClass);
//                    candidateClass.setScore(candidateClassScore);
//                    insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
//                }
//            });
//        });
//        logger.info("solution using only a single negative type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

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
                            ConjunctiveHornClauseV1 conjunctiveHornClause = new ConjunctiveHornClauseV1(owlObjectProperty);
                            conjunctiveHornClause.addPosObjectType(posOwlClassExpression);
                            conjunctiveHornClause.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty);
                            candidateClass.addConjunctiveHornClauses(conjunctiveHornClause);

                            // create candidate solution
                            CandidateSolutionV1 candidateSolution = new CandidateSolutionV1();
                            candidateSolution.addCandidateClass(candidateClass);
                            boolean added = addToSolutions(candidateSolution);
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = calculateAccuracyComplexCustom(conjunctiveHornClause);
                                conjunctiveHornClause.setScore(hornClauseScore);
                                insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);

                                Score candidateClassScore = calculateAccuracyComplexCustom(candidateClass);
                                candidateClass.setScore(candidateClassScore);
                                insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                            }
                        }
                    }
                });
            });
        });
        logger.info("solution using only a single positive and single negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());
//
//        // single positive and multiple negative (upto K1 limit).
//        logger.info("solution using only a single positive and multiple negative type started...............");
//        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
//            hashMap.forEach((posOwlClassExpression, integer) -> {
//
//                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressions = new ArrayList<>(
//                        reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));
//
//                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressionsForCombination = new ArrayList<>();
//
//                // create combination only those which are contained in the negative type.
//                // TODO(zaman): Can we remove this restriction? Probably we can allow all concept without the superclasses of postype. reasoner.getsuperclasses()
//                posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {
//                    if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
//                        // if subclass of this class is included in the negative type
//                        if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)) {
//                            posTypeOwlSubClassExpressionsForCombination.add(subClassOwlClassExpression);
//                        }
//                    }
//                });
//                // recover memory
//                posTypeOwlSubClassExpressions = null;
//
//                ArrayList<ArrayList<OWLClassExpression>> listCombinationOfSubClassesForNegPortion;
//                // combination of 2
//                listCombinationOfSubClassesForNegPortion = Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, 2);
//                // combination from 3 to upto ccombinationLimit
//                for (int combinationCounter = 3; combinationCounter <= ConfigParams.conceptLimitInNegExpr; combinationCounter++) {
//                    // combination of combinationCounter
//                    listCombinationOfSubClassesForNegPortion.addAll(Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, combinationCounter));
//                }
//
//                // keep only valid listCombinationOfSubClassesForNegPortion.
//                // a combination is valid if and only if it doesn't have self subClass.
//                // TODO: check with pascal. --- Okay
//                ArrayList<ArrayList<OWLClassExpression>> validListCombinationOfSubClassesForNegPortion = new ArrayList<>();
//                listCombinationOfSubClassesForNegPortion.forEach(classExpressions -> {
//                    if (isValidCombinationOfSubClasses(classExpressions)) {
//                        validListCombinationOfSubClassesForNegPortion.add(classExpressions);
//                    }
//                });
//                // recover memory
//                listCombinationOfSubClassesForNegPortion = null;
//
//                validListCombinationOfSubClassesForNegPortion.forEach(subClasses -> {
//
//                    // if every class of this combination is in negative types then include this combination otherwise skip this.
//                    // this is trivially true as we are creating combination of those subclasses which are also contained in the negTypes.
//
//                    //create conjunctive horn clause and add positive part and negative part too
//                    ConjunctiveHornClauseV1 conjunctiveHornClause = new ConjunctiveHornClauseV1(owlObjectProperty);
//                    conjunctiveHornClause.setPosObjectType(posOwlClassExpression);
//                    conjunctiveHornClause.setNegObjectTypes(subClasses);
//
//                    // create candidate class
//                    CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty);
//                    candidateClass.addConjunctiveHornClauses(conjunctiveHornClause);
//
//                    // create candidate solution
//                    CandidateSolutionV1 candidateSolution = new CandidateSolutionV1();
//                    candidateSolution.addCandidateClass(candidateClass);
//                    boolean added = addToSolutions(candidateSolution);
//                    if (added) {
//                        // save temporarily for combination
//                        Score hornClauseScore = calculateAccuracyComplexCustom(conjunctiveHornClause);
//                        conjunctiveHornClause.setScore(hornClauseScore);
//                        insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);
//
//                        Score candidateClassScore = calculateAccuracyComplexCustom(candidateClass);
//                        candidateClass.setScore(candidateClassScore);
//                        insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
//                    }
//                });
//            });
//        });
//        logger.info("solution using only a single positive and multiple negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        // TODO(zaman): multiple positive and multiple negative. ecii-extension, need to implement this
        // multiple positive (upto K4 limit) and multiple negative (upto K1 limit).
        //
        logger.info("solution using multiple positive and multiple negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            ArrayList<OWLClassExpression> allPosTypes = new ArrayList<>(hashMap.keySet());

            ArrayList<ArrayList<OWLClassExpression>> listCombinationOfPosClassesForPosPortion;
            // making a list of 1 item means it will consist of single item, this is to reduce the code from uppper portions.
            listCombinationOfPosClassesForPosPortion = Utility.combinationHelper(allPosTypes, 2);
            // combination of 2 to the limit
            for (int combinationCounter = 3; combinationCounter < ConfigParams.conceptLimitInPosExpr; combinationCounter++) {
                listCombinationOfPosClassesForPosPortion.addAll(Utility.combinationHelper(allPosTypes, combinationCounter));
            }

            // keep only valid listCombinationOfSubClassesForNegPortion.
            // a combination is valid if and only if it doesn't have self subClass.
            // TODO: check with pascal. --- Okay
            ArrayList<ArrayList<OWLClassExpression>> validListCombinationOfPosClassesForPosPortion = new ArrayList<>();
            listCombinationOfPosClassesForPosPortion.forEach(classExpressions -> {
                if (isValidCombinationOfSubClasses(classExpressions)) {
                    validListCombinationOfPosClassesForPosPortion.add(classExpressions);
                }
            });
            // recover memory
            listCombinationOfPosClassesForPosPortion = null;

            validListCombinationOfPosClassesForPosPortion.forEach(posOwlClassExpressions -> {

                HashSet<OWLClassExpression> posTypeOwlSubClassExpressions = new HashSet<>();

                for (OWLClassExpression posOwlClassExpression : posOwlClassExpressions) {
                    posTypeOwlSubClassExpressions.addAll(reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));
                }

                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressionsForCombination = new ArrayList<>();

                // create combination only those which are contained in the negative type.
                // TODO(zaman): Can we remove this restriction? Probably we can allow all concept without the superclasses of postype. reasoner.getsuperclasses()
                posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {
                    if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                        // if subclass of this class is included in the negative type
                        if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)) {
                            posTypeOwlSubClassExpressionsForCombination.add(subClassOwlClassExpression);
                        }
                    }
                });
                // recover memory
                posTypeOwlSubClassExpressions = null;


                ArrayList<ArrayList<OWLClassExpression>> listCombinationOfSubClassesForNegPortion;
                // combination of 1
                listCombinationOfSubClassesForNegPortion = Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, 2);
                // combination from 2 to upto ccombinationLimit
                for (int combinationCounter = 3; combinationCounter <= ConfigParams.conceptLimitInNegExpr; combinationCounter++) {
                    // combination of combinationCounter
                    listCombinationOfSubClassesForNegPortion.addAll(Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, combinationCounter));
                }

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


                validListCombinationOfSubClassesForNegPortion.forEach(subClasses -> {

                    // if every class of this combination is in negative types then include this combination otherwise skip this.
                    // this is trivially true as we are creating combination of those subclasses which are also contained in the negTypes.

                    //create conjunctive horn clause and add positive part and negative part too
                    ConjunctiveHornClauseV1 conjunctiveHornClauseV1 = new ConjunctiveHornClauseV1(owlObjectProperty);
                    conjunctiveHornClauseV1.setPosObjectTypes(posOwlClassExpressions);
                    conjunctiveHornClauseV1.setNegObjectTypes(subClasses);

                    // create candidate class
                    CandidateClassV1 candidateClassV1 = new CandidateClassV1(owlObjectProperty);
                    candidateClassV1.addConjunctiveHornClauses(conjunctiveHornClauseV1);

                    // create candidate solution
                    CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1();
                    candidateSolutionV1.addCandidateClass(candidateClassV1);
                    boolean added = addToSolutions(candidateSolutionV1);
                    if (added) {
                        // save temporarily for combination
                        Score hornClauseScore = calculateAccuracyComplexCustom(conjunctiveHornClauseV1);
                        conjunctiveHornClauseV1.setScore(hornClauseScore);
                        insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV1);

                        Score candidateClassScore = calculateAccuracyComplexCustom(candidateClassV1);
                        candidateClassV1.setScore(candidateClassScore);
                        insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV1);
                    }
                });

            });
        });
        logger.info("solution using multiple positive and multiple negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

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
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " started...............");
            ArrayList<ConjunctiveHornClauseV1> hornClauseArrayList = new ArrayList<>(conjunctiveHornClauses);
            logger.info("\thorn clause size: " + hornClauseArrayList.size());

            ArrayList<ArrayList<ConjunctiveHornClauseV1>> listCombinationOfHornClauses;
            // combination of 2
            listCombinationOfHornClauses = Utility.combinationHelper(hornClauseArrayList, 2);
            // combination from 3 to upto ccombinationLimit
            for (int combinationCounter = 3; combinationCounter <= ConfigParams.hornClauseLimit; combinationCounter++) {
                // combination of combinationCounter
                listCombinationOfHornClauses.addAll(Utility.combinationHelper(hornClauseArrayList, combinationCounter));
            }

            //  Valid combination of hornClauses.
            //  TODO: check with pascal. -- Okay -- Pascal said okay, but it seems not okay really.
            ArrayList<ArrayList<ConjunctiveHornClauseV1>> validListCombinationOfHornClauses = new ArrayList<>();
            listCombinationOfHornClauses.forEach(classExpressions -> {
                if (isValidCombinationOfHornClauses(classExpressions)) {
                    validListCombinationOfHornClauses.add(classExpressions);
                }
            });


            validListCombinationOfHornClauses.forEach(conjunctiveHornClausesCombination -> {
                //create candidate class
                CandidateClassV1 candidateClass = new CandidateClassV1(owlObjectProperty);
                candidateClass.setConjunctiveHornClauses(conjunctiveHornClausesCombination);

                // create candidate solution
                CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1();
                candidateSolutionV1.addCandidateClass(candidateClass);
                boolean added = addToSolutions(candidateSolutionV1);
                if (added) {
                    // save temporarily for combination
                    Score candidateClassScore = calculateAccuracyComplexCustom(candidateClass);
                    candidateClass.setScore(candidateClassScore);
                    insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClass);
                }
            });
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());

        });
        logger.info("solution using combination of horn clause finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());


        /**
         * Select top k6 CandidateClasses to make combination. This function reduces the candidate Classes size.
         */
        sortAndFilterCandidateClassMap(ConfigParams.candidateClassesListMaxSize);

        /**
         * combination of candidateClass/objectproperties. (upto K3/objPropsCombinationLimit limit)
         *
         */
        logger.info("solution using combination of object proeprties started...............");
        SharedDataHolder.objPropertiesCombination.forEach(owlObjectProperties -> {

            List<Collection<CandidateClassV1>> origList = new ArrayList<>();
            candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
                if (owlObjectProperties.contains(owlObjectProperty)) {
                    origList.add(candidateClasses);
                }
            });
            Collection<List<CandidateClassV1>> objPropsCombination = Utility.restrictedCombinationHelper(origList);

            //  Valid combination of ObjectProperties.
            //  TODO: check with pascal. -- Okay.
            objPropsCombination.forEach(candidateClasses -> {

                // create candidate solution
                CandidateSolutionV1 candidateSolution = new CandidateSolutionV1();
                candidateSolution.setCandidateClasses(new ArrayList<>(candidateClasses));
                addToSolutions(candidateSolution);
            });
        });
        logger.info("solution using combination of object proeprties finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());
    }


    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to modify data of the:
     *  1. SharedDataHolder.objectsInPosIndivs or
     *  2. SharedDataHolder.objectsInNegIndivs or
     *  3. SharedDataHolder.typeOfObjectsInPosIndivs or
     *  4. SharedDataHolder.typeOfObjectsInNegIndivs
     * @param hashMap : HashMap<OWLObjectProperty, HashMap<T, Integer>>
     * @param objProp
     * @param data
     * @param <T>
     */
    //@formatter:on
    private <T> void modifyHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data) {
        if (hashMap.containsKey(objProp)) {
            if (hashMap.get(objProp).containsKey(data)) {
                hashMap.get(objProp).put(data, hashMap.get(objProp).get(data) + 1);
            } else {
                hashMap.get(objProp).put(data, 1);
            }
        } else {
            HashMap<T, Integer> tmpHashMap = new HashMap<>();
            tmpHashMap.put(data, 1);
            hashMap.put(objProp, tmpHashMap);
        }
    }


    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to enter data into:
     *  1. SharedDataHolder.objectsInPosIndivs or
     *  2. SharedDataHolder.objectsInNegIndivs or
     *  3. SharedDataHolder.typeOfObjectsInPosIndivs or
     *  4. SharedDataHolder.typeOfObjectsInNegIndivs
     * @param hashMap : HashMap<OWLObjectProperty, HashMap<T, Integer>>
     * @param objProp
     * @param data
     * @param <T>
     */
    //@formatter:on
    private <T> void insertIntoHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data) {
        if (hashMap.containsKey(objProp)) {
            if (hashMap.get(objProp).containsKey(data)) {
                hashMap.get(objProp).put(data, hashMap.get(objProp).get(data) + 1);
            } else {
                hashMap.get(objProp).put(data, 1);
            }
        } else {
            HashMap<T, Integer> tmpHashMap = new HashMap<>();
            tmpHashMap.put(data, 1);
            hashMap.put(objProp, tmpHashMap);
        }
    }


    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to insert data into:
     *  SharedDataHolder.individualHasObjectTypes
     * @param hashMap : HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>>
     * @param individual
     * @param owlObjectProperty
     * @param owlClassExpression
     */
    //@formatter:on
    private void insertIntoHashMap(HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>> hashMap,
                                   OWLNamedIndividual individual, OWLObjectProperty owlObjectProperty, OWLClassExpression owlClassExpression) {
        if (hashMap.containsKey(individual)) {
            if (hashMap.get(individual).containsKey(owlObjectProperty)) {
                hashMap.get(individual).get(owlObjectProperty).add(owlClassExpression);
            } else {
                HashSet<OWLClassExpression> tmpHashSet = new HashSet<>();
                tmpHashSet.add(owlClassExpression);
                hashMap.get(individual).put(owlObjectProperty, tmpHashSet);
            }
        } else {
            HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> tmpHashMap = new HashMap<>();
            HashSet<OWLClassExpression> tmpHashSet = new HashSet<>();
            tmpHashSet.add(owlClassExpression);
            tmpHashMap.put(owlObjectProperty, tmpHashSet);
            hashMap.put(individual, tmpHashMap);
        }
    }


    /**
     * Utility/Helper method to insert entry into HashMap
     * Used for coveredPosIndividualsMap/excludedNegIndividualsMap
     *
     * @param hashMap
     * @param data
     */
    private <T> void insertIntoHashMap(HashMap<T, Integer> hashMap, T data) {
        if (hashMap.containsKey(data)) {
            hashMap.put(data, hashMap.get(data) + 1);
        } else {
            hashMap.put(data, 1);
        }
    }


    /**
     * Utility/Helper method to insert entry into HashSet
     * Used for hornClauseHashMap
     *
     * @param hashMap
     * @param <T1>
     * @param <T2>
     */
    private <T1, T2> void insertIntoHashMap(HashMap<T1, HashSet<T2>> hashMap, T1 key, T2 data) {
        if (hashMap.containsKey(key)) {
            hashMap.get(key).add(data);
        } else {
            HashSet<T2> tmpHashSet = new HashSet<>();
            tmpHashSet.add(data);
            hashMap.put(key, tmpHashSet);
        }
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
        logger.info("Given obj property: " + Utility.getShortName(owlObjectProperty));


        // find the indivs and corresponding types of indivs which appeared in the positive images
        logger.info("size: " + SharedDataHolder.posIndivs.size());
        for (OWLNamedIndividual posIndiv : SharedDataHolder.posIndivs) {
            //bare type/direct type
            if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                //for no object property or direct types we used SharedDataHolder.noneOWLObjProp
                logger.info("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual.");
                logger.info("object count: " + reasoner.getTypes(posIndiv, false).getFlattened().size());
                reasoner.getTypes(posIndiv, false).getFlattened().forEach(posType -> {
                    logger.info("posType: " + posType.toString());
                    if (!posType.equals(owlDataFactory.getOWLThing()) && !posType.equals(owlDataFactory.getOWLNothing())) {
                        // insert into individualObject's type count
                        insertIntoHashMap(SharedDataHolder.typeOfObjectsInPosIndivs, owlObjectProperty, posType);

                        //insert into individualObject to individualObject type mapping
                        insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, posIndiv, owlObjectProperty, posType);
                    }
                });
            } else {

                logger.info("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI().getShortForm());
                logger.info("object count: " + reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened().size());
                reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened().forEach(eachIndi -> {
                    logger.debug("\tindi: " + eachIndi.getIRI());

                    // insert into individuals count
                    insertIntoHashMap(SharedDataHolder.objectsInPosIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(posType -> {
                        logger.info("posType: " + posType.toString());
                        if (!posType.equals(owlDataFactory.getOWLThing()) && !posType.equals(owlDataFactory.getOWLNothing())) {
                            // insert into individualObject's type count
                            insertIntoHashMap(SharedDataHolder.typeOfObjectsInPosIndivs, owlObjectProperty, posType);

                            //insert into individualObject to individualObject type mapping
                            insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, posIndiv, owlObjectProperty, posType);
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
                logger.info("object count: " + reasoner.getTypes(negIndiv, false).getFlattened().size());
                reasoner.getTypes(negIndiv, false).getFlattened().forEach(negType -> {
                    logger.info("negType: " + negType.toString());
                    if (!negType.equals(owlDataFactory.getOWLThing()) && !negType.equals(owlDataFactory.getOWLNothing())) {
                        // insert into individualObject's type count
                        insertIntoHashMap(SharedDataHolder.typeOfObjectsInNegIndivs, owlObjectProperty, negType);

                        //insert into individualObject to individualObject type mapping
                        insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, negIndiv, owlObjectProperty, negType);
                    }
                });
            } else {
                logger.info("Below concepts are type/supertype of negative " + negIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI().getShortForm());
                logger.info("object count: " + reasoner.getObjectPropertyValues(negIndiv, owlObjectProperty).getFlattened().size());
                reasoner.getObjectPropertyValues(negIndiv, owlObjectProperty).getFlattened().forEach(eachIndi -> {

                    // insert into individualObject count
                    insertIntoHashMap(SharedDataHolder.objectsInNegIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(negType -> {
                        logger.info("negType: " + negType.toString());
                        if (!negType.equals(owlDataFactory.getOWLThing()) && !negType.equals(owlDataFactory.getOWLNothing())) {
                            //insert into individualObject's type count
                            insertIntoHashMap(SharedDataHolder.typeOfObjectsInNegIndivs, owlObjectProperty, negType);

                            // individualObject to individualObject type mapping
                            insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, negIndiv, owlObjectProperty, negType);
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
                        double posRatio = SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).get(owlClassExpr) / SharedDataHolder.posIndivs.size();
                        double negRatio = SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).get(owlClassExpr) / SharedDataHolder.negIndivs.size();

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
        ArrayList<ArrayList<OWLObjectProperty>> listCombination;
        // combination of 2
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

    // use double to ensure when dividing we are getting double result not integer.
    transient volatile protected double nrOfPositiveClassifiedAsPositive;
    /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
    transient volatile protected double nrOfPositiveClassifiedAsNegative;
    transient volatile protected double nrOfNegativeClassifiedAsNegative;
    /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
    transient volatile protected double nrOfNegativeClassifiedAsPositive;


    /**
     * Determine whether this owlnamedIndividual contained within the candidate classes of this array of candidateClasses.
     * precondition: all object property of this candidateclasses must be same.
     * for none
     *
     * @param candidateClassesV1
     * @param owlNamedIndividual
     * @return boolean
     */
    private boolean isContainedInCandidateClasses(ArrayList<CandidateClassV1> candidateClassesV1, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {
        boolean contained = false;

        OWLObjectProperty owlObjectProperty = candidateClassesV1.get(0).getOwlObjectProperty();

        // for none type object properties it must be contained in each of the candidate classes.
        int containedInTotalCandidateClasses = 0;

        HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                individualHasObjectTypes.get(owlNamedIndividual);

        if (null != objPropsMap && objPropsMap.containsKey(owlObjectProperty)) {
            if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                // todo(zaman): need to implement logic.
                //  if **all** candidate class of this group contains this individual then the full group contains this individual.
                for (CandidateClassV1 candidateClass : candidateClassesV1) {
                    if (owlObjectProperty.equals(candidateClass.getOwlObjectProperty())) {
                        if (candidateClass.getConjunctiveHornClauses().size() > 0) {
                            if (isContainedInCandidateClass(candidateClass, owlNamedIndividual, isPosIndiv)) {
                                containedInTotalCandidateClasses++;
                            }
                        }
                    }
                }
                if (candidateClassesV1.size() == containedInTotalCandidateClasses) {
                    contained = true;
                    return contained;
                }
            } else {
                // if **any** candidate class of this group contains this individual then the full group contains this individual.
                for (CandidateClassV1 candidateClass : candidateClassesV1) {
                    if (owlObjectProperty.equals(candidateClass.getOwlObjectProperty())) {
                        if (candidateClass.getConjunctiveHornClauses().size() > 0) {
                            if (isContainedInCandidateClass(candidateClass, owlNamedIndividual, isPosIndiv)) {
                                contained = true;
                                return contained;
                            }
                        }
                    }
                }
            }
        }
        return contained;
    }

    /**
     * Determine whether this owlnamedIndividual contained within this candidate class.
     * This means that,
     * For, none owlObjectProperty,
     * if **all** of the horn clauses (connected to this cnadidate class) contain this individual then this candidate class contain this individual.
     * For, proper owlObjectProperty,
     * if **any** one horn clause of the horn clauses (connected to this cnadidate class) contain this individual then this candidate class contain this individual.
     *
     * @param candidateClassV1
     * @param owlNamedIndividual
     * @return
     */
    private boolean isContainedInCandidateClass(CandidateClassV1 candidateClassV1, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {
        return isContainedInHornClauses(candidateClassV1.getConjunctiveHornClauses(), owlNamedIndividual, isPosIndiv);
    }


    /**
     * Determine whether this owlnamedIndividual contained within the hornclause of this hornClauses.
     * This means that,
     * For, none owlObjectProperty,
     * if **all** of the horn clauses contain this individual then this individual is covered by this list.
     * For, proper owlObjectProperty,
     * if **any** one horn clause of the horn clauses contain this individual then this individual is covered by this list.
     *
     * @param hornClauses
     * @param owlNamedIndividual
     * @return
     */
    private boolean isContainedInHornClauses(ArrayList<ConjunctiveHornClauseV1> hornClauses, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {

        boolean contained = false;

        // all horclause have same objectProperty, so it doesn't matter which one we take. we can obvioulsy make sure here.
        OWLObjectProperty owlObjectProperty = hornClauses.get(0).getOwlObjectProperty();

        HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                individualHasObjectTypes.get(owlNamedIndividual);

        if (null != objPropsMap && objPropsMap.containsKey(owlObjectProperty)) {
            if (isPosIndiv) {
                if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                    // for postypes and none object property: if all horn clause of this group contains this individual then the full arraylist<hornclauses> contains this individual.
                    int includedCounter = 0;
                    for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                        if (owlObjectProperty.equals(hornClause.getOwlObjectProperty())) {
                            if (hornClause.isContainedInHornClause(owlNamedIndividual, isPosIndiv)) {
                                includedCounter++;
                            }
                        }
                    }
                    if (hornClauses.size() == includedCounter) {
                        contained = true;
                    }
                } else {
                    // for postypes and proper object property: if any horn clause of this group contains this individual then the full arraylist<hornclauses> contains this individual.
                    for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                        if (owlObjectProperty.equals(hornClause.getOwlObjectProperty())) {
                            if (hornClause.isContainedInHornClause( owlNamedIndividual, isPosIndiv)) {
                                contained = true;
                                break;
                            }
                        }
                    }
                }
            } else {
                if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                    // for negtypes and proper object property: it just need to be excluded by **any** part , as the hornclauses come like this: C1 ⊓ ... ⊓ Cn
                    for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                        if (owlObjectProperty.equals(hornClause.getOwlObjectProperty())) {
                            if (hornClause.isContainedInHornClause( owlNamedIndividual, isPosIndiv)) {
                                contained = true;
                                break;
                            }
                        }
                    }
                } else {
                    // for negtypes and proper object property: it must be excluded by each part , as the hornclauses come like this: R1(C1 ⊔ C2 ⊔ C3 ⊔ C4 ⊔ C5 ⊔ C6 ….)
                    int excludedCounter = 0;
                    for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                        if (owlObjectProperty.equals(hornClause.getOwlObjectProperty())) {
                            if (hornClause.isContainedInHornClause( owlNamedIndividual, isPosIndiv)) {
                                excludedCounter++;
                            }
                        }
                    }
                    if (hornClauses.size() == excludedCounter) {
                        contained = true;
                    } else {
                        // this means, this individual is included in both pos portion and neg portion of this list of conjunctive hornClauses, so our solution should be excluded, or need another method to calculates this.
                        logger.info("this means, this individual is included in both pos portion and neg portion of this list of conjunctive hornClauses, " +
                                "so our solution should be excluded, or need another method to calculates this.. CandidateSolutionFinderV1.isContainedInHornClauses()");
                        logger.info("\tindividual: " + Utility.getShortName(owlNamedIndividual));
                        for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                            logger.info("\t\thornClause: " + hornClause.getHornClauseAsString());
                        }
                        monitor.writeMessage("this means, this individual is included in both pos portion and neg portion of this list of conjunctive hornClauses," +
                                " so our solution should be excluded, or need another method to calculates this.. CandidateSolutionFinderV1.isContainedInHornClauses()");
                        monitor.writeMessage("\tindividual: " + Utility.getShortName(owlNamedIndividual));
                        for (ConjunctiveHornClauseV1 hornClause : hornClauses) {
                            monitor.writeMessage("\t\thornClause: " + hornClause.getHornClauseAsString());
                        }
                    }
                }
            }
        }

        return contained;
    }

//    /**
//     * Determine whether this owlnamedIndividual contained within  this hornclause.
//     * Our v1 hornclause is of this formula: B1 ⊓ B2 ⊓ B3 …. ⊓  ¬(D1 ⊔...⊔Djk))
//     * So, to satisfy, this individual must be in
//     * 1. all posTypes and
//     * 2. not on the negativeSide.
//     * verified/unit tested for single posType without negTypes -- this function is totally okay.
//     * @param hornClause
//     * @param owlNamedIndividual
//     * @return
//     */
//    private boolean isContainedInHornClause(ConjunctiveHornClauseV1 hornClause, OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {
//
//        boolean contained = false;
//
//        // if an individual exists in both pos part and neg part then it is not a valid conjunctive horn clause.
//        // TODO(Zaman): need to verify our candidate solution.
//        // when this condition is meeting we are still saying contained=false.
//
//        if (hornClause != null && owlNamedIndividual != null) {
//            if (SharedDataHolder.individualHasObjectTypes.containsKey(owlNamedIndividual)) {
//                HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
//                        individualHasObjectTypes.get(owlNamedIndividual);
//
//                if (objPropsMap.containsKey(hornClause.getOwlObjectProperty())) {
//
//                    if (isPosIndiv) {
//                        // is in positive side  and not in negative side
//                        if (null != hornClause.getPosObjectTypes()) {
//                            // must be in allpostypes
//                            int containedInPosTypeCounter = 0;
//                            for (OWLClassExpression posType : hornClause.getPosObjectTypes()) {
//                                if (objPropsMap.get(hornClause.getOwlObjectProperty()).contains(posType)) {
//                                    containedInPosTypeCounter++;
//                                }
//                            }
//                            if (hornClause.getPosObjectTypes().size() == containedInPosTypeCounter) {
//                                // make sure it is also not caintained in the negative portions
//                                if (!isContainedInAnyClassExpressions(hornClause.getNegObjectTypes(), owlNamedIndividual, hornClause.getOwlObjectProperty())) {
//                                    contained = true;
//                                }
//                            }
//                        } else {
//                            // it dont have positive. so if it is excluded by negative then it is covered. TODO: check
//                        }
//                    } else {
//                        // negindivs : is in negative side and not in positive side
//                        // if any one of the negtypes contained this type then it is contained within the negTypes.
//                        boolean containedInNegPortion = false;
//                        for (OWLClassExpression negType : hornClause.getNegObjectTypes()) {
//                            if (objPropsMap.get(hornClause.getOwlObjectProperty()).contains(negType)) {
//                                //totalSolPartsInThisGroupCounter++;
//                                containedInNegPortion = true;
//                                break;
//                            }
//                        }
//
//                        if (containedInNegPortion) {
//                            // need to make sure it is not in the posPortion.
//                            int containedInPosTypeCounter = 0;
//                            for (OWLClassExpression posType : hornClause.getPosObjectTypes()) {
//                                if (objPropsMap.get(hornClause.getOwlObjectProperty()).contains(posType)) {
//                                    containedInPosTypeCounter++;
//                                }
//                            }
//                            // some postype may cover this individual but at-least 1 postype need to exclude this neg individual.
//                            if (hornClause.getPosObjectTypes().size() > containedInPosTypeCounter) {
//                                contained = true;
//                            } else {
//                                // TODO(Zaman): if individual contained in both negative portion and in Positive portion then, actually we should exclude this solution.
//                                logger.info("individual contained in both negative portion and in Positive portion, so we should exclude this solution.");
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        if (isPosIndiv) {
//            logger.info("PosIndiv " + Utility.getShortName(owlNamedIndividual) + " is contained in hornClause " + hornClause.getHornClauseAsString() + ": " + contained);
//        } else {
//            logger.info("NegIndiv " + Utility.getShortName(owlNamedIndividual) + " is contained in hornClause " + hornClause.getHornClauseAsString() + ": " + contained);
//        }
//        return contained;
//    }
//
//


    /**
     * Calculate accuracy of a hornClause.
     * TODO(zaman): need to fix to make compatible with v1
     *
     * @param conjunctiveHornClauseV1
     * @return
     */
    private Score calculateAccuracyComplexCustom(ConjunctiveHornClauseV1 conjunctiveHornClauseV1) {

        /**
         * Individuals covered by this hornClause
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by this hornClause
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        /**
         * For positive individuals, a individual must be contained within each AND section to be added as a coveredIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {

            if (conjunctiveHornClauseV1.isContainedInHornClause( thisOwlNamedIndividual, true)) {
                insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within any single section to be added as a excludedIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            if (conjunctiveHornClauseV1.isContainedInHornClause( thisOwlNamedIndividual, false)) {
                insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
            }
        }

        // todo(zaman): there is severe problem. when we dont have negPortion in the hornClause then any negative is not being covered by that portion, need to fix it. so essentially this function isContainedInHornClause()
        //  need to be fixed. for example hornClause developedAsia is not excluding Syria as it is not being excluded by any negPortion.!!!!!!
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
     * Calculate accuracy of a candidateClass.
     *
     * @param candidateClassV1
     * @return
     */
    private Score calculateAccuracyComplexCustom(CandidateClassV1 candidateClassV1) {

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

            if (isContainedInCandidateClass(candidateClassV1, thisOwlNamedIndividual, true)) {
                insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within any single section to be added as a excludedIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            if (isContainedInCandidateClass(candidateClassV1, thisOwlNamedIndividual, false)) {
                insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
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
     * TODO(Zaman) : need to fix the accuracy calculation for v1
     *
     * @param candidateSolutionV1
     * @return
     */
    private Score calculateAccuracy(CandidateSolutionV1 candidateSolutionV1) {

        HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> groupedCandidateClasses = candidateSolutionV1.getGroupedCandidateClasses();

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

            for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> singleGroupOfCandidateClasses : groupedCandidateClasses.entrySet()) {

                ArrayList<CandidateClassV1> candidateClassesV1 = singleGroupOfCandidateClasses.getValue();
                if (candidateClassesV1.size() > 0) {
                    // if owlObjectProperty is none type then candidate classes are conjuncted
                    // if owlObjectProperty is proper type then candidate classes are unioned.
                    // this logic is handled in function isContainedInCandidateClasses(...), so we don't need to handle it here.

                    if (!isContainedInCandidateClasses(candidateClassesV1, thisOwlNamedIndividual, true)) {
                        // this individual is not contained in this arraylist of candidate classes.
                        // so this individual is not covered.
                        // we need to start iterating with next individual
                        continue nextPosIndivIter;
                    } else {
                        containedInTotalGroups++;
                    }
//                    OWLObjectProperty owlObjectProperty = singleGroupOfCandidateClasses.getKey();
//                    if (owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
//                        // todo(zaman): need to implement logic
//                    } else {
//
//                    }
                }
            }
            if (containedInTotalGroups == groupedCandidateClasses.size()) {
                insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within each AND section to be added as a excludedInvdividuals.
         * TODO(zaman): fix-logic: it seems if an individual is excluded by any sections of the AND then is will be excluded by the whole solution.
         * I.e. each
         */
        nextNegIndivIter:
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

//            int containedInTotalGroups = 0;

            for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> entry : groupedCandidateClasses.entrySet()) {
                // each group will be concatenated by AND.
                OWLObjectProperty owlObjectProperty = entry.getKey();
                // not passing object property here, because we can recover object property from candidate class
                ArrayList<CandidateClassV1> candidateClasses = entry.getValue();
                if (candidateClasses.size() > 0) {
                    if (!isContainedInCandidateClasses(candidateClasses, thisOwlNamedIndividual, false)) {
                        // this individual is not contained in this arraylist of candidate classes.
                        // so this individual is not covered.
                        // we need to start iterating with next individual
                        insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
                        continue nextNegIndivIter;
                    } else {
//                        containedInTotalGroups++;
                    }
                }
            }
//            if (containedInTotalGroups == groupedCandidateClasses.size()) {
//                insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
//            }
        }

        // TODO(zaman): it should be logger.debug instead of logger.info
        logger.info("solution: " + candidateSolutionV1.getSolutionAsString());
        logger.info("coveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.info("coveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.info("excludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.info("excludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());

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


    private int newIRICounter = 0;

    private IRI getUniqueIRI() {
        ++newIRICounter;
        String str = ":_Dracula__Dragon_Z" + newIRICounter;
        try {

            return Utility.createEntityIRI(str);
        } catch (MalFormedIRIException ex) {
            return IRI.create(str);
        }

    }

    /**
     * Calculate accuracy of a solution.
     *
     * @param candidateSolution
     * @return
     */
    private void calculateAccuracyByReasoner(CandidateSolutionV1 candidateSolution) {

        // add this class expression to ontology and reinitiate reasoner.
        OWLClassExpression owlClassExpression = candidateSolution.getSolutionAsOWLClassExpression();
        // create a unique name
        OWLClass owlClass = SharedDataHolder.owlDataFactory.getOWLClass(getUniqueIRI());
        OWLAxiom eqAxiom = SharedDataHolder.owlDataFactory.getOWLEquivalentClassesAxiom(owlClass, owlClassExpression);
        ChangeApplied ca = SharedDataHolder.owlOntologyManager.addAxiom(SharedDataHolder.owlOntology, eqAxiom);
        logger.info("Adding candidateSolution.getSolutionAsOWLClassExpression to ontology Status: " + ca.toString());
        reasoner = Utility.initReasoner(ConfigParams.reasonerName, SharedDataHolder.owlOntology, monitor);

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
                insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
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
                insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
                logger.info("\t" + Utility.getShortName(thisOwlNamedIndividual) + " is contained by this concept using reasoner: " + owlClassExpression);
            } else {
//                logger.info("not found. size: " + negIndivsByReasoner.size());
            }
        }

        logger.info("solution: " + candidateSolution.getSolutionAsString());
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
        candidateSolution.getScore().setPrecision_by_reasoner(precision);
        candidateSolution.getScore().setRecall_by_reasoner(recall);
        candidateSolution.getScore().setF_measure_by_reasoner(f_measure);
        candidateSolution.getScore().setCoverage_by_reasoner(coverage);


    }

    /**
     * @param K6
     */
    public void calculateAccuracyOfTopK6ByReasoner(int K6) {
        if (SharedDataHolder.SortedCandidateSolutionSetV1.size() < K6) {
            SharedDataHolder.SortedCandidateSolutionSetV1.forEach(candidateSolution -> {
                calculateAccuracyByReasoner(candidateSolution);
            });
        } else {
            for (int i = 0; i < K6; i++) {
                calculateAccuracyByReasoner(SharedDataHolder.SortedCandidateSolutionSetV1.get(i));
            }
        }
    }


    transient volatile private int o1Length = 0;
    transient volatile private int o2Length = 0;

    // temporary variables for using inside lambda
    transient volatile private List<ConjunctiveHornClauseV1> conjunctiveHornClausesList = new ArrayList<>();
    transient volatile private List<CandidateClassV1> candidateClassesList = new ArrayList<>();

    /**
     * Select top k5 hornCluases from the hornClausesMap.
     *
     * @param limit
     * @return
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1>> sortAndFilterHornClauseMap(int limit) {

        // make a list
        conjunctiveHornClausesList.clear();
        hornClausesMap.forEach((owlObjectProperty, conjunctiveHornClauses) -> {
            conjunctiveHornClausesList.addAll(conjunctiveHornClauses);
            logger.info("conjunctiveHornClauses size:  " + conjunctiveHornClauses.size());
        });

        if (conjunctiveHornClausesList.size() > 0) {

            // sort the list
            logger.info("horn clauses map  will be filtered initial size: " + conjunctiveHornClausesList.size());
            conjunctiveHornClausesList.sort((o1, o2) -> {
                if (o1.getScore().getCoverage() - o2.getScore().getCoverage() > 0) {
                    return -1;
                } else if (o1.getScore().getCoverage() == o2.getScore().getCoverage()) {
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

            // todo(zaman): there is significant error in coverage score of a conjunctivehornclause. for china vs syria experiment developedAsia(China) shows coverage score of 0.5, but it must be 1.0
            // test sorting
            logger.info("Score of first hornClause:  " + conjunctiveHornClausesList.get(0).getScore().getCoverage());
            logger.info("Score of last hornClause:  " + conjunctiveHornClausesList.get(conjunctiveHornClausesList.size() - 1).getScore().getCoverage());

            // filter/select top n (upto limit)
            if (conjunctiveHornClausesList.size() > limit + 1) {
                conjunctiveHornClausesList = conjunctiveHornClausesList.subList(0, limit + 1);
            }

            // make group again.
            hornClausesMap.clear();
            conjunctiveHornClausesList.forEach(conjunctiveHornClause -> {
                insertIntoHashMap(hornClausesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
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
     * Select top k6 CandidateClass from the candidateClassMap.
     *
     * @param limit
     * @return
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV1>> sortAndFilterCandidateClassMap(int limit) {
        // make a list
        candidateClassesList.clear();
        candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
            candidateClassesList.addAll(candidateClasses);
        });

        if (candidateClassesList.size() > 0) {
            // sort the list
            logger.info("horn clauses map  will be filtered initial size: " + candidateClassesList.size());
            candidateClassesList.sort((o1, o2) -> {
                if (o1.getScore().getCoverage() - o2.getScore().getCoverage() > 0) {
                    return -1;
                } else if (o1.getScore().getCoverage() == o2.getScore().getCoverage()) {
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
                            o1Length += conjunctiveHornClause.getPosObjectTypes().size();
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
            logger.info("Score of first candidate class:  " + candidateClassesList.get(0).getScore().getCoverage());
            logger.info("Score of last candidate class:  " + candidateClassesList.get(candidateClassesList.size() - 1).getScore().getCoverage());

            // filter/select top n (upto limit)
            if (candidateClassesList.size() > limit + 1) {
                candidateClassesList = candidateClassesList.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesList.forEach(conjunctiveHornClause -> {
                insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesList.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + candidateClassesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!");
                monitor.stopSystem("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!", true);
            }
        } else {
            logger.info("No filtering done. candidateClasses map empty");
        }


        return candidateClassesMap;
    }

    /**
     * Sort the solutions
     *
     * @param ascending
     * @return
     */
    public boolean sortSolutionsCustom(boolean ascending) {

        ArrayList<CandidateSolutionV1> solutionList = new ArrayList<>(
                SharedDataHolder.CandidateSolutionSetV1);

        // small to large
        if (ascending) {
            solutionList.sort(new Comparator<CandidateSolutionV1>() {
                @Override
                public int compare(CandidateSolutionV1 o1, CandidateSolutionV1 o2) {
                    if (o1.getScore().getCoverage() - o2.getScore().getCoverage() > 0) {
                        return 1;
                    }
                    if (o1.getScore().getCoverage() == o2.getScore().getCoverage()) {
                        // compare length, shorter length will be chosen first
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
                        if (o1Length - o2Length > 0) {
                            return -1;
                        }
                        if (o1Length == o2Length) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                }
            });
        } else {
            solutionList.sort(new Comparator<CandidateSolutionV1>() {
                @Override
                public int compare(CandidateSolutionV1 o1, CandidateSolutionV1 o2) {
                    if (o1.getScore().getCoverage() - o2.getScore().getCoverage() > 0) {
                        return -1;
                    }
                    if (o1.getScore().getCoverage() == o2.getScore().getCoverage()) {
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
                }
            });
        }

        // save in shared data holder
        SharedDataHolder.SortedCandidateSolutionSetV1 = solutionList;

        return true;
    }


    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("####################Solutions####################:");
        monitor.writeMessage("\n####################Solutions####################:");
        solutionCounter = 0;

        SharedDataHolder.SortedCandidateSolutionSetV1.forEach((solution) -> {

            if (solution.getGroupedCandidateClasses().size() > 0) {
                solutionCounter++;

                String solutionAsString = solution.getSolutionAsString();

                if (solutionAsString.length() > 0 && null != solution.getScore()) {
                    //logger.info("solution " + solutionCounter + ": " + solutionAsString);
                    monitor.writeMessage("solution " + solutionCounter + ": " + solutionAsString);
                    DLSyntaxRendererExt dlRenderer = new DLSyntaxRendererExt();
                    monitor.writeMessage("\tsolution pretty-printed by reasoner: " + dlRenderer.render(solution.getSolutionAsOWLClassExpression()));

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

        logger.info("Total solutions found using raw list: " + SharedDataHolder.SortedCandidateSolutionSetV1.size());
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