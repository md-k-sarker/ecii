package org.dase.ecii;


import org.apache.commons.io.FilenameUtils;
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
import java.util.*;

/**
 * Main class to initiate the different processes
 * 1. Concept Induction -e
 * 2. Measure similarity -m
 * 3. Combine Ontoloty -c
 * 4. Strip ontology -s
 * 5. Ontology creation -o
 */
public class Main {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //    private static OWLOntology ontology;
    //private static OWLOntologyManager manager;
    //private static OWLDataFactory dataFacotry;
    private static OWLReasoner owlReasoner;
    private static PrintStream outPutStream;
    private static Monitor monitor;

    private static JTextPane jTextPane;

    public static void setTextPane(JTextPane textPane) {
        jTextPane = textPane;
    }


    /**
     * clean the shared data holders.
     * should be called before starting the each induction operation.
     */
    private static void cleanSharedDataHolder() {
        SharedDataHolder.cleanSharedDataHolder();
    }

    /**
     * Initiate the start of a single operation
     * initiate the outputpath, logger path, monitor etc.
     *
     * @param outputResultPath
     */
    private static void initiateSingleOpsStart(String outputResultPath) {

        try {
            // file to write
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputResultPath));
            PrintStream printStream = new PrintStream(bos, true);
            outPutStream = printStream;

