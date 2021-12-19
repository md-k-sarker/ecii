package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV2;
import org.dase.ecii.datastructure.CandidateSolutionV2;
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
 * Algorithm version: V2
 *
 */
public class CandidateSolutionFinderV2 extends CandidateSolutionFinder {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        super(_reasoner, _ontology, _printStream, _monitor);
        this.hornClausesMap = new HashMap<>();
        this.candidateClassesMap = new HashMap<>();
    }

    /**
     * Utility/Helper method to add solution to solutionsSet.
     *
     * @param candidateSolutionV2
     */
    private boolean addToSolutions(CandidateSolutionV2 candidateSolutionV2) {

        if (!SharedDataHolder.CandidateSolutionSetV2.contains(candidateSolutionV2)) {
            // calculate score
            Score accScore = candidateSolutionV2.calculateAccuracyComplexCustom();
            if (accScore.getDefaultScoreValue() > 0) {
                candidateSolutionV2.setScore(accScore);
                // save to shared data holder
                SharedDataHolder.CandidateSolutionSetV2.add(candidateSolutionV2);
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * save the initial solutions into SharedDataHolder.candidateSolutionV2Set object.
     */
    public void createAndSaveSolutions() {

        // for rfilled types and for bare types. for no object property/direct/bare types we used SharedDataHolder.noneOWLObjProp

        // solution using only a single positive type
        createSolutionUsingSinglePosTypes();

        // create solution using both positive and negative of class expressions.
        // single positive and single negative.
        createSolutionUsingSinglePosAndNegTypes();

        // multiple positive and multiple negative.
        createSolutionUsingMultiplePosAndNegTypes();

        // create solution by combining hornClause
        if (ConfigParams.hornClauseLimit > 1)
            createSolutionByCombiningHornClause();

        // create solution by combining candidateClass
        if (ConfigParams.objPropsCombinationLimit > 1)
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
        logger.info("solution using only a single positive type finished. Total solutions: " +
                SharedDataHolder.CandidateSolutionSetV2.size());
    }

    /**
     * Create solutions using single positive types and single negative types.
     */
    private void createSolutionUsingSinglePosAndNegTypes() {
        logger.info("\nSolution using only a single positive and single negative type started...............");


        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {

            hashMap.forEach((posOwlClassExpression, integer) -> {
                logger.debug("posOwlClassExpression: " + posOwlClassExpression);
                // take subclasses
                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressions = new ArrayList<>(
                        reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));

                posTypeOwlSubClassExpressions.forEach(subClassOwlClassExpression -> {
                    if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                        // if subclass of this class is included in the negative type &&
                        // is negType object covers at-least n negIndividuals
                        // where n = ConfigParams.typeOfObjectsInNegIndivsMinSize
                        if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(subClassOwlClassExpression)
                                && SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).get(subClassOwlClassExpression)
                                >= ConfigParams.negTypeMinCoverIndivsSize) {

                            //create conjunctive horn clause and add positive part and negative part too
                            ConjunctiveHornClauseV1V2 conjunctiveHornClause = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                            conjunctiveHornClause.addPosObjectType(posOwlClassExpression);
                            conjunctiveHornClause.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV2 candidateClassV2 = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                            candidateClassV2.addConjunctiveHornClauses(conjunctiveHornClause);

                            // create candidate solution
                            CandidateSolutionV2 candidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                            candidateSolutionV2.addCandidateClass(candidateClassV2);
                            ////////////////////////////////////////
                            ///// this will take long time if, we have a large number of individuals in the ontology.
                            // this is because we calculate the hornclause's accuracy by reasoner in ecii-v2 and ecii-v1
                            logger.debug("addToSolutions() started.............");
                            boolean added = addToSolutions(candidateSolutionV2);
                            logger.debug("addToSolutions() finished");
                            /////////////////////////////////////
                            logger.debug("creating candidate solution finished: " + candidateSolutionV2.getSolutionAsString(false));
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClause.calculateAccuracyComplexCustom();
                                conjunctiveHornClause.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClause);

                                Score candidateClassScore = candidateClassV2.calculateAccuracyComplexCustom();
                                candidateClassV2.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV2);
                            }
                            logger.debug("adding candidate solution finished: " + candidateClassesMap.size());
                        }
                    }
                });
            });
        });
        logger.info("solution using only a single positive and single negative type finished." +
                " Total Solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());

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

                    // filter hashmap
                    OWLObjectProperty owlObjectProperty = owlObjectPropertyHashMapEntry.getKey();
                    HashMap<OWLClassExpression, Integer> hashMap = new HashMap<>(
                            owlObjectPropertyHashMapEntry.getValue().entrySet()
                                    .stream().filter(e -> e.getValue() >= ConfigParams.posTypeMinCoverIndivsSize)
                                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

                    if (hashMap.size() > 0) {
                        ArrayList<OWLClassExpression> limitedPosTypes = new ArrayList<>(hashMap.keySet());

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
                            logger.debug("debug: classExpressions.size(): " + classExpressions.size());
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
                                            .get(subClassOwlClassExpression) >= ConfigParams.negTypeMinCoverIndivsSize) {
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
                            logger.debug("debug: posOwlClassExpressions: " + posOwlClassExpressions.size());
                            validListCombinationOfSubClassesForNegPortion.forEach(subClasses -> {

                                // if every class of this combination is in negative types then include this combination otherwise skip this.
                                // this is trivially true as we are creating combination of those subclasses which are also contained in the negTypes.

                                //create conjunctive horn clause and add positive part and negative part too
                                ConjunctiveHornClauseV1V2 conjunctiveHornClauseV1V2 = new ConjunctiveHornClauseV1V2(owlObjectProperty, reasoner, ontology);
                                conjunctiveHornClauseV1V2.setPosObjectTypes(posOwlClassExpressions);
                                conjunctiveHornClauseV1V2.setNegObjectTypes(subClasses);

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
                    } else {
                        logger.warn("Filtering by ConfigParams.typeOfObjectsInPosIndivsMinSize produces 0 size hashmap!!");
                    }
                });
        logger.info("solution using multiple positive and multiple negative type finished. Total Solutions: "
                + SharedDataHolder.CandidateSolutionSetV2.size());
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
                CandidateClassV2 candidateClass = new CandidateClassV2(owlObjectProperty, reasoner, ontology);
                candidateClass.setConjunctiveHornClauses(conjunctiveHornClausesCombination);

                // create candidate solution
                CandidateSolutionV2 candidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                candidateSolutionV2.addCandidateClass(candidateClass);
                boolean added = addToSolutions(candidateSolutionV2);
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

    }

    /**
     * Create solutions using the combination of candidateClasses.
     * This function at first select the top K6 candidateClass and,
     * make combination of them to produce solutions
     *
     * V2: combination of candidateclass should be based on multiple object property not on single
     *      object property
     *      for example, it should be:  highFor.(Gene1 and Gene2) and lowFor.Gene3
     *      not: highFor.Gene1 and highFor.Gene2
     *
     *   todo:   to implement this: we can group the candidate classes based on the owlobjectproperty. grouping is already implemented
     *   in candidatesolutionv-x.java
     *
     */
    private void createSolutionByCombiningCandidateClass() {
        /**
         * Select top k6 CandidateClasses to make combination. This function reduces the candidate Classes size.
         */
        SortingUtility.sortAndFilterCandidateClassV2Map(candidateClassesMap, ConfigParams.candidateClassesListMaxSize);

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

            /**
             * debug some code
             */
            logger.debug("debugging owlObjectProperty: origList before objPropsCombination");
            origList.forEach(candidateClassV2s -> {
                logger.debug("objprops: ");
                candidateClassV2s.forEach(candidateClassV2 -> {
                    logger.debug(candidateClassV2.owlObjectProperty + "\t");
                });
                logger.debug("\n");
            });
            /**
             * Insights from debugging. our origList is producing list as [[highFor., highFor., highFor.], [empty., empty., empty.,]]
             * details log is in: /Users/sarker/Workspaces/Jetbrains/residue-emerald/residue/experiments/phite/pliers/phase-3/phite-settings-v1_results_ecii_V2_log.log file
             */

            Collection<List<CandidateClassV2>> objPropsCombination = Utility.restrictedCombinationHelper(origList);

            //  Valid combination of ObjectProperties.
            objPropsCombination.forEach(candidateClassV2s -> {

                // create candidate solution
                CandidateSolutionV2 candidateSolutionV2 = new CandidateSolutionV2(reasoner, ontology);
                candidateSolutionV2.setCandidateClasses(new ArrayList<>(candidateClassV2s));
                addToSolutions(candidateSolutionV2);
            });
        });
        logger.info("solution using combination of object proeprties/candidateClass finished. " +
                "Total solutions: " + SharedDataHolder.CandidateSolutionSetV2.size());
    }

    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("####################Solutions (sorted by " + Score.defaultScoreType + ")####################:");
        monitor.writeMessage("\n####################Solutions (sorted by " + Score.defaultScoreType + ")####################:");
        solutionCounter = 0;

        /// sum, max and average of all accuracy
        double f1Total = 0;
        double precisionTotal = 0;
        double recallTotal = 0;
        double coverageTotal = 0;
        
        double f1Max = 0;
        double precisionMax = 0;
        double recallMax = 0;
        double coverageMax = 0;
        
        double f1Average = 0;
        double precisionAverage = 0;
        double recallAverage = 0;
        double coverageAverage = 0;

        for (CandidateSolutionV2 solution : SharedDataHolder.SortedCandidateSolutionListV2) {
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

                    // add to the accuracy total
                    f1Total += solution.getScore().getF_measure();
                    precisionTotal += solution.getScore().getPrecision();
                    recallTotal += solution.getScore().getRecall();
                    coverageTotal += solution.getScore().getCoverage();

                    // calculate max
                    f1Max = Math.max(solution.getScore().getF_measure(), f1Max);
                    precisionMax = Math.max(solution.getScore().getPrecision(), precisionMax);
                    recallMax = Math.max(solution.getScore().getRecall(), recallMax);
                    coverageMax = Math.max(solution.getScore().getCoverage(), coverageMax);
                }
            }
        }

        logger.info("Total solutions found using raw list: " + SharedDataHolder.SortedCandidateSolutionListV2.size());
        logger.info("Total solutions found after removing empty solution: " + solutionCounter);

        monitor.writeMessage("\nTotal solutions found: " + solutionCounter);

        /// average of all accuracy
        f1Average = f1Total == 0 ? 0 : f1Total / (double) solutionCounter;
        precisionAverage = precisionTotal == 0 ? 0 : precisionTotal / (double) solutionCounter;
        recallAverage = recallTotal == 0 ? 0 : recallTotal / (double) solutionCounter;
        coverageAverage = coverageTotal == 0 ? 0 : coverageTotal / (double) solutionCounter;

        monitor.writeMessage("\nF1 average: " + f1Average);
        monitor.writeMessage("Precision average: " + precisionAverage);
        monitor.writeMessage("Recall average: " + recallAverage);
        monitor.writeMessage("Coverage average: " + coverageAverage);

        monitor.writeMessage("\nF1 max: " + f1Max);
        monitor.writeMessage("Precision max: " + precisionMax);
        monitor.writeMessage("Recall max: " + recallMax);
        monitor.writeMessage("Coverage max: " + coverageMax);
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