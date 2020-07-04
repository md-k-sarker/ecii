package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateClassV0;
import org.dase.ecii.datastructure.CandidateSolutionV0;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV0;
import org.dase.ecii.datastructure.HashMapUtility;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
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

@SuppressWarnings("Duplicates")


/**
 * Find solution using the algorithm mentioned in the paper. This solves the problem of creating combination of disjunctions..
 * Algorithm version: V0
 *
 */
public class CandidateSolutionFinderV0 {

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
        // debugExtractObjectTypes();

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
     * Utility/Helper method to add solution to solutionsSet.
     *
     * @param candidateSolutionV0
     */
    private boolean addToSolutions(CandidateSolutionV0 candidateSolutionV0) {

        if (!SharedDataHolder.candidateSolutionV0Set.contains(candidateSolutionV0)) {
            // calculate score
            Score accScore = candidateSolutionV0.calculateAccuracyComplexCustom();
            if (accScore.getCoverage() > 0) {
                candidateSolutionV0.setScore(accScore);
                // save to shared data holder
                SharedDataHolder.candidateSolutionV0Set.add(candidateSolutionV0);
                return true;
            }
            return false;
        }
        return false;
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

        ArrayList<OWLClassExpression> aListModified = new ArrayList<>();

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

        // for rfilled types and for bare types. for no object property or direct types we used SharedDataHolder.noneOWLObjProp

        // create solution using just one class expression.

        // solution using only a single positive type
        logger.info("solution using only a single positive type started...............");
        SharedDataHolder.typeOfObjectsInPosIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((posOwlClassExpression, integer) -> {

                //create conjunctive horn clause and add positive part and no negative part initially
                ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new
                        ConjunctiveHornClauseV0(owlObjectProperty,reasoner, ontology);
                conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);

                // create candidate class
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty,reasoner, ontology);
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
        logger.info("solution using only a single positive type finished. Total solutions: " + SharedDataHolder.candidateSolutionV0Set.size());

        // should we use only negative type without a single positive type in Conjunctive Horn Clauses?
        // ref: https://en.wikipedia.org/wiki/Horn_clause
        // solution using only a single negative type is only okay for V0 hornClauses,
        // essentially only for ecii_V0 solution.
        logger.info("solution using only a single negative type started...............");
        SharedDataHolder.typeOfObjectsInNegIndivs.forEach((owlObjectProperty, hashMap) -> {
            hashMap.forEach((negOwlClassExpression, integer) -> {

                // create conjunctive horn clause and add negative part and no positive part initially
                ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty,reasoner, ontology);
                conjunctiveHornClauseV0.addNegObjectType(negOwlClassExpression);

                // create candidate class
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty,reasoner, ontology);
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
        logger.info("solution using only a single negative type finished. Total solutions: " + SharedDataHolder.candidateSolutionV0Set.size());

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
                            ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty,reasoner, ontology);
                            conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);
                            conjunctiveHornClauseV0.addNegObjectType(subClassOwlClassExpression);

                            // create candidate class
                            CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty,reasoner, ontology);
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

        // single positive and multiple negative (upto K1 limit).
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
                    ConjunctiveHornClauseV0 conjunctiveHornClauseV0 = new ConjunctiveHornClauseV0(owlObjectProperty,reasoner, ontology);
                    conjunctiveHornClauseV0.setPosObjectType(posOwlClassExpression);
                    conjunctiveHornClauseV0.setNegObjectTypes(subClasses);

