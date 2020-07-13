package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/17/20.
*/

import org.dase.ecii.exceptions.MalFormedIRIException;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
import org.dase.ecii.ontofactory.strip.StripDownOntology;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * Driver Class to call/start concept induction or measure similarity operations.
 */
public class ConceptInductionM {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private OWLOntology ontology;
    //private static OWLOntologyManager manager;
    private OWLReasoner owlReasoner;
    private PrintStream outPutStream;
    private Monitor monitor;

    /**
     *
     */
    private ConceptInductionM() {

    }

    /**
     * Constructor
     */
    public ConceptInductionM(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Load ontology and save the reference to SharedDataHolder
     *
     * @throws IOException
     * @throws OWLOntologyCreationException
     */
    public void loadOntoAndSaveReference() throws IOException, OWLOntologyCreationException {
        // load ontology
        ontology = Utility.loadOntology(ConfigParams.ontoPath);

        // make sure ontology is loaded before init.
        if (null != ontology) {
            SharedDataHolder.owlOntologyOriginal = ontology;
            SharedDataHolder.owlOntologyManager = SharedDataHolder.owlOntologyOriginal.getOWLOntologyManager();
            SharedDataHolder.owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
            SharedDataHolder.dlSyntaxRendererExt = new DLSyntaxRendererExt();
        } else {
            logger.error("ontology is null.");
            logger.error("program exiting");
            monitor.stopSystem("", true);
        }
    }

    /**
     * Strip the ontology to make the processing easier.
     * Stripping is strictly in memory, no saving in disk
     */
    public void stripOnto() {

        if (null != ontology) {
            logger.info("Stripping started..............");
            logger.info("Before stripping axioms size: " + ontology.getAxioms().size());

            StripDownOntology stripDownOntology = new StripDownOntology(ontology);
            ListofObjPropAndIndiv listofObjPropAndIndiv = new ListofObjPropAndIndiv();
            // direct individuals
            listofObjPropAndIndiv.directIndivs.addAll(SharedDataHolder.posIndivs);
            listofObjPropAndIndiv.directIndivs.addAll(SharedDataHolder.negIndivs);
            // indirect individuals
            if (SharedDataHolder.objProperties.size() > 1) {

                for (Map.Entry<OWLObjectProperty, Double> entry : SharedDataHolder.objProperties.entrySet()) {
                    OWLObjectProperty owlObjectProperty = entry.getKey();
                    if (owlObjectProperty != SharedDataHolder.noneOWLObjProp) {
                        listofObjPropAndIndiv.objPropsofInterest.add(owlObjectProperty);
                    }
                }
                // search and save the indirectIndivs
                listofObjPropAndIndiv = stripDownOntology.processIndirectIndivsUsingObjProps(listofObjPropAndIndiv);
            }
            HashSet<OWLAxiom> axiomsToKeep = stripDownOntology.extractAxiomsRelatedToIndivs(
                    listofObjPropAndIndiv.directIndivs,
                    listofObjPropAndIndiv.inDirectIndivs);

            /**
             * Option 1
             */
//            HashSet<OWLAxiom> axiomsToRemove = new HashSet<>();
//            Set<OWLAxiom> allAxioms = ontology.getAxioms();
//            // set subtraction allAxioms.removeAll = allAxioms-axiomsToKeep = allAxioms will contain only the axioms to remove
//            allAxioms.removeAll(axiomsToKeep);
//            axiomsToRemove.addAll(allAxioms);
//
//            SharedDataHolder.owlOntologyManager.removeAxioms(ontology, axiomsToRemove);

            /**
             * Option 2, create new in memory ontology
             */
            try {
                SharedDataHolder.owlOntologyStripped = SharedDataHolder.owlOntologyManager.createOntology(axiomsToKeep);
            } catch (OWLOntologyCreationException e) {
                logger.error("Fatal error!!!!!!!!");
                e.printStackTrace();
                logger.error("program exiting");
                monitor.stopSystem("", true);
            }

            logger.info("After stripping axioms size: " + SharedDataHolder.owlOntologyStripped.getAxioms().size());
            logger.info("Stripping finished.");
        } else {
            logger.error("Stripping can't start. Ontology is null");
            logger.error("program exiting");
            monitor.stopSystem("", true);
        }
    }

    /**
     * Write the parameters of ecci to result file
     */
    private void writeUserDefinedValuesToResultFile() {
        monitor.writeMessage("\nUser defined parameters:");
        monitor.writeMessage("ecciAlgorithmVersion: " + ConfigParams.ECIIAlgorithmVersion);
        monitor.writeMessage("K1/conceptLimitInNegExpr: " + ConfigParams.conceptLimitInNegExpr);
        monitor.writeMessage("K2/hornClauseLimit: " + ConfigParams.hornClauseLimit);
        monitor.writeMessage("K3/objPropsCombinationLimit: " + ConfigParams.objPropsCombinationLimit);
        monitor.writeMessage("K4/conceptLimitInPosExpr: " + ConfigParams.conceptLimitInPosExpr);
        monitor.writeMessage("K5/hornClausesListMaxSize: " + ConfigParams.hornClausesListMaxSize);
        monitor.writeMessage("K6/candidateClassesListMaxSize: " + ConfigParams.candidateClassesListMaxSize);
        monitor.writeMessage("K7/removeCommonTypes: " + ConfigParams.removeCommonTypes);
        if (ConfigParams.removeCommonTypes)
            monitor.writeMessage("removeCommonTypesFromOneSideOnly: " + ConfigParams.removeCommonTypesFromOneSideOnly);
        monitor.writeMessage("limitPosTypes: " + ConfigParams.limitPosTypes);
        if (ConfigParams.limitPosTypes)
            monitor.writeMessage("k9/posClassListMaxSize: " + ConfigParams.posClassListMaxSize);
        monitor.writeMessage("posTypeMinCoverIndivsSize: " + ConfigParams.posTypeMinCoverIndivsSize);
        monitor.writeMessage("limitNegTypes: " + ConfigParams.limitNegTypes);
        if (ConfigParams.limitNegTypes)
            monitor.writeMessage("k10/negClassListMaxSize: " + ConfigParams.negClassListMaxSize);
        monitor.writeMessage("negTypeMinCoverIndivsSize: " + ConfigParams.negTypeMinCoverIndivsSize);
        monitor.writeMessage("DefaultScoreType: " + Score.defaultScoreType);
        monitor.writeMessage("ReasonerName: " + ConfigParams.reasonerName);
        monitor.writeMessage("k8/ValidateByReasonerSize: " + ConfigParams.validateByReasonerSize);
    }

    /**
     * Check whether the posIndivs, negIndivs and objprops exist in the ontology
     * Does not check on the imported module of the ontology
     *
     * @param owlOntology
     * @param posIndivs
     * @param negIndivs
     * @param objProps
     * @return
     */
    private boolean isEntitiesExist(OWLOntology owlOntology, HashSet<OWLNamedIndividual> posIndivs,
                                    HashSet<OWLNamedIndividual> negIndivs, HashSet<OWLObjectProperty> objProps) {
        logger.info("Checking positive individuals exists in the ontology.......");
        HashSet<OWLNamedIndividual> ontoIndivs = new HashSet<>(owlOntology.getIndividualsInSignature());
        for (OWLNamedIndividual eachIndi : posIndivs) {
            if (!ontoIndivs.contains(eachIndi)) {
                logger.error("Positive individual " + eachIndi + " not found in the provided ontology. \n\tProgram exiting...");
                monitor.stopSystem("Positive individual " + eachIndi + " not found in the provided ontology." +
                        " \n\tProgram exiting...", true);
            }
        }
        logger.info("Checking positive individuals exists in the ontology finished successfully, all individuals found.");

        logger.info("Checking negative individuals exists in the ontology.......");
        for (OWLNamedIndividual eachIndi : negIndivs) {
            if (!ontoIndivs.contains(eachIndi)) {
                logger.error("Negative individual " + eachIndi + " not found in the provided ontology. \n\tProgram exiting...");
                monitor.stopSystem("Negative individual " + eachIndi + " not found in the provided ontology." +
                        " \n\tProgram exiting...", true);
            }
        }
        logger.info("Checking negative individuals exists in the ontology finished successfully, all individuals found.");

        logger.info("Checking input object properties exists in the ontology........");
        HashSet<OWLObjectProperty> ontoProps = new HashSet<>(owlOntology.getObjectPropertiesInSignature());
        for (OWLObjectProperty eachProp : objProps) {
            if (!eachProp.equals(SharedDataHolder.noneOWLObjProp)) {
                if (!ontoProps.contains(eachProp)) {
                    logger.error("Object property " + eachProp + " not found in the provided ontology. \n\tProgram exiting...");
                    monitor.stopSystem("Object property " + eachProp + " not found in the provided ontology." +
                            " \n\tProgram exiting...", true);
                }
            }
        }
        logger.info("Checking input object properties exists in the ontology finished successfully, all object properties found.");
        return true;
    }

    /**
     * Start the single induction process.
     *
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public void doOpsConceptInductionM() throws
            OWLOntologyCreationException, IOException, MalFormedIRIException {

        logger.info("Working with confFile: " + ConfigParams.confFilePath);
        monitor.writeMessage("Working with confFile: " + Paths.get(ConfigParams.confFilePath).getFileName());

        // write user defined values to resultFile
        writeUserDefinedValuesToResultFile();

        logger.info("posIndivs from conf size: " + SharedDataHolder.posIndivs.size());
        monitor.writeMessage("posIndivs from conf:");
        SharedDataHolder.posIndivs.forEach(owlNamedIndividual -> {
            logger.debug("\t" + Utility.getShortName(owlNamedIndividual));
            monitor.writeMessage("\t" + Utility.getShortName(owlNamedIndividual));
        });

        logger.info("negIndivs from conf: " + SharedDataHolder.negIndivs.size());
        monitor.writeMessage("negIndivs from conf:");
        SharedDataHolder.negIndivs.forEach(owlNamedIndividual -> {
            logger.debug("\t" + Utility.getShortName(owlNamedIndividual));
            monitor.writeMessage("\t" + Utility.getShortName(owlNamedIndividual));
        });

        logger.info("objProps from conf:");
        monitor.writeMessage("objProps from conf:");
        SharedDataHolder.objProperties.forEach((owlObjectProperty, aDouble) -> {
            logger.info("\t" + Utility.getShortName(owlObjectProperty));
            monitor.writeMessage("\t" + Utility.getShortName(owlObjectProperty));
        });

        // algorithm starting time here.
        DateFormat dateFormat = Utility.getDateTimeFormat();
        Long algoStartTime = System.currentTimeMillis();
        monitor.displayMessage("Algorithm starts at: " + dateFormat.format(new Date()), true);

        //load Onto
        loadOntoAndSaveReference();

        // check if individuals and objProps exist in the ontology, if anything not found program will exit immediately
        isEntitiesExist(ontology, SharedDataHolder.posIndivs, SharedDataHolder.negIndivs,
                new HashSet<>(SharedDataHolder.objProperties.keySet()));

        // strip down the ontology
        stripOnto();

        // initiate reasoner
        logger.info("reasoner initializing started........");
        owlReasoner = Utility.initReasoner(ConfigParams.reasonerName, SharedDataHolder.owlOntologyStripped, monitor);
        logger.info("reasoner initialized successfully");

        // Create a new ConceptFinder object with the given reasoner.
        CandidateSolutionFinder findConceptsObj;
        if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V0)
            findConceptsObj = new CandidateSolutionFinderV0(owlReasoner, SharedDataHolder.owlOntologyStripped, outPutStream, monitor);
        else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V1)
            findConceptsObj = new CandidateSolutionFinderV1(owlReasoner, SharedDataHolder.owlOntologyStripped, outPutStream, monitor);
        else
            findConceptsObj = new CandidateSolutionFinderV2(owlReasoner, SharedDataHolder.owlOntologyStripped, outPutStream, monitor);

        logger.info("finding solutions started...............");
        findConceptsObj.findConcepts(0, 0);
        logger.info("\nfinding solutions finished.");

        // sort solutions
        sortSolutions();

        // calculate/validate accuracy by reasoner if user want
        calculateByReasoner(findConceptsObj);

        Long algoEndTime = System.currentTimeMillis();
        monitor.displayMessage("\nAlgorithm ends at: " + dateFormat.format(new Date()), true);
        logger.info("Algorithm ends at: " + dateFormat.format(new Date()), true);
        monitor.displayMessage("\nAlgorithm duration: " + (algoEndTime - algoStartTime) / 1000.0 + " sec", true);
        logger.info("Algorithm duration: " + (algoEndTime - algoStartTime) / 1000.0 + " sec", true);

        // print/write solutions
        writeSolutions(findConceptsObj);

        if (ConfigParams.runPairwiseSimilarity) {
            logger.info("\nFinding similarity started...............");
            measurePairwiseSimilarity();
            logger.info("Finding similarity finished");
        }
    }

    /**
     * Sort the solutions
     */
    private void sortSolutions() {
        logger.info("sorting solutions................");
        if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V2) {
            SharedDataHolder.SortedCandidateSolutionListV2 = SortingUtility.
                    sortSolutionsV2Custom(SharedDataHolder.CandidateSolutionSetV2,
                            ConfigParams.ascendingOfStringLength);
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V1) {
            SharedDataHolder.SortedCandidateSolutionListV1 = SortingUtility.
                    sortSolutionsV1Custom(SharedDataHolder.CandidateSolutionSetV1,
                            ConfigParams.ascendingOfStringLength);
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V0) {
            SharedDataHolder.SortedCandidateSolutionListV0 = SortingUtility.
                    sortSolutionsV0Custom(SharedDataHolder.CandidateSolutionSetV0,
                            ConfigParams.ascendingOfStringLength);
        }
        logger.info("sorting solutions finished.");
    }

