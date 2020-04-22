package org.dase.ecii.ontofactory;
/*
Written by sarker.
Written at 5/22/18.
*/

import org.apache.commons.csv.CSVParser;
import org.apache.commons.logging.impl.Log4JLogger;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Combine ontology.
 * Steps.
 * 1. Take all axioms from the original sumo ontology
 * 2. Iterate over the ontologies onto
 * 3. For each onto take axioms.
 * 4. Merge the axioms with original axioms.
 * 5.
 */
public class OntoCombiner {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Monitor monitor;
    private HashSet<OWLAxiom> owlAxioms = new HashSet<>();

//    private static HashSet<OWLAxiom> owlAxioms = new HashSet<>();

    /* Folder name is the key and set< of .owl file> (annotations files) are the value.*/
    private HashMap<String, HashSet<String>> fileBrowserMapping = new HashMap<>();
    //    private String outputOntoIRIString = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu";

    public String outputOntoIRIString = "";

    public OntoCombiner(String outputOntoIRIString) {
        this.outputOntoIRIString = outputOntoIRIString;
        this.owlAxioms = new HashSet<>();
    }


    public long addAxioms(HashSet<OWLAxiom> newAxioms) {

        if (null != owlAxioms) {
            owlAxioms.addAll(newAxioms);
        } else {
            logger.error("base owlAxioms is null.");
        }
        return owlAxioms.size();
    }

    public void saveOntology(String path, String iri) {
        IRI ontoIRI = IRI.create(iri);
        saveOntology(path, ontoIRI);
    }

    public void saveOntology(HashSet<OWLAxiom> owlAxioms, String path, String iri) {
        IRI ontoIRI = IRI.create(iri);
        saveOntology(owlAxioms, path, ontoIRI);
    }

