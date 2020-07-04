package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/17/20.
*/

import org.dase.ecii.datastructure.CandidateSolutionV2;
import org.dase.ecii.exceptions.MalFormedIRIException;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.dase.ecii.ontofactory.StripDownOntology;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
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
import java.util.*;

/**
 * Driver Class to call/start concept induction or measure similarity operations.
 */
public class ConceptInductionM {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private OWLOntology ontology;
    //private static OWLOntologyManager manager;
    //private static OWLDataFactory dataFacotry;
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
        // load ontotology
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
                logger.error("Fatar error!!!!!!!!");
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
        monitor.writeMessage("\nUser defined parameters:");
        monitor.writeMessage("K1/negExprTypeLimit: " + ConfigParams.conceptLimitInNegExpr);
        monitor.writeMessage("K2/hornClauseLimit: " + ConfigParams.hornClauseLimit);
        monitor.writeMessage("K3/objPropsCombinationLimit: " + ConfigParams.objPropsCombinationLimit);
        monitor.writeMessage("K4/posExprTypeLimit: " + ConfigParams.conceptLimitInPosExpr);
        monitor.writeMessage("K5/hornClausesListMaxSize: " + ConfigParams.hornClausesListMaxSize);
        monitor.writeMessage("K6/candidateClassesListMaxSize: " + ConfigParams.candidateClassesListMaxSize);
        monitor.writeMessage("K7/removeCommonTypes: " + ConfigParams.removeCommonTypes);
        monitor.writeMessage("DefaultScoreType: " + Score.defaultScoreType);
        monitor.writeMessage("ReasonerName: " + ConfigParams.reasonerName);
        monitor.writeMessage("k8/ValidateByReasonerSize: " + ConfigParams.validateByReasonerSize);
        monitor.writeMessage("k9/posClassListMaxSize: " + ConfigParams.posClassListMaxSize);
        monitor.writeMessage("k10/negClassListMaxSize: " + ConfigParams.negClassListMaxSize);

        logger.info("posIndivs from conf:");
        monitor.writeMessage("posIndivs from conf:");
        SharedDataHolder.posIndivs.forEach(owlNamedIndividual -> {
            logger.info("\t" + Utility.getShortName(owlNamedIndividual));
            monitor.writeMessage("\t" + Utility.getShortName(owlNamedIndividual));
        });

        logger.info("negIndivs from conf:");
        monitor.writeMessage("negIndivs from conf:");
        SharedDataHolder.negIndivs.forEach(owlNamedIndividual -> {
            logger.info("\t" + Utility.getShortName(owlNamedIndividual));
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

        // strip down the ontology
        stripOnto();

        // initiate reasoner
        logger.info("reasoner initializing started........");
        owlReasoner = Utility.initReasoner(ConfigParams.reasonerName, SharedDataHolder.owlOntologyStripped, monitor);
        logger.info("reasoner initialized successfully");

        // Create a new ConceptFinder object with the given reasoner.
        CandidateSolutionFinderV0 findConceptsObj = new CandidateSolutionFinderV0(owlReasoner, SharedDataHolder.owlOntologyStripped, outPutStream, monitor);
        //ConceptFinderComplex findConceptsObj = new ConceptFinderComplex(owlReasoner, ontology, outPutStream, monitor);

        logger.info("finding solutions started...............");
        // SharedDataHolder.objPropImageContains,
        findConceptsObj.findConcepts(0, 0);
        //findConceptsObj.findConcepts(ConfigParams.tolerance, SharedDataHolder.objPropImageContains, ConfigParams.conceptsCombinationLimit);
        logger.info("\nfinding solutions finished.");

        logger.info("sorting solutions................");
        SharedDataHolder.SortedCandidateSolutionListV2 = SortingUtility.sortSolutionsV2Custom(SharedDataHolder.CandidateSolutionSetV2,
                ConfigParams.ascendingOfStringLength);
        logger.info("sorting solutions finished.");

        logger.info("calculating accuracy using reasoner for top k6 solutions................");
        findConceptsObj.calculateAccuracyOfTopK6ByReasoner(ConfigParams.validateByReasonerSize);
        logger.info("calculating accuracy using reasoner for top k6 solutions................");

        Long algoEndTime = System.currentTimeMillis();
        monitor.displayMessage("\nAlgorithm ends at: " + dateFormat.format(new Date()), true);
        logger.info("Algorithm ends at: " + dateFormat.format(new Date()), true);
        monitor.displayMessage("\nAlgorithm duration: " + (algoEndTime - algoStartTime) / 1000.0 + " sec", true);
        logger.info("Algorithm duration: " + (algoEndTime - algoStartTime) / 1000.0 + " sec", true);

        logger.info("printing solutions started...............");
        findConceptsObj.printSolutions(ConfigParams.validateByReasonerSize);
        logger.info("printing solutions finished.");
        monitor.writeMessage("\nTotal solutions found: " + SharedDataHolder.SortedCandidateSolutionListV2.size());

        if (ConfigParams.runPairwiseSimilarity) {
            logger.info("\nFinding similarity started...............");
            measurePairwiseSimilarity();
            logger.info("Finding similarity finished");
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