                    // create candidate class
                    CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty,reasoner, ontology);
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
        logger.info("solution using only a single positive and multiple negative type finished. Total Solutions: " + SharedDataHolder.candidateSolutionV0Set.size());

        /**
         * Select top k5 hornClauses to make combination. This function reduces the hornClauseMap size.
         */
        sortAndFilterHornClauseMap(ConfigParams.hornClausesListMaxSize);

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
                CandidateClassV0 candidateClassV0 = new CandidateClassV0(owlObjectProperty,reasoner, ontology);
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
            logger.info("\tcombination of horn clause using object property " + Utility.getShortName(owlObjectProperty) + " finished. Total solutions: " + SharedDataHolder.candidateSolutionV0Set.size());

        });
        logger.info("solution using combination of horn clause finished. Total solutions: " + SharedDataHolder.candidateSolutionV0Set.size());


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
        logger.info("solution using combination of object proeprties finished. Total solutions: " + SharedDataHolder.candidateSolutionV0Set.size());
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
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInPosIndivs, owlObjectProperty, posType);

                        //insert into individualObject to individualObject type mapping
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, posIndiv, owlObjectProperty, posType);
                    }
                });
            } else {

                logger.info("Below concepts are type/supertype of positive " + posIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI().getShortForm());
                logger.info("object count: " + reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened().size());
                reasoner.getObjectPropertyValues(posIndiv, owlObjectProperty).getFlattened().forEach(eachIndi -> {
                    logger.debug("\tindi: " + eachIndi.getIRI());

                    // insert into individuals count
                    HashMapUtility.insertIntoHashMap(SharedDataHolder.objectsInPosIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(posType -> {
                        logger.info("posType: " + posType.toString());
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
                logger.info("object count: " + reasoner.getTypes(negIndiv, false).getFlattened().size());
                reasoner.getTypes(negIndiv, false).getFlattened().forEach(negType -> {
                    logger.info("negType: " + negType.toString());
                    if (!negType.equals(owlDataFactory.getOWLThing()) && !negType.equals(owlDataFactory.getOWLNothing())) {
                        // insert into individualObject's type count
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.typeOfObjectsInNegIndivs, owlObjectProperty, negType);

                        //insert into individualObject to individualObject type mapping
                        HashMapUtility.insertIntoHashMap(SharedDataHolder.individualHasObjectTypes, negIndiv, owlObjectProperty, negType);
                    }
                });
            } else {
                logger.info("Below concepts are type/supertype of negative " + negIndiv.getIRI().toString() + " individual through objProp " + owlObjectProperty.getIRI().getShortForm());
                logger.info("object count: " + reasoner.getObjectPropertyValues(negIndiv, owlObjectProperty).getFlattened().size());
                reasoner.getObjectPropertyValues(negIndiv, owlObjectProperty).getFlattened().forEach(eachIndi -> {

                    // insert into individualObject count
                    HashMapUtility.insertIntoHashMap(SharedDataHolder.objectsInNegIndivs, owlObjectProperty, eachIndi);

                    reasoner.getTypes(eachIndi, false).getFlattened().forEach(negType -> {
                        logger.info("negType: " + negType.toString());
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


    transient volatile protected double nrOfTotalIndividuals;
    transient volatile protected double nrOfPositiveIndividuals;
    transient volatile protected double nrOfNegativeIndividuals;

    /**
     * @param K6
     */
    public void calculateAccuracyOfTopK6ByReasoner(int K6) {
        if (SharedDataHolder.SortedCandidateSolutionListV0.size() < K6) {
            SharedDataHolder.SortedCandidateSolutionListV0.forEach(candidateSolutionV0 -> {
                candidateSolutionV0.calculateAccuracyByReasoner();
            });
        } else {
            for (int i = 0; i < K6; i++) {
                SharedDataHolder.SortedCandidateSolutionListV0.get(i).calculateAccuracyByReasoner();
            }
        }
    }


    transient volatile private int o1Length = 0;
    transient volatile private int o2Length = 0;

    // temporary variables for using inside lambda
    transient volatile private List<ConjunctiveHornClauseV0> conjunctiveHornClausesListV0 = new ArrayList<>();
    transient volatile private List<CandidateClassV0> candidateClassesListV0 = new ArrayList<>();

    /**
     * Select top k5 hornCluases from the hornClausesMap.
     *
     * @param limit
     * @return
     */
    private HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV0>> sortAndFilterHornClauseMap(int limit) {

        // make a list
        conjunctiveHornClausesListV0.clear();
        hornClausesMap.forEach((owlObjectProperty, conjunctiveHornClauses) -> {
            conjunctiveHornClausesListV0.addAll(conjunctiveHornClauses);
            logger.info("conjunctiveHornClauses size:  " + conjunctiveHornClauses.size());
        });

        if (conjunctiveHornClausesListV0.size() > 0) {

            // sort the list
            logger.info("horn clauses map  will be filtered initial size: " + conjunctiveHornClausesListV0.size());
            conjunctiveHornClausesListV0.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length = 0;
                    o2Length = 0;

                    if (null != o1.getPosObjectType())
                        o1Length++;
                    if (null != o1.getNegObjectTypes()) {
                        // bug-fix: o2 to o1
                        o1Length += o1.getNegObjectTypes().size();
                    }
                    if (null != o2.getPosObjectType())
                        o2Length++;
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

            // test sorting
            logger.info("Score of first hornClause:  " + conjunctiveHornClausesListV0.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last hornClause:  " + conjunctiveHornClausesListV0.get(conjunctiveHornClausesListV0.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (conjunctiveHornClausesListV0.size() > limit + 1) {
                conjunctiveHornClausesListV0 = conjunctiveHornClausesListV0.subList(0, limit + 1);
            }

            // make group again.
            hornClausesMap.clear();
            conjunctiveHornClausesListV0.forEach(conjunctiveHornClauseV0 -> {
                HashMapUtility.insertIntoHashMap(hornClausesMap, conjunctiveHornClauseV0.getOwlObjectProperty(), conjunctiveHornClauseV0);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (conjunctiveHornClausesListV0.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + conjunctiveHornClausesListV0.size());
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
    private HashMap<OWLObjectProperty, HashSet<CandidateClassV0>> sortAndFilterCandidateClassMap(int limit) {
        // make a list
        candidateClassesListV0.clear();
        candidateClassesMap.forEach((owlObjectProperty, candidateClasses) -> {
            candidateClassesListV0.addAll(candidateClasses);
        });

        if (candidateClassesListV0.size() > 0) {
            // sort the list
            logger.info("horn clauses map  will be filtered initial size: " + candidateClassesListV0.size());
            candidateClassesListV0.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length = 0;
                    o2Length = 0;

                    o1.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                        if (null != conjunctiveHornClauseV0.getPosObjectType()) o1Length++;
                        o1Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
                    });

                    o2.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                        if (null != conjunctiveHornClauseV0.getPosObjectType()) o2Length++;
                        o2Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
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
            logger.info("Score of first candidate class:  " + candidateClassesListV0.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last candidate class:  " + candidateClassesListV0.get(candidateClassesListV0.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (candidateClassesListV0.size() > limit + 1) {
                candidateClassesListV0 = candidateClassesListV0.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesListV0.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesListV0.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + candidateClassesListV0.size());
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

        ArrayList<CandidateSolutionV0> solutionList = new ArrayList<>(
                SharedDataHolder.candidateSolutionV0Set);

        // small to large
        if (ascending) {
            solutionList.sort(new Comparator<CandidateSolutionV0>() {
                @Override
                public int compare(CandidateSolutionV0 o1, CandidateSolutionV0 o2) {
                    if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                        return 1;
                    }
                    if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                        // compare length, shorter length will be chosen first
                        o1Length = 0;
                        o2Length = 0;

                        //o1Length += o1.getAtomicPosOwlClasses().size();
                        o1.getCandidateClasses().forEach(candidateClassV0 -> {
                            candidateClassV0.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                                if (null != conjunctiveHornClauseV0.getPosObjectType()) o1Length++;
                                o1Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
                            });
                        });
                        //o2Length += o2.getAtomicPosOwlClasses().size();
                        o2.getCandidateClasses().forEach(candidateClassV0 -> {
                            candidateClassV0.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                                if (null != conjunctiveHornClauseV0.getPosObjectType()) o2Length++;
                                o2Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
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
            solutionList.sort(new Comparator<CandidateSolutionV0>() {
                @Override
                public int compare(CandidateSolutionV0 o1, CandidateSolutionV0 o2) {
                    if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                        return -1;
                    }
                    if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                        // compare length
                        o1Length = 0;
                        o2Length = 0;

                        //o1Length += o1.getAtomicPosOwlClasses().size();
                        o1.getCandidateClasses().forEach(candidateClassV0 -> {
                            candidateClassV0.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                                if (null != conjunctiveHornClauseV0.getPosObjectType()) o1Length++;
                                o1Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
                            });
                        });
                        //o2Length += o2.getAtomicPosOwlClasses().size();
                        o2.getCandidateClasses().forEach(candidateClassV0 -> {
                            candidateClassV0.getConjunctiveHornClauses().forEach(conjunctiveHornClauseV0 -> {
                                if (null != conjunctiveHornClauseV0.getPosObjectType()) o2Length++;
                                o2Length += conjunctiveHornClauseV0.getNegObjectTypes().size();
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
        SharedDataHolder.SortedCandidateSolutionListV0 = solutionList;

        return true;
    }


    /**
     * Print the solutions
     */
    public void printSolutions(int K6) {

        logger.info("####################Solutions####################:");
        monitor.writeMessage("\n####################Solutions####################:");
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

                        //logger.info("\t f_measure: " + solution.getScore().getF_measure());
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