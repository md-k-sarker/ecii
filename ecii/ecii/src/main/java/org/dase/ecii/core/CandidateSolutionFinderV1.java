package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV1;
import org.dase.ecii.datastructure.CandidateSolutionV1;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV1V2;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
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
 * Algorithm version: V1
 *
 */
public class CandidateSolutionFinderV1 extends CandidateSolutionFinder {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This is temporary hashMap used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> hornClausesMap;

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
        super(_reasoner, _ontology, _printStream, _monitor);
        this.hornClausesMap = new HashMap<>();
        this.candidateClassesMap = new HashMap<>();
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
     * save the initial solutions into SharedDataHolder.candidateSolutionV0Set object.
     */
    public void createAndSaveSolutions() {

        // for rfilled types and for bare types. for no object property/direct/bare types we used SharedDataHolder.noneOWLObjProp

        // create solution using just one class expression.
        // solution using only a single positive type
        createSolutionUsingSinglePosTypes();

        // create solution using both positive and negative of class expressions.
        // single positive and single negative.
        createSolutionUsingSinglePosAndNegTypes();

        // multiple positive and multiple negative.
        createSolutionUsingMultiplePosAndNegTypes();

        // create solution by combining hornClause
        createSolutionByCombiningHornClause();

        // create solution by combining candidateClass
        createSolutionByCombiningCandidateClass();
    }

    /**
     * Create solutions using single positive types.
     * Positive types include direct positive types and indirect positive types
     */
    private void createSolutionUsingSinglePosTypes() {
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
    }

    /**
     * Create solutions using single positive types and single negative types.
     */
    private void createSolutionUsingSinglePosAndNegTypes() {
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
    }

    /**
     * Create solutions using multiple positive types and multiple negative types.
     */
    private void createSolutionUsingMultiplePosAndNegTypes() {
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
    }

    /**
     * Create solutions using the combination of hornClauses.
     * This function at first select the top K5 hornClauses and,
     * make combination of them to produce solutions
     */
    private void createSolutionByCombiningHornClause() {
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
    }

    /**
     * Create solutions using the combination of candidateClasses.
     * This function at first select the top K6 candidateClass and,
     * make combination of them to produce solutions
     */
    private void createSolutionByCombiningCandidateClass() {
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
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("\n####################Solutions (sorted by " + Score.defaultScoreType + ")####################:");
        monitor.writeMessage("\n####################Solutions (sorted by " + Score.defaultScoreType + ")####################:");
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