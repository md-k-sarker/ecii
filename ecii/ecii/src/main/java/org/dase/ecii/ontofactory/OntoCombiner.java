package org.dase.ecii.ontofactory;
/*
Written by sarker.
Written at 5/22/18.
*/

import org.apache.commons.csv.CSVParser;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
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

    /* Folder name is the key and set< of .owl file> (annotations files) are the value.*/
    private HashMap<String, HashSet<String>> fileBrowserMapping = new HashMap<>();

    public String outputOntoIRIString = "";
    /**
     * Instance level owlAxioms holder.
     * For a single instance this variable will store all the owlAxioms need to combined.
     */
    private HashSet<OWLAxiom> owlAxioms;

    /**
     * private empty construcor
     */
    public OntoCombiner(Monitor monitor) {
        this(null, monitor);
    }

    /**
     * public constructor
     *
     * @param outputOntoIRIString
     */
    public OntoCombiner(String outputOntoIRIString, Monitor monitor) {
        this.outputOntoIRIString = outputOntoIRIString;
        this.owlAxioms = new HashSet<>();

        // often monitor is null for ontocombiner
        monitor = monitor;
    }

    /**
     * Add axioms to the instance level variable
     * private HashSet<OWLAxiom> owlAxioms
     *
     * @param newAxioms
     * @return
     */
    public long addAxioms(HashSet<OWLAxiom> newAxioms) {

        if (null != owlAxioms) {
            owlAxioms.addAll(newAxioms);
        } else {
            logger.error("base owlAxioms is null.");
        }
        return owlAxioms.size();
    }

    /**
     * save ontology to disk
     *
     * @param path
     * @param iri
     */
    public void saveOntology(String path, String iri) {
        IRI ontoIRI = IRI.create(iri);
        saveOntology(path, ontoIRI);
    }

    /**
     * Create an ontology using all the axioms in owlAxioms.
     *
     * @param savingPath
     * @param ontologyIRI
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
     * save ontology to disk
     *
     * @param owlAxioms
     * @param path
     * @param iri
     */
    public void saveOntology(HashSet<OWLAxiom> owlAxioms, String path, String iri) {
        IRI ontoIRI = IRI.create(iri);
        saveOntology(owlAxioms, path, ontoIRI);
    }

    /**
     * Create an ontology using all the axioms in owlAxioms. and save it to disk
     *
     * @param owlAxioms
     * @param savingPath
     * @param ontologyIRI
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

    /**
     * create hierarchy and store the data in fileBrowserMapping variable
     *
     * @param traversingRootPath
     */
    private void createFileHierarchy(String traversingRootPath) {
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
    public void process_image_names_from_csv(String csvPath, String columnName) {

        logger.info("Processing csv file: " + csvPath);
        CSVParser csvRecords = Utility.parseCSV(csvPath, true);

        csvRecords.forEach(strings -> {
            String each_image_file_name = strings.get(columnName);

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
     * @param filePath
     */
    private void checkUserInputForFileOverWriting(String filePath) {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String answer = scanner.next();
            if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
                break;
            } else {
                System.out.println("File " + filePath + " already exist. Program exiting");
                System.exit(0);
            }
        }
    }

    /**
     * @param outputPath
     * @param inputDirPath
     */
    public void combineOntologies(String outputPath, String inputDirPath) {
        try {
            if (null == outputPath) {
                outputPath = Paths.get(inputDirPath).toString() + "/combined.owl";
            }
            if (new File(outputPath).exists()) {
                System.out.println("File " + outputPath + " already exist. If you want to overwrite this file write yes to continue");
                checkUserInputForFileOverWriting(outputPath);
            }
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
     * This function takes the raw entity names (images names of ADE20K or text entity names for ifp) which are written in csv file.
     * Then search for the corresponding owl files matching the raw entity names and create hashmap of the owlfilespath
     * <p>
     * Then it just call the combineOntologies(outputPath, owl_files_path) to combine the ontologies
     *
     * @param outputPath
     * @param traversingRootPath
     * @param csvPath
     * @param csvColumnName
     * @param useFileNameExtender
     * @param fileNameExtender
     */
    public void combineOntologiesBySearchingFilesFromCSV(String outputPath, String traversingRootPath, String csvPath, String csvColumnName, String fileExtensionToSearch, boolean useFileNameExtender, String fileNameExtender) {
        try {

            logger.info("Processing combineOntologies started...............");
            if (null == outputPath) {
                logger.error("Error!!!!!! Can't combine ontology as output path to save ontology is null");
                monitor.stopSystem("Error!!!!!! Can't combine ontology as output path to " +
                        "save ontology is null, program exiting.", true);
                return;
            }
            if (new File(outputPath).isDirectory()) {
                logger.info("Given outputPath is a directory. Output will be saved as combined.owl on the \n\t" + outputPath + " directory");
                outputPath = outputPath + "/combined.owl";

                if (new File(outputPath).exists()) {
                    System.out.println("File " + outputPath + " already exist. If you want to overwrite this file write yes to continue");
                    checkUserInputForFileOverWriting(outputPath);
                }
            }
            HashSet<String> owl_files_path = new HashSet<>();

            // create file hierarchy map. key is the file name and value is the full path
            HashMap<String, String> all_image_files_path = new HashMap<>();
            Files.walk(Paths.get(traversingRootPath))
                    .filter(f -> f.toFile().isFile() && f.toString().endsWith(fileExtensionToSearch)).
                    forEach(f -> all_image_files_path.put(f.getFileName().toString(), f.toString()));

            CSVParser csvRecords = Utility.parseCSV(csvPath, true);

            csvRecords.forEach(strings -> {
                String each_image_file_name = strings.get(csvColumnName);
                if (all_image_files_path.containsKey(each_image_file_name)) {

                    String each_image_file_path = all_image_files_path.get(each_image_file_name);

                    String each_image_file_path_without_extension = each_image_file_path.replace(fileExtensionToSearch, "");

                    if (useFileNameExtender) {
                        each_image_file_path_without_extension = each_image_file_path_without_extension + fileNameExtender;
                    }
                    String each_owl_file_path = each_image_file_path_without_extension + ".owl";

                    owl_files_path.add(each_owl_file_path);
                } else {
                    logger.error(" Error!!!!!!!!!!. file "+ each_image_file_name +" not found in " + traversingRootPath);
                }
            });

            System.out.println("Total owl files found: " + owl_files_path.size());

            System.out.println("Calling the next step ontocombiner.......");
            combineOntologies(outputPath, owl_files_path);

        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception!!!!!!!!!" + Utility.getStackTraceAsString(e));
        }
    }

    /**
     *
     */
    String outputOntoIRIString_ = "";

    /**
     * @param outputPath
     * @param inputOntoPaths
     */
    public void combineOntologies(String outputPath, HashSet<String> inputOntoPaths) {

        if (null == outputPath) {
            logger.error("Error!!!!!! Can't combine ontology as output path to save ontology is null");
            monitor.stopSystem("Error!!!!!! Can't combine ontology as output path to " +
                    "save ontology is null, program exiting.", true);
            return;
        }

        owlAxioms = new HashSet<>();

        // load each onto.
        inputOntoPaths.forEach(ontofile -> {
            try {
                OWLOntology ontology = Utility.loadOntology(ontofile);
                logger.info("Adding axioms from : " + ontofile);
                if (ontology.getOntologyID().getOntologyIRI().isPresent()) {
                    outputOntoIRIString_ = ontology.getOntologyID().getOntologyIRI().get().toString();
                }
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
        if (null != this.outputOntoIRIString) {
            saveOntology(outputPath, outputOntoIRIString);
        } else {
            if (null != outputOntoIRIString_) {
                saveOntology(outputPath, this.outputOntoIRIString_);
            } else {
                logger.error("Error!!!!!! Can't save ontology as ontology iri is null");
            }
        }
        logger.info("\nSaving ontology at: " + outputPath + " successfull.");
    }


    /**
     * Iterate over folders/files to combine ontologies.
     */
    private void combineSumoOntoswithADE20K(String sumoOntoPath) {
        try {

            logger.info("combineSumoOntoswithADE20K started.............");

            createFileHierarchy(traversingRootPath);

//            printFileHierarchy();

            process_image_names_from_csv(csvPath1, "filename");
            process_image_names_from_csv(csvPath2, "filename");

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

            logger.info("combineSumoOntoswithADE20K finished.");
        } catch (Exception ex) {
            logger.error("!!!!!!!!!!!!Exception!!!!!!!!!!!!");
            logger.error(Utility.getStackTraceAsString(ex));
            monitor.stopSystem("Stopping program", true);
        }
    }


    // test different functionalities
    private static String OntoCombinerLogPath = "Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/log_sumo_combining.log";
    private static String sumoPath = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/SUMO_properly_named.owl";
    private static String OntoCombinerSavingPath = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/SUMO_combined.owl";
    private static String traversingRootPath = "/Users/sarker/Workspaces/Jetbrains/emerald/data/ade20k_images_and_owls/";

    private static HashSet<String> smallOntoPaths = new HashSet<>();

    private static String csvPath1 = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/kitchen_vs_non-kitchen.csv";
    private static String csvPath2 = "/Users/sarker/Workspaces/Jetbrains/emerald/experiments/ade20k-sumo/livingRoom_vs_non-livingRoom.csv";

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

    public static void main(String[] args) {
//        String p1 = "/Users/sarker/Dropbox/Project_HCBD/Experiments/nesy-2017/";
//        String p2 = "/Users/sarker/Dropbox/Project_HCBD/Experiments/nesy-2017/sumo_with_wordnet.owl";
//
//        test1(p1, p2, ontoIRIString);


        OntoCombiner ontoCombiner = new OntoCombiner("http://www.daselab.com/residue/analysis", null);
        // combineOntologies(String outputPath, String traversingRootPath, String csvPath, String csvColumnName, boolean useFileNameExtender, String fileNameExtender)
        ontoCombiner.combineOntologiesBySearchingFilesFromCSV(
                "/Users/sarker/Workspaces/Jetbrains/residue-emerald/emerald/data/classification_data_by_srikanth/combined_small_owls_sumo_iri_untill_6_14_2020.owl",
                "/Users/sarker/Dropbox/Emerald-Tailor-Expr-Data/ade20k_images_and_owls",
                "/Users/sarker/Workspaces/Jetbrains/residue-emerald/emerald/data/classification_data_by_srikanth/images_untill_6_14_2020.csv",
                "image_names",
                ".jpg",
                false,
                "_wiki");

    }

}
