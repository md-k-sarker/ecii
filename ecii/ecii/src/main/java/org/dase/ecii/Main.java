package org.dase.ecii;


import org.apache.log4j.PropertyConfigurator;
import org.dase.ecii.core.*;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.dase.ecii.exceptions.MalFormedIRIException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.*;

/**
 * Main class to initiate the induction process
 */
public class Main {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static OWLOntology ontology;
    //private static OWLOntologyManager manager;
    //private static OWLDataFactory dataFacotry;
    private static OWLReasoner owlReasoner;
    private static PrintStream outPutStream;
    private static Monitor monitor;

    static String alreadyGotResultPath = "/home/sarker/MegaCloud/ProjectHCBD/experiments/ade_with_wn_sumo/automated/without_score_got_result/";
    static ArrayList<String> alreadyGotResult = new ArrayList<String>();

    private static JTextPane jTextPane;

    public static void setTextPane(JTextPane textPane) {
        jTextPane = textPane;
    }


    /**
     * Initiate the variables by using namespace from loaded ontology.
     * Must be called after loading ontology.
     */
    public static void init() {
        // make sure ontology is loaded before init.
        if (null != ontology) {
            SharedDataHolder.owlOntology = ontology;
            SharedDataHolder.owlOntologyManager = ontology.getOWLOntologyManager();
            SharedDataHolder.owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();


            IRI objectPropIri = IRI.create(ConfigParams.namespace, "imageContains");
            OWLObjectProperty imgContains = SharedDataHolder.owlDataFactory.getOWLObjectProperty(objectPropIri);
            // SharedDataHolder.objPropImageContains = imgContains;
            SharedDataHolder.dlSyntaxRendererExt = new DLSyntaxRendererExt();
        } else {
            logger.error("init called before ontology loading.");
            logger.error("program exiting");
            monitor.stopSystem("", true);
        }
    }


    /**
     * clean the shared data holders.
     * should be called before starting the each induction operation.
     */
    private static void cleanSharedDataHolder() {

        SharedDataHolder.objProperties.clear();
        SharedDataHolder.posIndivs.clear();
        SharedDataHolder.negIndivs.clear();

        SharedDataHolder.objectsInPosIndivs.clear();
        SharedDataHolder.objectsInNegIndivs.clear();

        SharedDataHolder.typeOfObjectsInPosIndivs = new HashMap<>();
        SharedDataHolder.typeOfObjectsInNegIndivs = new HashMap<>();

        SharedDataHolder.sortedTypeOfObjectsInNegIndivs = new HashMap<>();
        SharedDataHolder.sortedTypeOfObjectsInNegIndivs = new HashMap<>();

        SharedDataHolder.individualHasObjectTypes = new HashMap<>();

        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.clear();
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByECII.clear();

        SharedDataHolder.SortedByReasonerCandidateSolutionList.clear();

        SharedDataHolder.CandidateSolutionSet.clear();
        // HashMap<Solution:solution,Boolean:shouldTraverse> SolutionsMap
        SharedDataHolder.SortedCandidateSolutionList.clear();

        SharedDataHolder.CandidateSolutionSetV1.clear();
        SharedDataHolder.SortedCandidateSolutionListV1.clear();

        SharedDataHolder.CandidateSolutionSetV2.clear();
        SharedDataHolder.SortedCandidateSolutionListV2.clear();

        SharedDataHolder.newIRICounter = 0;
    }


    /**
     * Initiate the outputpath, logger path, monitor etc and call doOps().
     *
     * @param outputResultPath
     */
    private static void initiateSingleDoOps(String outputResultPath) {

        try {
            // file to write
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputResultPath));
            PrintStream printStream = new PrintStream(bos, true);
            outPutStream = printStream;

            monitor = new Monitor(outPutStream, jTextPane);
            monitor.start("Program started.............", true);
            logger.info("Program started................");
            doOps();

            monitor.displayMessage("Result saved at: " + ConfigParams.outputResultPath, true);
            monitor.stop(System.lineSeparator() + "Program finished.", true);
            logger.info("Program finished.");

