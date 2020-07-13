package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV0;
import org.dase.ecii.datastructure.CandidateSolutionV0;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV0;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
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
 * Algorithm version: V0
 *
 */
public class CandidateSolutionFinderV0 extends CandidateSolutionFinder {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This is temporary hashMap used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV0>> hornClausesMap;

    /**
     * This is temporary hashSet used for creating combination of hornClause.
     */
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV0>> candidateClassesMap;

    /**
     * Constructor
     *
     * @param _reasoner
     * @param _ontology
     */
    public CandidateSolutionFinderV0(OWLReasoner _reasoner, OWLOntology _ontology, PrintStream _printStream, Monitor _monitor) {
        super(_reasoner, _ontology, _printStream, _monitor);
        this.hornClausesMap = new HashMap<>();
        this.candidateClassesMap = new HashMap<>();
    }

    /**
     * Utility/Helper method to add solution to solutionsSet.
     *
     * @param candidateSolutionV0
     */
    private boolean addToSolutions(CandidateSolutionV0 candidateSolutionV0) {

        if (!SharedDataHolder.CandidateSolutionSetV0.contains(candidateSolutionV0)) {
            // calculate score
            Score accScore = candidateSolutionV0.calculateAccuracyComplexCustom();
            if (accScore.getCoverage() > 0) {
                candidateSolutionV0.setScore(accScore);
                // save to shared data holder
                SharedDataHolder.CandidateSolutionSetV0.add(candidateSolutionV0);
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

        // for rfilled types and for bare types. for no object property or direct types we used SharedDataHolder.noneOWLObjProp

        // solution using only a single positive type
        createSolutionUsingSinglePosTypes();

        // should we use only negative type without a single positive type in Conjunctive Horn Clauses?
        // ref: https://en.wikipedia.org/wiki/Horn_clause
        // solution using only a single negative type is only okay for V0 hornClauses,
        // essentially only for ecii_V0 solution.
        createSolutionUsingSingleNegTypes();

        // create solution using both positive and negative of class expressions.
        // single positive and single negative.
        createSolutionUsingSinglePosAndNegTypes();

        // single positive and multiple negative (upto K1 limit).
        createSolutionUsingSinglePosAndMultiNegTypes();

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
        logger.info("solution using only a single positive type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                //create conjunctive horn clause and add positive part and no negative part initially
                ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new
                        ConjunctiveHornClauseV0(owlObjectProperty, reasoner, ontology);
                conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);

                // create candidate class
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty, reasoner, ontology);
                candidateClassV0.addConjunctiveHornClauses(conjunctiveHornClauseV0);

                // create candidate solution
                CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                candidateSolutionV0.addCandidateClass(candidateClassV0);
                boolean added = addToSolutions(candidateSolutionV0);
                if (added) {
                    // save temporarily for combination
                    Score hornClauseScore = conjunctiveHornClauseV0.calculateAccuracyComplexCustom();
                    conjunctiveHornClauseV0.setScore(hornClauseScore);
                    HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV0);

                    Score candidateClassScore = candidateClassV0.calculateAccuracyComplexCustom();
                    candidateClassV0.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV0);
                }
            });
        });
        logger.info("solution using only a single positive type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());
    }

    /**
     * Create solutions using single negative types.
     */
    private void createSolutionUsingSingleNegTypes() {
        logger.info("solution using only a single negative type started...............");
        SharedDataHolder.typeOfObjectsInNegIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((negOwlClassExpression, integer) -> {

                // create conjunctive horn clause and add negative part and no positive part initially
                ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty, reasoner, ontology);
                conjunctiveHornClauseV0.addNegObjectType(negOwlClassExpression);

                // create candidate class
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty, reasoner, ontology);
                candidateClassV0.addConjunctiveHornClauses(conjunctiveHornClauseV0);

                // create candidate solution
                CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                candidateSolutionV0.addCandidateClass(candidateClassV0);
                boolean added = addToSolutions(candidateSolutionV0);
                if (added) {
                    // save temporarily for combination
                    Score hornClauseScore = conjunctiveHornClauseV0.calculateAccuracyComplexCustom();
                    conjunctiveHornClauseV0.setScore(hornClauseScore);
                    HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV0);

                    Score candidateClassScore = candidateClassV0.calculateAccuracyComplexCustom();
                    candidateClassV0.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV0);
                }
            });
        });
        logger.info("solution using only a single negative type finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());
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
                            ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty, reasoner, ontology);
                            conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);
                            conjunctiveHornClauseV0.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty, reasoner, ontology);
                            candidateClassV0.addConjunctiveHornClauses(conjunctiveHornClauseV0);

                            // create candidate solution
                            CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                            candidateSolutionV0.addCandidateClass(candidateClassV0);
                            boolean added = addToSolutions(candidateSolutionV0);
                            if (added) {
                                // save temporarily for combination
                                Score hornClauseScore = conjunctiveHornClauseV0.calculateAccuracyComplexCustom();
                                conjunctiveHornClauseV0.setScore(hornClauseScore);
                                HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV0);

                                Score candidateClassScore = candidateClassV0.calculateAccuracyComplexCustom();
                                candidateClassV0.setScore(candidateClassScore);
                                HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV0);
                            }
                        }
                    }
                });
            });
        });
        logger.info("solution using only a single positive and single negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV1.size());
    }

    /**
     * Create solutions using single positive types and single negative types.
     */
    private void createSolutionUsingSinglePosAndMultiNegTypes() {
        logger.info("solution using only a single positive and multiple negative type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressions = new ArrayList<>(
                        reasoner.getSubClasses(posOwlClassExpression, false).getFlattened().stream().collect(Collectors.toList()));

                ArrayList<OWLClassExpression> posTypeOwlSubClassExpressionsForCombination = new ArrayList<>();

                // create combination only those which are contained in the negative type.
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

                ArrayList<ArrayList<OWLClassExpression>> listCombinationOfSubClasses;
                // combination of 1, starting from 1 for negtypes. posTypes are at-least 2 here.
                listCombinationOfSubClasses = Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, 1);
                // combination from 2 to upto ccombinationLimit
                for (int combinationCounter = 2; combinationCounter <= ConfigParams.conceptLimitInNegExpr; combinationCounter++) {
                    // combination of combinationCounter
                    listCombinationOfSubClasses.addAll(Utility.combinationHelper(posTypeOwlSubClassExpressionsForCombination, combinationCounter));
                }

                // keep only valid listCombinationOfSubClasses.
                // a combination is valid if and only if it doesn't have self subClass.
                ArrayList<ArrayList<OWLClassExpression>> validListCombinationOfSubClasses = new ArrayList<>();
                listCombinationOfSubClasses.forEach(classExpressions -> {
                    if (isValidCombinationOfSubClasses(classExpressions)) {
                        validListCombinationOfSubClasses.add(classExpressions);
                    }
                });
                // recover memory
                listCombinationOfSubClasses = null;

                validListCombinationOfSubClasses.forEach(subClasses -> {

                    // if every class of this combination is in negative types then include this combination otherwise skip this.
                    // this is trivially true as we are creating combination of those subclasses which are also contained in the negTypes.

                    //create conjunctive horn clause and add positive part and negative part too
                    ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty, reasoner, ontology);
                    conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);
                    conjunctiveHornClauseV0.setNegObjectTypes(subClasses);

                    // create candidate class
                    CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty, reasoner, ontology);
                    candidateClassV0.addConjunctiveHornClauses(conjunctiveHornClauseV0);

                    // create candidate solution
                    CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                    candidateSolutionV0.addCandidateClass(candidateClassV0);
                    boolean added = addToSolutions(candidateSolutionV0);
                    if (added) {
                        // save temporarily for combination
                        Score hornClauseScore = conjunctiveHornClauseV0.calculateAccuracyComplexCustom();
                        conjunctiveHornClauseV0.setScore(hornClauseScore);
                        HashMapUtility.insertIntoHashMap(hornClausesMap, owlObjectProperty, conjunctiveHornClauseV0);

                        Score candidateClassScore = candidateClassV0.calculateAccuracyComplexCustom();
                        candidateClassV0.setScore(candidateClassScore);
                        HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV0);
                    }
                });
            });
        });
        logger.info("solution using only a single positive and multiple negative type finished. Total Solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());
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
        SortingUtility.sortAndFilterHornClauseV0Map(hornClausesMap, ConfigParams.hornClausesListMaxSize);

        /**
         * combination of horn clause. (upto K2/hornClauseLimit limit).
         * Use previously created horn clauses.
         */
        logger.info("solution using combination of horn clause started...............");
        hornClausesMap.forEach((owlObjectProperty, conjunctiveHornClauses) -> {
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " started...............");
            ArrayList<ConjunctiveHornClauseV0> hornClauseArrayList = new ArrayList<>(conjunctiveHornClauses);
            logger.info("\thorn clause size: " + hornClauseArrayList.size());

            ArrayList<ArrayList<ConjunctiveHornClauseV0>> listCombinationOfHornClauses = new ArrayList<>();
            // combination of 2
            if (ConfigParams.hornClauseLimit >= 2)
                listCombinationOfHornClauses = Utility.combinationHelper(hornClauseArrayList, 2);
            // combination from 3 to upto ccombinationLimit
            for (int combinationCounter = 3; combinationCounter <= ConfigParams.hornClauseLimit; combinationCounter++) {
                // combination of combinationCounter
                listCombinationOfHornClauses.addAll(Utility.combinationHelper(hornClauseArrayList, combinationCounter));
            }

            //  Valid combination of hornClauses.
            listCombinationOfHornClauses.forEach(conjunctiveHornClausesCombination -> {

                //create candidate class
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty, reasoner, ontology);
                candidateClassV0.setConjunctiveHornClauses(conjunctiveHornClausesCombination);

                // create candidate solution
                CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                candidateSolutionV0.addCandidateClass(candidateClassV0);
                boolean added = addToSolutions(candidateSolutionV0);
                if (added) {
                    // save temporarily for combination
                    Score candidateClassScore = candidateClassV0.calculateAccuracyComplexCustom();
                    candidateClassV0.setScore(candidateClassScore);
                    HashMapUtility.insertIntoHashMap(candidateClassesMap, owlObjectProperty, candidateClassV0);
                }
            });
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());

        });
        logger.info("solution using combination of horn clause finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());
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
        SortingUtility.sortAndFilterCandidateClassV0Map(candidateClassesMap, ConfigParams.candidateClassesListMaxSize);

        /**
         * combination of candidateClass/objectproperties. (upto K3/objPropsCombinationLimit limit)
         *
         */
        logger.info("solution using combination of object proeprties started...............");
        SharedDataHolder.objPropertiesCombination.forEach(owlObjectProperties -> {

            List<Collection<CandidateClassV0>> origList = new ArrayList<>();
            candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
                if (owlObjectProperties.contains(owlObjectProperty)) {
                    origList.add(candidateClasses);
                }
            });
            Collection<List<CandidateClassV0>> objPropsCombination = Utility.restrictedCombinationHelper(origList);

            //  Valid combination of ObjectProperties.
            objPropsCombination.forEach(candidateClasses -> {

                // create candidate solution
                CandidateSolutionV0 candidateSolutionV0 = new CandidateSolutionV0(reasoner, ontology);
                candidateSolutionV0.setCandidateClasses(new ArrayList<>(candidateClasses));
                addToSolutions(candidateSolutionV0);
            });
        });
        logger.info("solution using combination of object proeprties finished. Total solutions: " + SharedDataHolder.CandidateSolutionSetV0.size());
    }

    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("\n####################Solutions (sorted by "+ Score.defaultScoreType+")####################:");
        monitor.writeMessage("\n####################Solutions (sorted by "+ Score.defaultScoreType+")####################:");
        solutionCounter = 0;

        SharedDataHolder.SortedCandidateSolutionListV0.forEach((solution) -> {

            if (solution.getGroupedCandidateClasses().size() > 0) {
                solutionCounter++;

                String solutionAsString = solution.getSolutionAsString(true);

                if (solutionAsString.length() > 0 && null != solution.getScore()) {
                    //logger.info("solution " + solutionCounter + ": " + solutionAsString);
                    monitor.writeMessage("solution " + solutionCounter + ": " + solutionAsString);
                    DLSyntaxRendererExt dlRenderer = new DLSyntaxRendererExt();
                    monitor.writeMessage("\tsolution pretty-printed by reasoner: " + dlRenderer.render(solution.getSolutionAsOWLClassExpression()));

                    if (solutionCounter < K6) {
                        //logger.info("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score_by_reasoner: " + solution.getScore().getCoverage_by_reasoner());

                        //logger.info("\t f_measure: " + solution.getScore().getF_measure());
                        monitor.writeMessage("\t f_measure: " + solution.getScore().getF_measure());
                        monitor.writeMessage("\t f_measure_by_reasoner: " + solution.getScore().getF_measure_by_reasoner());
                        monitor.writeMessage("\t precision: " + solution.getScore().getPrecision());
                        monitor.writeMessage("\t recall: " + solution.getScore().getRecall());
                    } else {
                        //logger.info("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t coverage_score: " + solution.getScore().getCoverage());
                        monitor.writeMessage("\t f_measure: " + solution.getScore().getF_measure());
                        monitor.writeMessage("\t precision: " + solution.getScore().getPrecision());
                        monitor.writeMessage("\t recall: " + solution.getScore().getRecall());

                    }
                }
            }
        });

        logger.info("Total solutions found using raw list: " + SharedDataHolder.SortedCandidateSolutionListV0.size());
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