    /**
     * Create an ontology using all the axioms in owlAxioms.
     */
    public void saveOntology(String savingPath, IRI ontologyIRI) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology ontology = manager.createOntology(owlAxioms, ontologyIRI);
            Utility.saveOntology(ontology, savingPath);
        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!OWLOntologyCreationException!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        }
    }

    /**
     * Create an ontology using all the axioms in owlAxioms.
     */
    public void saveOntology(HashSet<OWLAxiom> owlAxioms, String savingPath, IRI ontologyIRI) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology ontology = manager.createOntology(owlAxioms, ontologyIRI);
            Utility.saveOntology(ontology, savingPath);
        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!OWLOntologyCreationException!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        }
    }


    /**
     * Check whether the file structure is correct.
     */
    private void printFileHierarchy() {
        logger.info("############Printing files hierarchy###################");
        fileBrowserMapping.forEach((dir, files) -> {
            logger.info("\ndir: " + dir.toString());
            files.forEach(file -> {
                logger.info("\tfile:" + file.toString());
            });
        });
    }


    private void createFileHierarchy() {
        try {

            //Files.walk(Paths.get(traversingRootPath)).filter(d->)
            // actually we can simply start from any folder and traverse.
            // because we need only file names and traversing with a folder will give any files even the file is nested.


            try {
                logger.info("iterating started with " + traversingRootPath.toString());
                HashSet<String> files = Files.walk(Paths.get(traversingRootPath))
                        .filter(f -> f.toFile().isFile() && f.toString().endsWith(".jpg")).
                                map(f -> f.toString()).collect(Collectors.toCollection(HashSet::new));
                fileBrowserMapping.put(traversingRootPath.toString(), files);
            } catch (Exception ex) {
                logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
                logger.error(Utility.getStackTraceAsString(ex));
                monitor.stopSystem("Stopping program", true);
            }

        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        }
    }

    /**
     * @param csvPath
     */
    public void process_image_names_from_csv(String csvPath) {

        logger.info("Processing csv file: " + csvPath);
        CSVParser csvRecords = Utility.parseCSV(csvPath, true);
        String image_file_name = "filename";

        csvRecords.forEach(strings -> {
            String each_image_file_name = strings.get(image_file_name);

            for (Map.Entry<String, HashSet<String>> entry : fileBrowserMapping.entrySet()) {
                for (String eachFile : entry.getValue()) {
                    if (eachFile.contains(each_image_file_name)) {
                        String each_owl_file_name = eachFile.replace(".jpg", ".owl");
                        smallOntoPaths.add(each_owl_file_name);
                    }
                }
            }
        });
        logger.info("Processing csv file: " + csvPath + " finished");
        logger.info("\t total small owl files now: " + smallOntoPaths.size());
    }

    private static void printSmallOntoFileNames() {
        logger.info("############Printing small Onto File Names###################");
        smallOntoPaths.forEach(ontofile -> {
            logger.info("\tonto file: " + ontofile.toString());
        });
        logger.info("############Printing small Onto File Names finished.");
    }


    /**
     * Iterate over folders/files to combine ontologies.
     */
    private void doOps(String sumoOntoPath) {
        try {

            logger.info("doOps started.............");

            createFileHierarchy();

//            printFileHierarchy();

            process_image_names_from_csv(csvPath1);
            process_image_names_from_csv(csvPath2);

            printSmallOntoFileNames();

            // load original sumo
            OWLOntology _ontology = Utility.loadOntology(sumoOntoPath, monitor);
            HashSet<OWLAxiom> _axioms = _ontology.getAxioms().stream().collect(Collectors.toCollection(HashSet::new));
            addAxioms(_axioms);

            // load each small onto.
            smallOntoPaths.forEach(ontofile -> {
                try {

                    OWLOntology ontology = Utility.loadOntology(ontofile, monitor);
                    logger.info("Adding axioms from : " + ontofile.toString());
                    HashSet<OWLAxiom> axioms = ontology.getAxioms().stream().collect(Collectors.toCollection(HashSet::new));
                    addAxioms(axioms);
                    logger.info("axioms size now: " + owlAxioms.size());
                } catch (Exception ex) {
                    logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
                    logger.error(Utility.getStackTraceAsString(ex));
                    monitor.stopSystem("Stopping program", true);
                }
            });

            logger.info("\nSaving ontology at: " + OntoCombinerSavingPath);
            saveOntology(OntoCombinerSavingPath, outputOntoIRIString);

            logger.info("doOps finished.");
        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        }
    }


    private void combineAllSmallOntoFromADE20KWithSumo(String ade2krootPath, String OntoCombinerSavingPath, String sumoPath) {
        try {
            String[] paths = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "y", "z"};
            for (int i = 0; i < paths.length; i++) {

                if (i > 0) {
                    traversingRootPath = ade2krootPath.replace("/" + paths[i - 1] + "/", "/" + paths[i] + "/");

                    OntoCombinerSavingPath = OntoCombinerSavingPath.replace("_" + paths[i - 1] + ".owl", "_" + paths[i] + ".owl");

                    OntoCombinerLogPath = OntoCombinerLogPath.replace("_" + paths[i - 1] + ".log", "_" + paths[i] + ".log");
                } else {

                }
                owlAxioms = new HashSet<>();
                fileBrowserMapping = new HashMap<>();

                logger.info("Program started.............");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(OntoCombinerLogPath));
                PrintStream printStream = new PrintStream(bos, true);
                monitor = new Monitor(printStream);


                System.out.println("traversingRootPath: " + traversingRootPath);
                System.out.println("OntoCombinerSavingPath: " + OntoCombinerSavingPath);
                System.out.println("OntoCombinerLogPath: " + OntoCombinerLogPath);
                Log4JLogger l = new Log4JLogger();

                doOps(sumoPath);

                printStream.close();
            }

        } catch (FileNotFoundException ex) {
            logger.error("!!!!!!!!!!!!OWLOntologyCreationException!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        } finally {
            monitor.stopSystem("Program finished", true);
        }
    }


    private void test1(String traversingRootPath, String ontoCombinerSavingPath, String ontoIRIString) {


        HashSet<OWLAxiom> allAxioms = new HashSet<>();
        try {
            Files.walk(Paths.get(traversingRootPath)).filter(f -> f.toString().endsWith(".owl")).forEach(ontoFile -> {
                try {
                    OWLOntology ontology = Utility.loadOntology(ontoFile, monitor);
                    logger.info("Adding axioms from : " + ontoFile.toString());
                    HashSet<OWLAxiom> axioms = ontology.getAxioms().stream().collect(Collectors.toCollection(HashSet::new));
                    allAxioms.addAll(axioms);

                    logger.info("axioms size now: " + allAxioms.size());
                } catch (Exception ex) {
                    logger.error("!!!!!!!!!!!!!!");
                    ex.printStackTrace();
                }
            });
        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!!!");
            ex.printStackTrace();
        }

        logger.info("\nSaving ontology at: " + ontoCombinerSavingPath);
        saveOntology(allAxioms, ontoCombinerSavingPath, ontoIRIString);

    }


    private static String OntoCombinerLogPath = "Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/log_sumo_combining.log";
    private static String sumoPath = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/SUMO_properly_named.owl";
    private static String OntoCombinerSavingPath = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/SUMO_combined.owl";
    private static String traversingRootPath = "/Users/sarker/Workspaces/Jetbrains/emerald/data/ade20k_images_and_owls/";

    private static HashSet<String> smallOntoPaths = new HashSet<>();

    private static String csvPath1 = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/kitchen_vs_non-kitchen.csv";
    private static String csvPath2 = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/livingRoom_vs_non-livingRoom.csv";


    /**
     * @param outputPath
     * @param inputDirPath
     */
    public void combineOntologies(String outputPath, String inputDirPath) {
        try {
            File inputDirPathAsFile = new File(inputDirPath);
            logger.info("Processing combineOntologies started...............");
            if (inputDirPathAsFile.isDirectory()) {
                HashSet<String> inputOntoPaths = new HashSet<>();
                Files.walk(Paths.get(inputDirPath))
                        .filter(path -> path.toFile().isFile() && path.toString().endsWith(".owl")
                                || path.toFile().isFile() && path.toString().endsWith(".rdf"))
                        .forEach(path -> {
                            inputOntoPaths.add(path.toString());
                            logger.info("Adding " + path + " to inputOntoPaths hashSet for further processing... ");
                        });
                logger.info("inputOntoPaths size: " + inputOntoPaths.size());
                combineOntologies(outputPath, inputOntoPaths);
            } else {
                logger.error(" Error!!!!!!!!!!. inputDirPath " + inputDirPath + " is not a directory");
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception!!!!!!!!!" + Utility.getStackTraceAsString(e));
        }
    }

    /**
     * @param outputPath
     * @param inputOntoPaths
     */
    public void combineOntologies(String outputPath, HashSet<String> inputOntoPaths) {
        owlAxioms = new HashSet<>();

        // load each onto.
        inputOntoPaths.forEach(ontofile -> {
            try {
                OWLOntology ontology = Utility.loadOntology(ontofile);
                logger.info("Adding axioms from : " + ontofile);
                HashSet<OWLAxiom> axioms = ontology.getAxioms().stream().collect(Collectors.toCollection(HashSet::new));
                addAxioms(axioms);
                logger.info("axioms size now: " + owlAxioms.size());
            } catch (Exception ex) {
                logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
                logger.error(Utility.getStackTraceAsString(ex));
                monitor.stopSystem("Stopping program", true);
            }
        });

        logger.info("\nSaving ontology at: " + outputPath);
        saveOntology(outputPath, this.outputOntoIRIString);
        logger.info("\nSaving ontology at: " + outputPath + " successfull.");
    }

    public static void main(String[] args) {
//        String p1 = "/Users/sarker/Dropbox/Project_HCBD/Experiments/nesy-2017/";
//        String p2 = "/Users/sarker/Dropbox/Project_HCBD/Experiments/nesy-2017/sumo_with_wordnet.owl";
//
//        test1(p1, p2, ontoIRIString);
        OntoCombiner ontoCombiner = new OntoCombiner("http://www.daselab.com/residue/analysis");
        ontoCombiner.combineOntologies(
                "/Users/sarker/Workspaces/Jetbrains/residue/data/7_IFPs/Entities/7_ifp_combined_with_wiki_V0.owl",
                "/Users/sarker/Workspaces/Jetbrains/residue/data/7_IFPs/Entities");


    }

}
