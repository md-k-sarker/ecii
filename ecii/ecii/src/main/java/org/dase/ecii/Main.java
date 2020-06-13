package org.dase.ecii;


import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.PropertyConfigurator;
import org.dase.ecii.core.*;
import org.dase.ecii.ontofactory.CreateOWLFromCSV;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.dase.ecii.ontofactory.OntoCombiner;
import org.dase.ecii.ontofactory.StripDownOntology;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndivTextualName;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.dase.ecii.exceptions.MalFormedIRIException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import javax.swing.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
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

        SharedDataHolder.sortedByReasonerCandidateSolutionV0List.clear();

        SharedDataHolder.candidateSolutionV0Set.clear();
        // HashMap<Solution:solution,Boolean:shouldTraverse> SolutionsMap
        SharedDataHolder.sortedCandidateSolutionV0List.clear();

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

    public static void measurePairwiseSimilarity() {
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
                        String resultantFilePath = f.toString().replace(".config", ConfigParams.resultFileExtension);
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


    /**
     * Strip down the input ontology and save it to disk.
     * Keep only the related axioms of given entity (got from csv file).
     *
     * @param inputOntoPath
     * @param entityCsvFilePath
     * @param indivColumnName
     * @param typeColumnName
     * @param outputOntoIRI
     */
    public static void stripDownOntoIndivsTypes(String inputOntoPath, String entityCsvFilePath, String indivColumnName, String typeColumnName, String outputOntoIRI) {

        StripDownOntology stripDownOntology = new StripDownOntology(inputOntoPath);

        HashMap<String, HashSet<String>> namesHashMap = stripDownOntology.readIndivTypesFromCSVFile(entityCsvFilePath, indivColumnName, typeColumnName);

        HashMap<OWLNamedIndividual, HashSet<OWLClass>> entityHashMap = stripDownOntology.convertToOntologyEntity(namesHashMap);

        HashSet<OWLAxiom> axiomsToKeep = new HashSet<>();

        for (Map.Entry<OWLNamedIndividual, HashSet<OWLClass>> indivTypesHashMap : entityHashMap.entrySet()) {
            axiomsToKeep.addAll(stripDownOntology.extractAxiomsRelatedToOWLClasses(indivTypesHashMap.getValue()));
        }

        OWLOntologyManager outputOntoManager = OWLManager.createOWLOntologyManager();

        OWLOntology outputOntology = null;
        try {
            outputOntology = outputOntoManager.createOntology(IRI.create(outputOntoIRI));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        outputOntoManager.addAxioms(outputOntology, axiomsToKeep);

        try {
            String extension = FilenameUtils.getExtension(inputOntoPath);
            String outputOntoPath = inputOntoPath.replace("." + extension, "_stripped." + extension);
            Utility.saveOntology(outputOntology, outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public static void stripDownOntoIndivsObjProps(String inputOntoPath, String entityCsvFilePath, String indivColumnName, String objPropColumnName, String outputOntoIRI) {
        StripDownOntology stripDownOntology = new StripDownOntology(inputOntoPath);

        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = stripDownOntology.
                readEntityFromCSVFile(entityCsvFilePath, objPropColumnName, indivColumnName);

        ListofObjPropAndIndiv listofObjPropAndIndiv = stripDownOntology.
                convertToOntologyEntity(listofObjPropAndIndivTextualName);

        listofObjPropAndIndiv = stripDownOntology.processIndirectIndivsUsingObjProps(listofObjPropAndIndiv);

        HashSet<OWLAxiom> axiomsToKeep = stripDownOntology.extractAxiomsRelatedToIndivs(listofObjPropAndIndiv.directIndivs, listofObjPropAndIndiv.inDirectIndivs);

        OWLOntologyManager outputOntoManager = OWLManager.createOWLOntologyManager();

        OWLOntology outputOntology = null;
        try {
            outputOntology = outputOntoManager.createOntology(IRI.create(outputOntoIRI));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        outputOntoManager.addAxioms(outputOntology, axiomsToKeep);

        try {
            String extension = FilenameUtils.getExtension(inputOntoPath);
            String outputOntoPath = inputOntoPath.replace("." + extension, "stripped." + extension);
            Utility.saveOntology(outputOntology, outputOntoPath);

            monitor.displayMessage("File stripped successfully and saved at: " + outputOntoPath, true);

        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param inputOntologiesDirectory
     * @param outputOntologyIRI
     */
    public static void combineOntologies(String inputOntologiesDirectory, String outputOntologyIRI) {
        if(null == inputOntologiesDirectory){
            logger.error("Error!!!!!! Input ontologies directory path can't be null");
            return;
        }
        OntoCombiner ontoCombiner = new OntoCombiner(outputOntologyIRI);
        ontoCombiner.combineOntologies(null, inputOntologiesDirectory);
    }

    /**
     * @param csvPath
     * @param objPropColumnName
     * @param indivColumnName
     * @param typeColumnName
     */
    public static void createOntologyFromCSV(String csvPath, String objPropColumnName, String indivColumnName, String typeColumnName) {
        CreateOWLFromCSV createOWLFromCSV = null;
        try {
            logger.info("processing csv file: " + csvPath);
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), "talksAbout",
                    "http://www.daselab.com/residue/analysis");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(indivColumnName, typeColumnName);
    }


    /**
     * Process the configurations of the program.
     */
    private static void processConfigurations() {

    }

    static String argErrorStr1 = "Given parameter:";
    static String argErrorStr2 = "is not in correct format.";
    static String argErrorNoArgGiven = "No options and parameter is provided. You need to specify a option and correspoding parameters.";

    public static void printHelp() {
        String helpCommand = "\n\nProgram options:" +
                "\n1. Measure similarity between ontology entities" +
                "\n2. Perform concept induction" +
                "\n3. Strip down ontology or keeping entities of interest while discarding others" +
                "\n4. Create ontology from CSV file" +
                "\n5. Combine multiple ontology" +
                "\n\n" +
                "\tFor Help: [-h]" +
                "\n\tOptions:" +
                "\n\t\t-m : Measure similarity between ontology entity" +
                "\n\t\t-e : Concept induction by ecii algorithm" +
                "\n\t\t-c : Combine ontology" +
                "\n\t\t-s : Strip down ontology" +
                "\n\t\t-o : Ontology creation from CSV" +
                "\n\n" +
                "Parameters for different options:" +
                "\n\t-m or -e [config_file_path]" +
                "\n\t-m or -e [-b] [directory_path]" +
                "\n\t-c [inputOntologiesDirectory, outputOntologyIRI]" +
                "\n\t-s [-obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI] " +
                "\n\t-o [entityCsvFilePath, objPropColumnName, indivColumnName, typeColumnName, outputOntoIRI]" +
                "\n\nTo measure similarity between ontology entities..... or " +
                "\nTo perform concept induction....." +
                "\nProgram runs in two mode. " +
                "\n\tBatch mode and " +
                "\n\tsingle mode. " +
                "\nIn single mode it will take a config file as input parameter and run the program as mentioned by the parameters in config file." +
                "\nIn Batch mode it take directory as input parameter and will run all the config files within that directory." +
                "\nCommand:" +
                "\n\tFor single mode: [option] [config_file_path]" +
                "\n\tFor Batch mode:  [option] [-b directory_path]" +
                "\n" +
                "\n\tExample of Concept Induction command:" +
                "\n\tFor single mode:" +
                "\n\t\t\tjava -jar ecii.jar -e config_file" +
                "\n\tFor Batch mode:" +
                "\n\t\t\tjava -jar ecii.jar -e -b directory";

        String helpCommandParameter = "";

        System.out.println(helpCommand);

    }


    private static boolean decideOp(String[] args) {

        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg + " ");
        }
        // measure similarity or concept induction by ecii
        if (args[0].equals("-m") || args[0].equals("-e")) {
            logger.debug("given program argument: " + sb.toString());
            if (args[0].equals("-m")) {
                logger.info("Program starting to measure similarity");
            } else {
                logger.info("Program starting to run concept induction");
            }
            if (args.length > 2) {
                /**
                 * args[0] = m or -e
                 * args[1] = -b
                 * args[2] = directory
                 */
                if (args.length > 3) {
                    logger.info("Intended option requires only 3 parameters but more given. Others are discarded.");
                }
                if (args[1].trim().toLowerCase().equals("-b") && !args[2].trim().endsWith(".config")) {
                    ConfigParams.batch = true;
                    ConfigParams.batchStartingPath = args[2];
                    // start with args[1])
                    try {
                        logger.info("Running on folder: " + args[2]);
                        processBatchRunning(args[2]);
                    } catch (Exception e) {
                        logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
                        if (null != monitor) {
                            monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
                        } else {
                            System.exit(0);
                        }
                    }
                } else {
                    System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                    printHelp();
                }
            } else {
                //  if (args.length == 2), this is always true for this decideOp function
                /* args[0] = m or -e
                 * args[1] = *.config  */
                if (args[1].endsWith(".config")) {
                    // parse the config file
                    ConfigParams.batch = false;
                    ConfigParams.parseConfigParams(args[1]);
                    initiateSingleDoOps(ConfigParams.outputResultPath);
                } else {
                    System.out.println("\nError!!! Config file must end with .config\n");
                    printHelp();
                }
            }
        } else if (args[0].equals("-c")) {
            if (args.length == 2) {
                combineOntologies(args[1], null);
            }
            if (args.length == 3) {
                combineOntologies(args[1], args[2]);
            } else {
                System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelp();
            }
        } else if (args[0].equals("-s")) {
            // strip down
            // -s [-obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI]
            if (args.length == 7) {
                if (args[1].equals("obj") || args[1].equals("type")) {
                    if (args[1].equals("obj")) {
                        stripDownOntoIndivsObjProps(args[2], args[3], args[4], args[5], args[6]);
                    } else {
                        stripDownOntoIndivsTypes(args[2], args[3], args[4], args[5], args[6]);
                    }
                } else {
                    System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                    printHelp();
                }
            } else {
                System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelp();
            }
        } else if (args[0].equals("-o")) {
            // -o [entityCsvFilePath, objPropColumnName, indivColumnName, typeColumnName, outputOntoIRI]"
            if (args.length == 6) {
                createOntologyFromCSV(args[1], args[2], args[3], args[4]);
            } else {
                System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelp();
            }
        } else {
            System.out.println(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
            printHelp();
            return false;
        }
        return true;
    }

    /**
     * https://github.com/google/guice/issues/1133
     * https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
     */
    public static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
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


        disableWarning();

        SharedDataHolder.programStartingDir = System.getProperty("user.dir");
        logger.info("Working directory/Program starting directory = " + SharedDataHolder.programStartingDir);

        logger.debug("args.length: " + args.length);

        if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].equals("-h"))
                    printHelp();
                else {
                    System.out.println(argErrorStr1 + " " + args[0] + " " + argErrorStr2);
                    printHelp();
                }
            } else {
                // args.length => 2
                decideOp(args);
            }
        } else {
            //if (args[0].equals("-h"))
            // args.length == 0
            System.out.println(argErrorNoArgGiven + "\n");
            printHelp();
        }
    }
}