    /**
     * Calculate accuracy by reasoner
     */
    private void calculateByReasoner(CandidateSolutionFinder findConceptsObj) {
        logger.info("calculating accuracy using reasoner for top k6 solutions................");
        if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V2 &&
                findConceptsObj instanceof CandidateSolutionFinderV2) {
            findConceptsObj.calculateAccuracyOfTopK6ByReasoner(
                    SharedDataHolder.SortedCandidateSolutionListV2,
                    ConfigParams.validateByReasonerSize);
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V1 &&
                findConceptsObj instanceof CandidateSolutionFinderV1) {
            findConceptsObj.calculateAccuracyOfTopK6ByReasoner(
                    SharedDataHolder.SortedCandidateSolutionListV1,
                    ConfigParams.validateByReasonerSize);
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V0 &&
                findConceptsObj instanceof CandidateSolutionFinderV0) {
            findConceptsObj.calculateAccuracyOfTopK6ByReasoner(
                    SharedDataHolder.SortedCandidateSolutionListV0,
                    ConfigParams.validateByReasonerSize);
        }

        logger.info("calculating accuracy using reasoner for top k6 solutions................");
    }

    /**
     * Write/print solutions
     *
     * @param findConceptsObj
     */
    private void writeSolutions(CandidateSolutionFinder findConceptsObj) {
        logger.info("printing solutions started...............");
        findConceptsObj.printSolutions(ConfigParams.validateByReasonerSize);
        logger.info("printing solutions finished.");
        if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V2 &&
                findConceptsObj instanceof CandidateSolutionFinderV2) {
            monitor.writeMessage("\nTotal solutions found: " + SharedDataHolder.SortedCandidateSolutionListV2.size());
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V1 &&
                findConceptsObj instanceof CandidateSolutionFinderV1) {
            monitor.writeMessage("\nTotal solutions found: " + SharedDataHolder.SortedCandidateSolutionListV1.size());
        } else if (ConfigParams.ECIIAlgorithmVersion == ECIIVersion.V0 &&
                findConceptsObj instanceof CandidateSolutionFinderV0) {
            monitor.writeMessage("\nTotal solutions found: " + SharedDataHolder.SortedCandidateSolutionListV0.size());
        }
    }

    public void measurePairwiseSimilarity() {
        if (SharedDataHolder.posIndivs.size() != SharedDataHolder.negIndivs.size()) {
            logger.error("Pairwise similarity cant be done as positive and negative size is not equal");
            return;
        }

        ArrayList<OWLNamedIndividual> posIndivsList = new ArrayList<>(SharedDataHolder.posIndivs);
        ArrayList<OWLNamedIndividual> negIndivsList = new ArrayList<>(SharedDataHolder.negIndivs);

        Similarity similarity = new Similarity(monitor, 0, owlReasoner);

        if (posIndivsList.size() == negIndivsList.size()) {
            for (int i = 0; i < posIndivsList.size(); i++) {
                double similarity_score = similarity.findSimilarityIndivWithAnotherIndiv(posIndivsList.get(i), negIndivsList.get(i));
            }
        }
    }

}