            monitor = new Monitor(outPutStream, jTextPane);
            monitor.start("Program started.............", true);
            logger.info("Program started................");
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
     * Initiate the end of a single operation.
     */
    private static void initiateSingleOpsEnd(String outputResultPath) {

        try {
            monitor.displayMessage("Result saved at: " + outputResultPath, true);
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
     * Initiate the outputpath, logger path, monitor etc and call doOps().
     *
     * @param outputResultPath
     */
    private static void doSingleConceptInductionM(String outputResultPath) {

        try {
            ConceptInductionM conceptInductionM = new ConceptInductionM(monitor);
            conceptInductionM.doOpsConceptInductionM();
        } catch (Exception e) {
            logger.info("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
            if (null != monitor) {
                monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
            } else {
                System.exit(0);
            }
        }
    }


    private static void processBatchRunning(String dirPath) {
        processBatchRunning(Paths.get(dirPath));
    }

    /**
     * Iterate over the folders and call doSingleConceptInductionM() for each confFile.
     *
     * @param dirPath
     */
    private static void processBatchRunning(Path dirPath) {

        try {
            // iterate over the files of a the folder
            Files.walk(dirPath).filter(f -> f.toFile().isFile())
                    .filter(f -> f.toFile().getAbsolutePath().endsWith(".config") && f.toFile().getAbsolutePath().contains("_sumo_bkg_"))
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
                            initiateSingleOpsStart(ConfigParams.outputResultPath);
                            doSingleConceptInductionM(ConfigParams.outputResultPath);
                            initiateSingleOpsEnd(ConfigParams.outputResultPath);
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
     * Use stripDownOntoIndivsObjProps() function instead of this one.
     * <p>
     * Strip down the input ontology and save it to disk.
     * Keep only the related axioms of given entity (got from csv file).
     * This function creates problem as
     * extractAxiomsRelatedToOWLClasses() only keeps classes which are subclass or superclass of someother class,
     * but not the direct subclass of owl:Thing, although that class may have instance.
     *
     * @param inputOntoPath
     * @param entityCsvFilePath
     * @param indivColumnName
     * @param typeColumnName
     * @param outputOntoIRI
     * @deprecated
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
     * Strip down the input ontology and save it to disk.
     * Keep only the related axioms of given entity (got from csv file).
     *
     * @param inputOntoPath
     * @param entityCsvFilePath
     * @param indivColumnName
     * @param objPropColumnName
     * @param outputOntoIRI
     */
    public static void stripDownOntoIndivsObjProps(String inputOntoPath, String entityCsvFilePath, String indivColumnName, String objPropColumnName, String outputOntoIRI) {

        String extension = FilenameUtils.getExtension(inputOntoPath);
        logger.info("extension: " + extension);
        String outputOntoPath = inputOntoPath.replace("." + extension, "_stripped." + extension);
        String outputLogPath = outputOntoPath.replace("." + extension, ".log");

        initiateSingleOpsStart(outputLogPath);

        System.out.println("inputOntoPath: " + inputOntoPath);
        monitor.displayMessage("File stripped started with inputOntoPath " + inputOntoPath + "........... ", true);

        StripDownOntology stripDownOntology = new StripDownOntology(inputOntoPath);

        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = stripDownOntology.
                readEntityFromCSVFile(entityCsvFilePath, objPropColumnName, indivColumnName);

        // process objprops and direct indivs
        ListofObjPropAndIndiv listofObjPropAndIndiv = stripDownOntology.
                convertToOntologyEntity(listofObjPropAndIndivTextualName);

        // process indirect indivs
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

            Utility.saveOntology(outputOntology, outputOntoPath);

            monitor.displayMessage("File stripped successfully and saved at: " + outputOntoPath, true);

        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

        initiateSingleOpsEnd(outputLogPath);
    }

    /**
     * Combine the ontologies.
     * It calls the ontocombiner and provide different arguments to do the different type of combining.
     *
     * @param inputOntologiesDirectory
     * @param outputOntologyIRI
     */
    public static void combineOntologies(String inputOntologiesDirectory, String outputOntologyIRI) {
        if (null == inputOntologiesDirectory) {
            logger.error("Error!!!!!! Input ontologies directory path can't be null");
            return;
        }
        OntoCombiner ontoCombiner = new OntoCombiner(outputOntologyIRI);
        ontoCombiner.combineOntologies(null, inputOntologiesDirectory);
    }

    /**
     * @param csvPath
     * @param indivColumnName
     * @param typeColumnName
     * @param ontoIRI
     * @param objPropColumnName
     */
    public static void createOntologyFromCSV(String csvPath, String indivColumnName, String typeColumnName, String ontoIRI, String delimeter, String objPropColumnName) {
        CreateOWLFromCSV createOWLFromCSV = null;
        logger.info("Creating ontology by processing csv file: " + csvPath + " started............");

        try {
            // "http://www.daselab.com/residue/analysis"
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), "talksAbout",
                    ontoIRI, delimeter);
        } catch (OWLOntologyCreationException e) {
            logger.error("Error in creating ontology: " + csvPath + " !!!!!!!!!!!!");
            e.printStackTrace();
        }

        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(indivColumnName, typeColumnName);
        logger.info("Creating ontology by processing csv file: " + csvPath + " finished.");
    }

    /**
     * @param csvPath
     * @param entityColumnName
     * @param usePrefixForIndivCreation
     * @param indivPrefix
     * @param assignTypeUsingSameEntity
     * @param ontoIRI
     */
    public static void createOntologyFromCSV(String csvPath, String entityColumnName, String usePrefixForIndivCreation, String indivPrefix, String assignTypeUsingSameEntity, String ontoIRI,String delimeter) {

        boolean usePrefixForIndivCreation_ = false;
        boolean assignTypeUsingSameEntity_ = false;

        if (usePrefixForIndivCreation.toString().toLowerCase().equals("true")) {
            usePrefixForIndivCreation_ = true;
        }
        if (assignTypeUsingSameEntity.toString().toLowerCase().equals("true")) {
            assignTypeUsingSameEntity_ = true;
        }
        CreateOWLFromCSV createOWLFromCSV = null;
        logger.info("Creating ontology by processing csv file: " + csvPath + " started............");

        try {
            // "http://www.daselab.com/residue/analysis"
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), ontoIRI, delimeter);
        } catch (OWLOntologyCreationException e) {
            logger.error("Error in creating ontology: " + csvPath + " !!!!!!!!!!!!");
            e.printStackTrace();
        }
//     public void parseCSVToCreateIndivAndTheirTypes(String entityColumnName, boolean usePrefixForIndivCreation, String indivPrefix, boolean assignTypeUsingSameEntity)

        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(entityColumnName, usePrefixForIndivCreation_, indivPrefix, assignTypeUsingSameEntity_);
        logger.info("Creating ontology by processing csv file: " + csvPath + " finished.");
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
                "\n\t-o [entityCsvFilePath, indivColumnName, typeColumnName, outputOntoIRI, delimeter, objPropColumnName]" +
                "\n\t\t-o [entityCsvFilePath, entityColumnName, usePrefixForIndivCreation, indivPrefix, assignTypeUsingSameEntity, ontoIRI, delimeter]" +
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

    /**
     * Decide the different operations by this program.
     * This should be called if the arguments start with -m, -e, -o, -s, -c
     *
     * @param args
     * @return
     */
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
                 * args[0] = -m or -e
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
                    initiateSingleOpsStart(ConfigParams.outputResultPath);
                    doSingleConceptInductionM(ConfigParams.outputResultPath);
                    initiateSingleOpsEnd(ConfigParams.outputResultPath);
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
            // -s [obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI]
            if (args.length == 7) {
                if (args[1].equals("obj") || args[1].equals("type")) {
                    if (args[1].equals("obj")) {
                        // this function is preferable instead of the indivTypes.
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
            // -o [entityCsvFilePath, indivColumnName, typeColumnName, outputOntoIRI, objPropColumnName]"
            if (args.length == 7) {
                createOntologyFromCSV(args[1], args[2], args[3], args[4], args[5], args[6]);
            } else if (args.length == 8) {
                createOntologyFromCSV(args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
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