            outPutStream.close();
        } catch (Exception e) {
            logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
            } else {
                System.exit(0);
            }
        }
    }


    /**
     * Start the single induction process.
     *
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    private static void doOps() throws OWLOntologyCreationException, IOException, MalFormedIRIException {

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
        findConceptsObj.sortSolutionsCustom(false);
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

//        logger.info("printing solutions started...............");
//        findConceptsObj.printSolutions(ConfigParams.validateByReasonerSize);
//        logger.info("printing solutions finished.");
        monitor.writeMessage("\nTotal solutions found: " + SharedDataHolder.SortedCandidateSolutionListV2.size());

        logger.info("\nFinding similarity started...............");
        Similarity similarity = new Similarity(monitor, 0, owlReasoner);
        for (OWLNamedIndividual owlNamedIndividual : SharedDataHolder.posIndivs) {
            similarity.findSimilarityIFPWithAnotherIFP(owlNamedIndividual);
        }
        logger.info("Finding similarity finished");

    }

    private static void processBatchRunning(String dirPath) {
        processBatchRunning(Paths.get(dirPath));
    }

    /**
     * Iterate over the folders and call initiateSingleDoOps() for each confFile.
     *
     * @param dirPath
     */
    private static void processBatchRunning(Path dirPath) {

        try {
            // iterate over the files of a the folder
            Files.walk(dirPath).filter(f -> f.toFile().isFile())
                    .filter(f -> f.toFile().getAbsolutePath().endsWith(".config"))
                    .forEach(f -> {
                        // will get each file
                        String resultantFilePath = f.toString().replace(".config", "_results_ecii_v2.txt");
                        File resultFilePath = new File(resultantFilePath);
                        if (resultFilePath.exists() && !resultFilePath.isDirectory()) {
                            logger.info(f.toString() + " already has result, not running it.");
                        } else {
                            logger.info(" Program running for config file: " + f.toString());

                            // parse the config file
                            cleanSharedDataHolder();
                            ConfigParams.parseConfigParams(f.toString());
                            initiateSingleDoOps(ConfigParams.outputResultPath);
                        }
                    });
        } catch (Exception e) {
            logger.error("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
            } else {
                System.exit(0);
            }
        }

    }


    public static void printHelp() {
        String helpCommand = "Program runs in two mode. " +
                "\n\tBatch mode and " +
                "\n\tsingle mode. " +
                "\nIn single mode it will take a config file as input parameter and run the program as mentioned by the parameters in config file.\n" +
                "    \n" +
                "    In Batch mode it take directory as parameter and will run all the config files within that directory.\n" +
                "Command: \n" +
                "    For single mode: [config_file_path]\n" +
                "\n" +
                "    For Batch mode:  [-b directory_path]" +
                "\n" +
                "    For Help: [-h]" +
                "\n\n" +
                "Example:\n" +
                "    For single mode:\n" +
                "        java -jar ecii.jar config_file\n" +
                "\n" +
                "    For Batch mode:\n" +
                "        java -jar ecii.jar -b directory";

        String helpCommandParameter = "";

        System.out.println(helpCommand);

    }


    /**
     * Process the configurations of the program.
     */
    private static void processConfigurations() {

    }

    /**
     * @param args
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public static void main(String[] args) throws OWLOntologyCreationException, IOException, MalFormedIRIException {

        PropertyConfigurator.configure("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/main/resources/log4j.properties");


//        Files.walk(Paths.get("/Users/sarker/Workspaces/Jetbrains/residue/experiments/7_IFP/Entities_With_Ontology/raw_expr/"))
//                .filter(Files::isRegularFile)
//                .forEach(System.out::println);


        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg);
        }
        String argErrorStr1 = "Given parameter: ";
        String argErrorStr2 = " is not in correct format.";

        SharedDataHolder.programStartingDir = System.getProperty("user.dir");
        logger.info("Working directory/Program starting directory = " + SharedDataHolder.programStartingDir);

        logger.debug("args.length: " + args.length);

        if (args.length == 1) {

            if (args[0].equals("-h")) {
                printHelp();
                return;
            }
            /**
             * args[0] = confFilePath
             */
            logger.debug("given program argument: " + args[0]);
            if (args[0].endsWith(".config")) {

                // parse the config file
                ConfigParams.batch = false;
                ConfigParams.parseConfigParams(args[0]);
                initiateSingleDoOps(ConfigParams.outputResultPath);
            } else {
                System.out.println("Config file must ends with .config");
                printHelp();
            }

        } else if (args.length == 2) {
            /**
             * args[0] = -b
             * args[1] = directory
             */
            if (args[0].trim().toLowerCase().equals("-b") && !args[1].trim().endsWith(".config")) {
                ConfigParams.batch = true;
                ConfigParams.batchStartingPath = args[1];
                // start with args[1])
                try {
                    logger.info("Running on folder: " + args[1]);
                    processBatchRunning(args[1]);
                } catch (Exception e) {
                    logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
                    if (null != monitor) {
                        monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
                    } else {
                        System.exit(0);
                    }
                }

            } else {
                System.out.println(argErrorStr1 + sb.toString() + argErrorStr2);
                printHelp();
            }
        } else {
            System.out.println(argErrorStr1 + sb.toString() + argErrorStr2);
            printHelp();
        }
    }
}
