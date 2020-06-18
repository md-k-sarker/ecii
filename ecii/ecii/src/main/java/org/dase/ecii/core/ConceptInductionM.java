package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/17/20.
*/

import org.dase.ecii.exceptions.MalFormedIRIException;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
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

/**
 * Driver Class to call/start concept induction or measure similarity operations.
 *
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
    private ConceptInductionM(){

    }

    /**
     * Constructor
     */
    public ConceptInductionM(Monitor monitor) {

    }

    /**
     * Initiate the variables by using namespace from loaded ontology.
     * Must be called after loading ontology.
     */
    public  void init() {
        // make sure ontology is loaded before init.
        if (null != ontology) {
            SharedDataHolder.owlOntology = ontology;
            SharedDataHolder.owlOntologyManager = ontology.getOWLOntologyManager();
            SharedDataHolder.owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
            SharedDataHolder.dlSyntaxRendererExt = new DLSyntaxRendererExt();
        } else {
            logger.error("init called before ontology loading.");
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
    public  void doOpsConceptInductionM() throws
            OWLOntologyCreationException, IOException, MalFormedIRIException {

        logger.info("Working with confFile: " + ConfigParams.confFilePath);
        monitor.writeMessage("Working with confFile: " + Paths.get(ConfigParams.confFilePath).getFileName());
        // load ontotology
        ontology = Utility.loadOntology(ConfigParams.ontoPath);
        //loadOntology();

        //init variables
        init();

        // algorithm starting time here.
        DateFormat dateFormat = Utility.getDateTimeFormat();
        Long algoStartTime = System.currentTimeMillis();
        monitor.displayMessage("Algorithm starts at: " + dateFormat.format(new Date()), true);

        // initiate reasoner
        logger.info("reasoner initializing started........");
        owlReasoner = Utility.initReasoner(ConfigParams.reasonerName, ontology, monitor);
        logger.info("reasoner initialized successfully");

        logger.info("reading pos and neg indivs from conf file started........");
        SharedDataHolder.posIndivs = Utility.readPosExamplesFromConf(SharedDataHolder.confFileFullContent);
        logger.info("reading pos indivs from conf file finished successfully. SharedDataHolder.posIndivs.size: " + SharedDataHolder.posIndivs.size());
        logger.info("reading neg indivs from conf file started........");
        SharedDataHolder.negIndivs = Utility.readNegExamplesFromConf(SharedDataHolder.confFileFullContent);
        logger.info("reading neg indivs from conf file finished successfully SharedDataHolder.negIndivs.size: " + SharedDataHolder.negIndivs.size());

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

        // Create a new ConceptFinder object with the given reasoner.
        CandidateSolutionFinderV2 findConceptsObj = new CandidateSolutionFinderV2(owlReasoner, ontology, outPutStream, monitor);
        //ConceptFinderComplex findConceptsObj = new ConceptFinderComplex(owlReasoner, ontology, outPutStream, monitor);

        logger.info("finding solutions started...............");
        // SharedDataHolder.objPropImageContains,
        findConceptsObj.findConcepts(0, 0);
        //findConceptsObj.findConcepts(ConfigParams.tolerance, SharedDataHolder.objPropImageContains, ConfigParams.conceptsCombinationLimit);
        logger.info("\nfinding solutions finished.");

        logger.info("sorting solutions................");
        findConceptsObj.sortSolutionsCustom(ConfigParams.ascendingOfStringLength);
        //findConceptsObj.sortSolutions(false);
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


    public  void measurePairwiseSimilarity() {
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
