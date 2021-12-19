package org.dase.ecii;


import org.apache.commons.io.FilenameUtils;
import org.dase.ecii.core.ConceptInductionM;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.ontofactory.OntoCombiner;
import org.dase.ecii.ontofactory.create.CreateOWLFromCSV;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndivTextualName;
import org.dase.ecii.ontofactory.strip.StripDownOntology;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import sun.misc.Unsafe;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
            if (null != outputResultPath) {
                // file to write
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputResultPath));
                PrintStream printStream = new PrintStream(bos, true);
                outPutStream = printStream;
            }
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
            if (null != outputResultPath) {
                monitor.displayMessage("Result saved at: " + outputResultPath, true);
            }
            monitor.stop(System.lineSeparator() + "Program finished.", true);
            logger.info("Program finished.");

            if (null != outPutStream) {
                outPutStream.close();
            }
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
     * Call function to do concept induction and possibly measure pariwise similarity
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


    /**
     * Call processBatchRunning by changing String dirPath to path dirPath
     *
     * @param dirPath
     * @param runPairwiseSimilarity
     */
    private static void processBatchRunning(String dirPath, boolean runPairwiseSimilarity) {
        processBatchRunning(Paths.get(dirPath), runPairwiseSimilarity);
    }

    /**
     * Iterate over the folders and call doSingleConceptInductionM() for each confFile.
     *
     * @param dirPath
     */
    private static void processBatchRunning(Path dirPath, boolean runPairwiseSimilarity) {

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
                            ConfigParams.runPairwiseSimilarity = runPairwiseSimilarity;
                            // overridde the configParams settings
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

        initiateSingleOpsStart(null);
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
        initiateSingleOpsEnd(null);
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
        logger.debug("extension: " + extension);
        String outputOntoPath = inputOntoPath.replace("." + extension, "_stripped." + extension);
        String outputLogPath = outputOntoPath.replace("." + extension, ".log");

        initiateSingleOpsStart(outputLogPath);

        logger.info("inputOntoPath: " + inputOntoPath);
        monitor.displayMessage("File stripped started with inputOntoPath " + inputOntoPath + "........... ", true);

        StripDownOntology stripDownOntology = new StripDownOntology(inputOntoPath);

        // load csv first
        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = stripDownOntology.
                readEntityFromCSVFile(entityCsvFilePath, objPropColumnName, indivColumnName);

        if (null != listofObjPropAndIndivTextualName) {

            // load ontology related resources
            if (stripDownOntology.loadOntologyRelatedResources()) {

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
            }
        } else {
            logger.error("listofObjPropAndIndivTextualName is null, because coudn't read entity from csv, program exiting!!!");
            monitor.stopSystem("listofObjPropAndIndivTextualName is null, because coudn't read entity from csv, program exiting!!!", true);
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
        initiateSingleOpsStart(null);
        OntoCombiner ontoCombiner = new OntoCombiner(outputOntologyIRI, monitor);
        ontoCombiner.combineOntologies(null, inputOntologiesDirectory);
        initiateSingleOpsEnd(null);
    }


    /**
     * It take a root path to search files, file names are written in csv file,
     * then use some mechanism to change file names to match with owl file.
     *
     * @param outputPath
     * @param traversingRootPath
     * @param csvPath
     * @param csvColumnName
     * @param useFileNameExtender
     * @param fileNameExtender
     */
    public static void combineOntologiesBySearchingFilesFromCSV(String outputPath,
                                                                String traversingRootPath,
                                                                String csvPath, String csvColumnName,
                                                                String fileExtensionToSearch,
                                                                String useFileNameExtender, String fileNameExtender) {
        boolean shouldUseFileNameExtender = Boolean.parseBoolean(useFileNameExtender);

        if (null == traversingRootPath) {
            logger.error("Error!!!!!! Input ontologies directory path can't be null");
            return;
        }
        if (null == csvPath) {
            logger.error("Error!!!!!! csv path can't be null");
            return;
        }
        if (null == csvColumnName) {
            logger.error("Error!!!!!! csvColumnName can't be null");
            return;
        }
        if (null == fileExtensionToSearch) {
            logger.error("Error!!!!!! fileExtensionToSearch can't be null");
            return;
        }
        initiateSingleOpsStart(null);
        OntoCombiner ontoCombiner = new OntoCombiner(monitor);
        ontoCombiner.combineOntologiesBySearchingFilesFromCSV(outputPath,
                traversingRootPath,
                csvPath,
                csvColumnName,
                fileExtensionToSearch,
                shouldUseFileNameExtender,
                fileNameExtender);
        initiateSingleOpsEnd(null);

    }

    /**
     * Call functions to create ontology by using the entity names from csv
     *
     * @param csvPath
     * @param indivColumnName
     * @param typeColumnName
     * @param ontoIRI
     * @param providingEntityFullName, if false it will use  ontoIRI+entityName to generate full name
     * @param delimiter
     * @param objPropName
     */
    public static void createOntologyFromCSV(String csvPath, String indivColumnName, String typeColumnName,
                                             String usePrefixForIndivCreation, String indivPrefix,
                                             String ontoIRI, String providingEntityFullName, String delimiter, String objPropName) {

        initiateSingleOpsStart(null);
        boolean usePrefixForIndivCreation_ = false;
        boolean isProvidingEntityFullName = false;

        if (usePrefixForIndivCreation.toString().toLowerCase().equals("true")) {
            usePrefixForIndivCreation_ = true;
        }
        if (providingEntityFullName.toString().toLowerCase().equals("true")) {
            isProvidingEntityFullName = true;
        }
        CreateOWLFromCSV createOWLFromCSV = null;
        logger.info("Creating ontology by processing csv file: " + csvPath + " started............");

        try {
            // "http://www.daselab.com/residue/analysis"
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), objPropName,
                    ontoIRI, isProvidingEntityFullName, delimiter);
        } catch (OWLOntologyCreationException e) {
            logger.error("Error in creating ontology: " + csvPath + " !!!!!!!!!!!!");
            e.printStackTrace();
        }

        // params -- String indivColumnName, String typesColumnName, boolean usePrefixForIndiv, String indivPrefix
        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(indivColumnName, typeColumnName, usePrefixForIndivCreation_, indivPrefix);
        logger.info("Creating ontology by processing csv file: " + csvPath + " finished.");

        initiateSingleOpsEnd(null);
    }

    /**
     * // -o [entityCsvFilePath, rowIdColumnName, indivColumnName, separator,
     * //       usePrefixForIndivCreation, indivPrefix, ontoIRI,
     * //       providingEntityFullName, delimeter, objPropName]
     * for each row of the csv create an ontology.
     *
     * @param csvPath
     * @param rowIdColumnName
     * @param indivColumnName
     * @param separator
     * @param usePrefixForIndivCreation
     * @param indivPrefix
     * @param ontoIRI
     * @param providingEntityFullName
     * @param delimiter
     * @param objPropName
     */
    public static void createOntologyFromCSV(String csvPath, String rowIdColumnName, String indivColumnName, String separator,
                                             String usePrefixForIndivCreation, String indivPrefix,
                                             String ontoIRI, String providingEntityFullName, String delimiter, String objPropName) {

        initiateSingleOpsStart(null);
        boolean usePrefixForIndivCreation_ = false;
        boolean isProvidingEntityFullName = false;

        if (usePrefixForIndivCreation.toString().toLowerCase().equals("true")) {
            usePrefixForIndivCreation_ = true;
        }
        if (providingEntityFullName.toString().toLowerCase().equals("true")) {
            isProvidingEntityFullName = true;
        }
        CreateOWLFromCSV createOWLFromCSV = null;
        logger.info("Creating ontology by processing csv file: " + csvPath + " started............");

        try {
            // "http://www.daselab.com/residue/analysis"
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), true, objPropName,
                    ontoIRI, isProvidingEntityFullName, delimiter);
        } catch (OWLOntologyCreationException e) {
            logger.error("Error in creating ontology: " + csvPath + " !!!!!!!!!!!!");
            e.printStackTrace();
        }

        // params -- String indivColumnName, String typesColumnName, boolean usePrefixForIndiv, String indivPrefix
        // parseCSVToCreateOneIndivsForEachRow(String rowIdColumnName, String entityColumnName, String separator, boolean usePrefixForIndivCreation, String indivPrefix)
        createOWLFromCSV.parseCSVToCreateOneIndivsForEachRow(rowIdColumnName, indivColumnName, separator, usePrefixForIndivCreation_, indivPrefix);
        logger.info("Creating ontology by processing csv file: " + csvPath + " finished.");

        initiateSingleOpsEnd(null);
    }

    /**
     * Call functions to create ontology by using the entity names from csv
     *
     * @param csvPath
     * @param entityColumnName
     * @param usePrefixForIndivCreation
     * @param indivPrefix
     * @param assignTypeUsingSameEntity
     * @param ontoIRI
     * @param providingEntityFullName,  if false it will use  ontoIRI+entityName to generate full name
     * @param delimiter
     */
    public static void createOntologyFromCSV(String csvPath, String entityColumnName, String usePrefixForIndivCreation,
                                             String indivPrefix, String assignTypeUsingSameEntity,
                                             String ontoIRI, String providingEntityFullName, String delimiter) {

        initiateSingleOpsStart(null);
        boolean usePrefixForIndivCreation_ = false;
        boolean assignTypeUsingSameEntity_ = false;
        boolean isProvidingEntityFullName = false;

        if (usePrefixForIndivCreation.toString().toLowerCase().equals("true")) {
            usePrefixForIndivCreation_ = true;
        }
        if (assignTypeUsingSameEntity.toString().toLowerCase().equals("true")) {
            assignTypeUsingSameEntity_ = true;
        }
        if (providingEntityFullName.toString().toLowerCase().equals("true")) {
            isProvidingEntityFullName = true;
        }
        CreateOWLFromCSV createOWLFromCSV = null;
        logger.info("Creating ontology by processing csv file: " + csvPath + " started............");

        try {
            // "http://www.daselab.com/residue/analysis"
            createOWLFromCSV = new CreateOWLFromCSV(csvPath.toString(), ontoIRI, isProvidingEntityFullName, delimiter);
        } catch (OWLOntologyCreationException e) {
            logger.error("Error in creating ontology: " + csvPath + " !!!!!!!!!!!!");
            e.printStackTrace();
        }
        //     public void parseCSVToCreateIndivAndTheirTypes(String entityColumnName, boolean usePrefixForIndivCreation,
        //     String indivPrefix, boolean assignTypeUsingSameEntity)

        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(entityColumnName, usePrefixForIndivCreation_,
                indivPrefix, assignTypeUsingSameEntity_);
        logger.info("Creating ontology by processing csv file: " + csvPath + " finished.");
        initiateSingleOpsEnd(null);
    }


    /**
     * Process the configurations of the program.
     */
    private static void processConfigurations() {

    }

    static String argErrorStr1 = "Given parameter:";
    static String argErrorStr2 = "is not in correct format.";
    static String argErrorNoArgGiven = "No options and parameter is provided. You need to specify a option and correspoding parameters.";

    public static void printHelpCombine() {
        String helpCommand = "\nParameters for combine ontology:" +
                "\n\t-c [inputOntologiesDirectory, outputOntologyIRI]" +
                "\n\t\tor" +
                "\n\t-c [outputPath, traversingRootPath, csvPath, csvColumnName," +
                " fileExtensionToSearch, useFileNameExtender, fileNameExtender]" +
                "\n\n\tDetails of combining ontology: " +
                "https://github.com/md-k-sarker/ecii/wiki/Combine-Ontology";

        System.out.println(helpCommand);
    }

    public static void printHelpStrip() {
        String helpCommand = "\nParameters for stripping ontology:" +
                "\n\t-s [-obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI] " +
                "\n\n\tDetails of stripping ontology: " +
                "https://github.com/md-k-sarker/ecii/wiki/Strip-down-ontology";
        System.out.println(helpCommand);
    }

    public static void printHelpCreateOnto() {
        String helpCommand = "\nParameters for creating ontology:" +
                "\n\t-o [entityCsvFilePath, indivColumnName, typeColumnName, usePrefixForIndivCreation, indivPrefix, ontoIRI, " +
                "providingEntityFullName, delimiter, objPropName]" +
                "\n\t\tor" +
                "\n\t-o [entityCsvFilePath, entityColumnName, usePrefixForIndivCreation, indivPrefix, assignTypeUsingSameEntity, " +
                "ontoIRI, providingEntityFullName, delimiter]" +
                "\n\n\tDetails of stripping ontology: " +
                "https://github.com/md-k-sarker/ecii/wiki/Create-Ontology-or-Knowledge-Graph";

        System.out.println(helpCommand);
    }

    public static void printHelpConceptInduction() {
        String helpCommand = "\nParameters for concept induction:" +
                "\n\t-e [config_file_path]" +
                "\n\t\tor" +
                "\n\t-e [-b] [directory_path]" +
                "\n\n\tDetails of concept induction:" +
                "https://github.com/md-k-sarker/ecii/wiki/Contextual-data-analysis-using-ECII";

        System.out.println(helpCommand);
    }

    public static void printHelpSimilarityMeasure() {
        String helpCommand = "\nParameters for similarity measure by concept induction:" +
                "\n\t-m [config_file_path]" +
                "\n\t\tor" +
                "\n\t-m [-b] [directory_path]" +
                "\n\n\tDetails of similarity measure:" +
                "https://github.com/md-k-sarker/ecii/wiki/Contextual-data-analysis-using-ECII";

        System.out.println(helpCommand);
    }

    /**
     * Help for overall option
     */
    public static void printHelpOverAll() {
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
                "\n\t\t-c [outputPath, traversingRootPath, csvPath, csvColumnName, fileExtensionToSearch, useFileNameExtender, fileNameExtender]" +
                "\n\t-s [-obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI] " +
                "\n\t-o [entityCsvFilePath, indivColumnName, typeColumnName, usePrefixForIndivCreation, indivPrefix, ontoIRI, providingEntityFullName, delimiter, objPropName]" +
                "\n\t\t-o [entityCsvFilePath, entityColumnName, usePrefixForIndivCreation, indivPrefix, assignTypeUsingSameEntity, ontoIRI, providingEntityFullName, delimiter]" +
                "\n\nTo measure similarity between ontology entities..... or " +
                "\nTo perform concept induction....." +
                "\nProgram runs in two mode. " +
                "\n\tBatch mode or " +
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
                "\n\t\t\tjava -jar ecii.jar -e -b directory" +
                "\n\nConcept induction or similarity measure has many tuning parameters. Those are written in the configuration file." +
                "\nExample of configuration file can be seen in" +
                "https://github.com/md-k-sarker/ecii";

        String helpCommandParameter = "";

        System.out.println(helpCommand);

    }

    /**
     * Decide the different operations by this program.
     * This should be called if the arguments start with -m, -e, -o, -s, -c
     *
     * @param args
     * @return boolean
     */
    public static boolean decideOp(String[] args) {

        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg + " ");
        }
        // measure similarity or concept induction by ecii
        if (args[0].equalsIgnoreCase("-m") || args[0].equalsIgnoreCase("-e")) {
            logger.debug("given program argument: " + sb.toString());
            boolean runPairwiseSimilarity = false;
            if (args[0].equalsIgnoreCase("-m")) {
                logger.info("Program starting to measure similarity");
                runPairwiseSimilarity = true;
            } else {
                logger.info("Program starting to run concept induction");
                runPairwiseSimilarity = false;
            }

            if (args.length > 2) {
                /**
                 * args[0] = -m or -e
                 * args[1] = -b
                 * args[2] = directory
                 */
                if (args.length > 3) {
                    logger.warn("Intended option requires only 3 parameters but more given. First 3 are taken and others are discarded.");
                }

                if (args[1].trim().equalsIgnoreCase("-b") && !args[2].trim().endsWith(".config")) {
                    ConfigParams.batch = true;
                    ConfigParams.batchStartingPath = args[2];
                    // start with args[1])
                    try {
                        logger.info("Running on folder: " + args[2]);
                        processBatchRunning(args[2], runPairwiseSimilarity);
                    } catch (Exception e) {
                        logger.error("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e));
                        if (null != monitor) {
                            monitor.stopSystem("\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(e), true);
                        } else {
                            System.exit(0);
                        }
                    }
                } else {
                    logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                    printHelpConceptInduction();
                }
            } else {
                //  if (args.length == 2), this is always true for this decideOp function
                /* args[0] = m or -e
                 * args[1] = *.config  */
                if (args[1].endsWith(".config")) {
                    // parse the config file
                    ConfigParams.batch = false;
                    ConfigParams.parseConfigParams(args[1]);
                    // overridde the configParams settings
                    ConfigParams.runPairwiseSimilarity = runPairwiseSimilarity;
                    initiateSingleOpsStart(ConfigParams.outputResultPath);
                    doSingleConceptInductionM(ConfigParams.outputResultPath);
                    initiateSingleOpsEnd(ConfigParams.outputResultPath);
                } else {
                    logger.error("\nError!!! Config file must end with .config\n");
                    printHelpOverAll();
                }
            }
        } else if (args[0].equalsIgnoreCase("-c")) {
            logger.debug("given program argument: " + sb.toString());
            if (args.length == 2 || args.length == 3 || args.length == 8) {
                logger.info("Program starting to combine ontologies");
                if (args.length == 2) {
                    combineOntologies(args[1], null);
                }
                if (args.length == 3) {
                    combineOntologies(args[1], args[2]);
                }
                if (args.length == 8) {
                    combineOntologiesBySearchingFilesFromCSV(args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                }
            } else {
                logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelpCombine();
            }
        } else if (args[0].equalsIgnoreCase("-s")) {
            // strip down
            // -s [obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI]
            if (args.length == 7) {
                if (args[1].equals("-obj") || args[1].equals("-type")) {
                    logger.info("Program starting to strip/prune ontology entities");
                    if (args[1].equals("-obj")) {
                        // this function is preferable/recommended/should-use instead of the indivTypes.
                        stripDownOntoIndivsObjProps(args[2], args[3], args[4], args[5], args[6]);
                    } else {
                        stripDownOntoIndivsTypes(args[2], args[3], args[4], args[5], args[6]);
                    }
                } else {
                    logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                    printHelpOverAll();
                }
            } else {
                logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelpStrip();
            }
        } else if (args[0].equalsIgnoreCase("-o")) {
            // obj-prop: -o [entityCsvFilePath, indivColumnName, typeColumnName, usePrefixForIndivCreation, indivPrefix, ontoIRI, providingEntityFullName, delimiter, objPropName]
            // no-obj-prop: -o [entityCsvFilePath, entityColumnName, usePrefixForIndivCreation, indivPrefix, assignTypeUsingSameEntity, ontoIRI, providingEntityFullName, delimiter]
            if (args.length == 9 || args.length == 10 || args.length == 11) {
                logger.info("Program starting to create ontology");

                if (args.length == 9) {
                    // just indiv and type
                    // String csvPath, String entityColumnName, String usePrefixForIndivCreation, String indivPrefix, String assignTypeUsingSameEntity,
                    //                                             String ontoIRI, String providingEntityFullName, String delimiter
                    boolean usePrefixForIndivCreation = false;
                    createOntologyFromCSV(args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
                } else if (args.length == 10) {
                    // indiv, type and obj-prop
                    //String csvPath, String indivColumnName, String typeColumnName, String usePrefixForIndivCreation, String indivPrefix,
                    //                                             String ontoIRI, String providingEntityFullName, String delimiter, String objPropName
                    createOntologyFromCSV(args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
                } else {
                    // -o [entityCsvFilePath, rowIdColumnName, indivColumnName, separator,
                    //       usePrefixForIndivCreation, indivPrefix, ontoIRI,
                    //       providingEntityFullName, delimeter, objPropName]
                    // multiple ontology from a single csv file
                    createOntologyFromCSV(args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]);
                }
            } else {
                logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
                printHelpCreateOnto();
            }
        } else {
            logger.error(argErrorStr1 + " " + sb.toString() + " " + argErrorStr2);
            printHelpOverAll();
            return false;
        }
        return true;
    }

    /**
     * weird hack to disable the warnings
     * https://github.com/google/guice/issues/1133
     * https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
     */
    public static void disableWarning() {
        try {
//            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
//            theUnsafe.setAccessible(true);
//            Unsafe u = (Unsafe) theUnsafe.get(null);
//
//            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
//            Field logger = cls.getDeclaredField("logger");
//            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }


    /**
     * Main driver function for the ECII system.
     * This function captures the arguments, passes the arguments to decideOp or show help
     * ECII system can perform several data analysis operations like
     * "\n1. Measure similarity between ontology entities"
     * "\n2. Perform concept induction"
     * "\n3. Strip down ontology or keeping entities of interest while discarding others"
     * "\n4. Create ontology from CSV file"
     * "\n5. Combine multiple ontology"
     * so it is important to pass the arguments to appropriate functions, which is being done by decideOp() function
     *
     * @param args
     */
    public static void main(String[] args) {

        // weird hack to disable the warnings
        disableWarning();

        SharedDataHolder.programStartingDir = System.getProperty("user.dir");
        logger.info("Working directory/Program starting directory = " + SharedDataHolder.programStartingDir);

        logger.info("args.length: " + args.length);

        if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].equals("-h"))
                    //if (args[0].equals("-h"))
                    printHelpOverAll();
                else {
                    System.out.println(argErrorStr1 + " " + args[0] + " " + argErrorStr2);
                    printHelpOverAll();
                }
            } else {
                // args.length => 2
                if (args[0].matches("-m|-e|-o|-s|-c|-M|-E|-O|-S|-C")) {
                    decideOp(args);
                } else {
                    System.out.println(argErrorStr1 + " " + args[0] + " " + argErrorStr2);
                    printHelpOverAll();
                }
            }
        } else {
            // args.length == 0
            System.out.println(argErrorNoArgGiven + "\n");
            printHelpOverAll();
        }
    }
}
