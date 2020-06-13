package org.dase.ecii.ontofactory;
/*
Written by sarker.
Written at 3/7/20.
*/

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.PropertyConfigurator;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndivTextualName;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * Prune the ontology.
 * Input:
 * 1. Ontology, (possibly a large ontology, in our case the wikipedia hierarchy)
 * 2. Entities
 * 3. Object properties (in most case we do indiv1 imgContains obj1 and then we look for the type of obj1)
 * <p>
 * Output:
 * 1. Pruned ontology (will be saved on the given file location)
 * Invariant:
 * Input entities and their corresponding axioms (entity types and super types and associated obj properties) we need to keep
 */
public class StripDownOntology {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * axioms to keep
     */
    private static HashSet<OWLAxiom> axiomsToKeep = new HashSet<>();

    /**
     * Ontology manipulator
     */
    public static OWLOntology inputOntology;
    private static OWLOntologyManager inputOntoManager;
    //    private static OWLOntologyManager outputOntoManager;
    private static OWLDataFactory ontoDataFacotry;
//    private static OWLOntology outputOntology;

    /**
     * Entities/OWLNamedIndividuals we need. After parsing the text file, entities are converted from string to owl entity and saved to this data structure.
     */
//    private static HashSet<OWLNamedIndividual> referredDirectEntities = new HashSet<>();

    /**
     * Entities/OWLNamedIndividuals we need. These are the individuals like obj1 of this triple: indiv1 imgContains obj1
     */
//    private static HashSet<OWLNamedIndividual> referredIndirectEntities = new HashSet<>();
    public StripDownOntology(String inputOntoPath) {
        try {
            if (null != inputOntoPath) {
                logger.info("Initiating ontology related resources...............");
                long ontoLoadStartTime = System.currentTimeMillis();
                inputOntology = Utility.loadOntology(inputOntoPath);
                inputOntoManager = inputOntology.getOWLOntologyManager();
                ontoDataFacotry = inputOntoManager.getOWLDataFactory();

//                outputOntoManager = OWLManager.createOWLOntologyManager();
                logger.info("Initiating ontology related resources successfull");
                long ontoLoadEndTime = System.currentTimeMillis();
                logger.info("Onto load time: " + (ontoLoadEndTime - ontoLoadStartTime) / 1000 + " seconds");

            } else {
                logger.error("Initiating ontology related resources failed!!!!!!!!!!!!! because inputOntoPath is null");
            }
        } catch (Exception ex) {
            logger.error("Initiating ontology related resources failed!!!!!!!!!!!!!");
            ex.printStackTrace();
        }
    }


    /**
     * Read entity from csv file,
     *
     * @param csvFilePath
     * @param objPropColumnName: must remain on csv file
     * @param indivColumnName:   must remain on csv file
     * @return
     * @ exception: java.lang.IllegalArgumentException: Mapping for not found for any header name not found on csv file
     */
    public ListofObjPropAndIndivTextualName readEntityFromCSVFile(String csvFilePath, String objPropColumnName, String indivColumnName) {
        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = null;
        try {
            if (null != csvFilePath) {
                logger.info("readEntityFromCSVFile started..., input file name: " + csvFilePath);
                CSVParser csvRecords = Utility.parseCSV(csvFilePath, true);
                if (null != csvRecords) {
                    listofObjPropAndIndivTextualName = new ListofObjPropAndIndivTextualName();

                    for (CSVRecord strings : csvRecords) {
                        String objPropName = strings.get(objPropColumnName);
                        if (null != objPropName && objPropName.length() > 0) {
                            logger.info("ObjPropName: " + objPropName);
                            listofObjPropAndIndivTextualName.objPropNames.add(objPropName);
                        }
                        String indivName = strings.get(indivColumnName);
                        if (null != indivName && indivName.length() > 0) {
                            logger.info("indivName: " + indivName);
                            listofObjPropAndIndivTextualName.indivNames.add(indivName);
                        }
                    }
                }

                logger.info("readEntityFromCSVFile successfull.");
                logger.info("total namedIndividualsRawName: " + listofObjPropAndIndivTextualName.indivNames.size());
                logger.info("total objPropsRawName: " + listofObjPropAndIndivTextualName.objPropNames.size());
                return listofObjPropAndIndivTextualName;

            } else {
                logger.info("readEntityFromCSVFile starting failed!!!!!!!!!!!!! input entity file name is null!!!!!");
                return listofObjPropAndIndivTextualName;
            }
        } catch (Exception ex) {
            logger.info("readEntityFromCSVFile failed!!!! Reading entity file error, entity file name: " + entityTxtFilePath);
            ex.printStackTrace();
            return listofObjPropAndIndivTextualName;
        }
    }


    /**
     * @param csvFilePath
     * @param indivColumnName
     * @param typeColumnName
     * @return: HashMap<String, HashSet < String>>
     * @ exception: java.lang.IllegalArgumentException: Mapping for  not found for any header name not found on csv file
     */
    public HashMap<String, HashSet<String>> readIndivTypesFromCSVFile(String csvFilePath, String indivColumnName, String typeColumnName) {
        HashMap<String, HashSet<String>> hashSetHashMap = new HashMap<>();

        try {
            if (null != csvFilePath) {
                logger.info("readIndivTypesFromCSVFile started..., input file name: " + csvFilePath);
                CSVParser csvRecords = Utility.parseCSV(csvFilePath, true);

                String indivNameForDebug = "";

                if (null != csvRecords) {
                    hashSetHashMap = new HashMap<>();

                    for (CSVRecord strings : csvRecords) {

                        String indivName = strings.get(indivColumnName);
                        String classNames = "";
                        if (null != typeColumnName)
                            classNames = strings.get(typeColumnName);

                        if (null != indivName && indivName.length() > 0 && null != classNames && classNames.length() > 0) {
                            logger.info("indivName: " + indivName);
                            if (!hashSetHashMap.containsKey(indivName)) {
                                HashSet<String> owlClassNameHashSet = new HashSet<>();
                                hashSetHashMap.put(indivName, owlClassNameHashSet);
                            }

                            logger.info("classNames: " + classNames);
                            String[] classNamesArray = classNames.split(";");
                            for (String eachClassName : classNamesArray) {
                                logger.debug(" indiv " + indivName + " has type name: " + eachClassName);
                                hashSetHashMap.get(indivName).add(eachClassName);
                            }
                        }
                        indivNameForDebug = indivName;
                    }
                }

                logger.info("readIndivTypesFromCSVFile successfull.");
                logger.info("total namedIndividualsRawName: " + hashSetHashMap.size());
                logger.info("total types for namedIndividual: " + indivNameForDebug + ": " + hashSetHashMap.get(indivNameForDebug).size());
                return hashSetHashMap;

            } else {
                logger.info("readIndivTypesFromCSVFile starting failed!!!!!!!!!!!!! input entity file name is null!!!!!");
                return hashSetHashMap;
            }
        } catch (Exception ex) {
            logger.info("readIndivTypesFromCSVFile failed!!!! Reading entity file error, entity file name: " + csvFilePath);
            ex.printStackTrace();
            return hashSetHashMap;
        }
    }


    /**
     * Convert from text to ontology entity
     *
     * @param listofObjPropAndIndivTextualName
     * @return
     */
    public ListofObjPropAndIndiv convertToOntologyEntity(ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName) {

        ListofObjPropAndIndiv listofObjPropAndIndiv = null;

        logger.info("convertToOntologyEntity started..........");

        if (null != listofObjPropAndIndivTextualName) {

            listofObjPropAndIndiv = new ListofObjPropAndIndiv();

            // process individuals
            logger.info("processing direct individuals started..........");
            for (String str : listofObjPropAndIndivTextualName.indivNames) {
                IRI iri = IRI.create(str);
                logger.debug("iri: " + iri);
                OWLNamedIndividual owlNamedIndividual = ontoDataFacotry.getOWLNamedIndividual(iri);
                logger.debug("owlNamedIndividual: " + owlNamedIndividual);
                listofObjPropAndIndiv.directIndivs.add(owlNamedIndividual);
            }
            logger.info("processing direct individuals successfull.");
            logger.info("total directNamedIndividuals size: " + listofObjPropAndIndiv.directIndivs.size());

            // process owlobject properties
            logger.info("processing owlobject properties started..........");
            for (String str : listofObjPropAndIndivTextualName.objPropNames) {
                IRI iri = IRI.create(str);
                logger.debug("iri: " + iri);
                OWLObjectProperty owlObjectProperty = ontoDataFacotry.getOWLObjectProperty(iri);
                logger.debug("owlObjectProperty: " + owlObjectProperty);
                listofObjPropAndIndiv.objPropsofInterest.add(owlObjectProperty);
            }
            logger.info("processing owlobject properties successfull.");
            logger.info("total owlobject properties: " + listofObjPropAndIndiv.objPropsofInterest.size());

        } else {
            logger.info("convertToOntologyEntity failed because listofObjPropAndIndivTextualName is null ");
        }
        return listofObjPropAndIndiv;
    }

    /**
     * Convert from text to ontology entity
     *
     * @param indivAndTypesNameMap: HashMap<String,HashSet<String>>
     * @return
     */
    public HashMap<OWLNamedIndividual, HashSet<OWLClass>> convertToOntologyEntity(HashMap<String, HashSet<String>> indivAndTypesNameMap) {

        HashMap<OWLNamedIndividual, HashSet<OWLClass>> indivClassMap = null;

        logger.info("converting individuals and their types from text to owlentity started..........");

        if (null != indivAndTypesNameMap) {

            indivClassMap = new HashMap<OWLNamedIndividual, HashSet<OWLClass>>();

            HashSet<OWLClass> owlClassHashSet = null;

            // process individuals and their types
            logger.info("converting individuals from map started..........");
            for (Map.Entry<String, HashSet<String>> eachIndivAndTypesName : indivAndTypesNameMap.entrySet()) {

                if (null != eachIndivAndTypesName.getKey() && eachIndivAndTypesName.getKey().length() > 0) {
                    // process individuals
                    IRI iriIndi = IRI.create(eachIndivAndTypesName.getKey());
                    logger.debug("iriIndi: " + iriIndi);
                    OWLNamedIndividual owlNamedIndividual = ontoDataFacotry.getOWLNamedIndividual(iriIndi);
                    logger.debug("owlNamedIndividual: " + owlNamedIndividual);

                    if (!indivClassMap.containsKey(owlNamedIndividual)) {
                        owlClassHashSet = new HashSet<>();
                        indivClassMap.put(owlNamedIndividual, owlClassHashSet);
                    }
                    // if we already have this owlNamedIndividual just skip it.

                    if (null != eachIndivAndTypesName.getValue() && eachIndivAndTypesName.getValue().size() > 0) {
                        // process classes
                        for (String eachClassName : eachIndivAndTypesName.getValue()) {
                            if (null != eachClassName) {

                                IRI iriClass = IRI.create(eachClassName);
                                logger.debug("iriClass: " + iriClass);
                                OWLClass owlClass = ontoDataFacotry.getOWLClass(iriClass);
                                logger.debug("owlClass: " + owlClass);

                                indivClassMap.get(owlNamedIndividual).add(owlClass);
                            }
                        }
                    }
                }
            }
        } else {
            logger.error("converting individuals and their types from text to owlentity failed because indivAndTypesNameMap is null");
            return null;
        }

        logger.info("converting individuals and their types from text to owlentity successfull.");
        return indivClassMap;
    }


    /**
     * Create ontology individual from textual names
     *
     * @return
     */
//    private static boolean readTextFileAndConvertToOntologyEntity(String entityTxtFilePath) {
//        try {
//            if (null != entityTxtFilePath) {
//                logger.info("readTextFileAndConvertToOntologyEntity started..., input file name: " + entityTxtFilePath);
//                Files.readAllLines(Paths.get(entityTxtFilePath)).forEach(txtEntity -> {
//                    IRI iri = IRI.create(txtEntity);
//                    logger.debug("iri: " + iri);
//                    // create ontology namedindividual
//                    OWLNamedIndividual owlNamedIndividual = ontoDataFacotry.getOWLNamedIndividual(iri);
//                    logger.debug("owlNamedIndividual: " + owlNamedIndividual);
//                    referredDirectEntities.add(owlNamedIndividual);
//                });
//
//                logger.info("readTextFileAndConvertToOntologyEntity successfull.");
//                logger.info("total namedIndividuals: " + referredDirectEntities.size());
//                return true;
//            } else {
//                logger.info("readTextFileAndConvertToOntologyEntity starting failed!!!!!!!!!!!!! input entity file name is null!!!!!");
//                return false;
//            }
//        } catch (Exception ex) {
//            logger.info("readTextFileAndConvertToOntologyEntity failed!!!! Reading entity file error, entity file name: " + entityTxtFilePath);
//            ex.printStackTrace();
//            return false;
//        }
//    }

    /**
     * Process/search indirect indivs from the ontology.
     *
     * @param listofObjPropAndIndiv
     * @return the same object which was sent as parameter
     */
    public ListofObjPropAndIndiv processIndirectIndivsUsingObjProps(ListofObjPropAndIndiv listofObjPropAndIndiv) {
        if (null != inputOntology) {
            if (null != listofObjPropAndIndiv) {
                // process indirect indivs
                logger.info("processing indirectIndivs started........... ");
                for (OWLObjectProperty owlObjectProperty : listofObjPropAndIndiv.objPropsofInterest) {
                    logger.info("Processing object property " + owlObjectProperty);
                    for (OWLNamedIndividual owlNamedIndividual : listofObjPropAndIndiv.directIndivs) {
                        logger.debug("\t Processing object property with indiv " + owlNamedIndividual);
                        Collection<OWLIndividual> individuals = EntitySearcher.
                                getObjectPropertyValues(owlNamedIndividual, owlObjectProperty, inputOntology);
                        logger.debug("\t #Triple found indiv------object_property " + individuals.size());
                        for (OWLIndividual owlIndividual : individuals) {
                            if (owlIndividual instanceof OWLNamedIndividual) {
                                listofObjPropAndIndiv.inDirectIndivs.add((OWLNamedIndividual) owlIndividual);
                            }
                        }
                    }
                }
                logger.info("processing indirectIndivs finished successfully.");
                logger.info("total indirectNamedIndividuals size: " + listofObjPropAndIndiv.inDirectIndivs.size());
            } else {
                logger.error("processing indirectIndivs failed!!!!!!!!!!!!! because parameter listofObjPropAndIndiv is null.");
            }
        } else {
            logger.error("processing indirectIndivs failed!!!!!!!!!! because parameter inputOntology is null.");
        }
        return listofObjPropAndIndiv;
    }

    /**
     * @param owlObjectPropertiesOfInterest
     * @return
     */
//    private static boolean processObjectProperties(HashSet<OWLObjectProperty> owlObjectPropertiesOfInterest) {
//        try {
//            if (null != owlObjectPropertiesOfInterest) {
//                logger.info("processObjectProperties started.........");
//                logger.info("owlObjectPropertiesOfInterest size: " + owlObjectPropertiesOfInterest.size());
//
//                if (null != referredDirectEntities) {
//                    logger.info("referredDirectEntities size: " + referredDirectEntities.size());
//
//                    owlObjectPropertiesOfInterest.forEach(owlObjectProperty -> {
//                        logger.info("Processing object property " + owlObjectProperty);
//                        referredDirectEntities.forEach(owlNamedIndividual -> {
//                            logger.debug("\t Processing object property with indiv " + owlNamedIndividual);
//                            Collection<OWLIndividual> individuals = EntitySearcher.getObjectPropertyValues(owlNamedIndividual, owlObjectProperty, inputOntology);
//                            logger.debug("\t #Triple found indiv------object_property " + individuals.size());
//                            individuals.stream().filter(OWLNamedIndividual.class::isInstance).forEach(owlIndividual -> {
//                                referredIndirectEntities.add((OWLNamedIndividual) owlIndividual);
//                            });
//                        });
//                    });
//
//                    logger.info("processObjectProperties finished successfully.");
//                    logger.info("referredIndirectEntities size: " + referredDirectEntities.size());
//
//                    return true;
//                } else {
//                    logger.info("processObjectProperties could not start as referredDirectEntities is null.");
//                    return false;
//                }
//            } else {
//                logger.info("processObjectProperties could not start as owlObjectPropertiesOfInterest is null.");
//                return false;
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return false;
//        }
//    }

    static int reccounter = 0;
    static int indirectIndiCounter = 0;

    /**
     * // supertypes until we find owl:Thing
     *
     * @param owlClass
     */
    private void findSuperTypesRecursive(OWLClass owlClass) {
//        reccounter++;
////        if (reccounter > 100000) {
////            System.exit(-1);
////        }
        findSuperTypesRecursive(owlClass, axiomsToKeep);
//        logger.info("indirectIndiCounter: " + indirectIndiCounter + " \t reccounter: " + reccounter);
//        logger.info("Counter " + reccounter + " ### findSuperTypesRecursive started with: " + owlClass);
//
//        if (ontoDataFacotry.getOWLThing().equals(owlClass))
//            return;
//
//        // initial call
//        Collection<OWLClassExpression> superClasses = EntitySearcher.
//                getSuperClasses(owlClass, inputOntology);
//
//        logger.info("### findSuperTypesRecursive running with superClasses.size: " + superClasses.size());
//
//        if (superClasses.size() < 1)
//            return;
//
//        for (OWLClassExpression owlClassExpression : superClasses) {
//
//            logger.info("### findSuperTypesRecursive running with super-owlClassExpression: " + owlClassExpression);
//
//            if (owlClassExpression instanceof OWLClass) {
//                OWLClass owlClassNew = (OWLClass) owlClassExpression;
//                if (!owlClassNew.equals(owlClass)) {
//                    axiomsToKeep.addAll(inputOntology.getAxioms(owlClassNew));
//                    findSuperTypesRecursive(owlClassNew);
//                }
//            }
//        }
//
//        logger.info("### findSuperTypesRecursive finished with: " + owlClass);
    }

    /**
     * // supertypes until we find owl:Thing
     *
     * @param owlClass
     */
    private void findSuperTypesRecursive(OWLClass owlClass, HashSet<OWLAxiom> owlAxiomsToKeep) {

        reccounter++;
//        if (reccounter > 100000) {
//            System.exit(-1);
//        }
        logger.info("indirectIndiCounter: " + indirectIndiCounter + " \t reccounter: " + reccounter);
        logger.info("Counter " + reccounter + " ### findSuperTypesRecursive started with: " + owlClass);

        if (ontoDataFacotry.getOWLThing().equals(owlClass))
            return;

        // initial call
        Collection<OWLClassExpression> superClasses = EntitySearcher.
                getSuperClasses(owlClass, inputOntology);

        logger.info("### findSuperTypesRecursive running with superClasses.size: " + superClasses.size());

        if (superClasses.size() < 1)
            return;

        for (OWLClassExpression owlClassExpression : superClasses) {

            logger.info("### findSuperTypesRecursive running with super-owlClassExpression: " + owlClassExpression);

            if (owlClassExpression instanceof OWLClass) {
                OWLClass owlClassNew = (OWLClass) owlClassExpression;
                if (!owlClassNew.equals(owlClass)) {
                    owlAxiomsToKeep.addAll(inputOntology.getAxioms(owlClassNew));
                    findSuperTypesRecursive(owlClassNew, owlAxiomsToKeep);
                }
            }
        }

        logger.info("### findSuperTypesRecursive finished with: " + owlClass);
    }


    /**
     * @param owlNamedIndividual
     * @return
     */
    private boolean extractAxiomsRelatedToIndiv(OWLNamedIndividual owlNamedIndividual) {
        try {
            axiomsToKeep.addAll(inputOntology.getAxioms(owlNamedIndividual));

            // find the types of this owlNamedIndividual
            Collection<OWLClassExpression> owlClasses = new HashSet<>();
            owlClasses = EntitySearcher.getTypes(owlNamedIndividual, inputOntology);
            logger.info("Entity " + owlNamedIndividual + " has initial types total: " + owlClasses.size());

            // call recursive function to find all class hierarchy
            owlClasses.stream().filter(OWLClass.class::isInstance).forEach(owlClassExpression -> {
                OWLClass owlClass = (OWLClass) owlClassExpression;
                axiomsToKeep.addAll(inputOntology.getAxioms(owlClass));
                logger.info("Entity " + owlNamedIndividual + " has initial types: " + owlClass);
                findSuperTypesRecursive(owlClass, axiomsToKeep);
            });
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * keep all superclasses of owlClass by using the recursive call
     * @param owlClasses
     * @return
     */
    public HashSet<OWLAxiom> extractAxiomsRelatedToOWLClasses(HashSet<OWLClass> owlClasses) {
        HashSet<OWLAxiom> owlAxiomsRelatedToClasses = new HashSet<>();

        logger.info("Processing extractAxiomsRelatedToOWLClasses started........... ");

        for (OWLClass owlClass : owlClasses) {
            logger.info("Processing owlclass: " + owlClass);
            Set<OWLClassAxiom> axiomSet = inputOntology.getAxioms(owlClass);
            logger.info("\tRelated axioms size using inputOntology.getAxioms(owlClass).size: " + axiomSet.size());
            owlAxiomsRelatedToClasses.addAll(axiomSet);
            findSuperTypesRecursive(owlClass, owlAxiomsRelatedToClasses);
        }

        logger.info("Processing extractAxiomsRelatedToOWLClasses finished. owlAxiomsRelatedToClasses size: " + owlAxiomsRelatedToClasses.size());
        return owlAxiomsRelatedToClasses;
    }

    /**
     * @param referredDirectEntities
     * @return true if (axiomsToKeep.size() > 0) else false. or return false for any exception
     */
    public HashSet<OWLAxiom> extractAxiomsRelatedToIndivs(HashSet<OWLNamedIndividual> referredDirectEntities, HashSet<OWLNamedIndividual> referredIndirectEntities) {
        try {
            if (null != referredDirectEntities) {
                logger.info("extractAxiomsRelatedToIndivs started with referred Direct entities size: "
                        + referredDirectEntities.size() + " .............");
                referredDirectEntities.forEach(owlNamedIndividual -> {
                    extractAxiomsRelatedToIndiv(owlNamedIndividual);
                });
                logger.info("extractAxiomsRelatedToIndivs successfull. total axioms (to keep) size : " + axiomsToKeep.size());
            } else {
                logger.info("extractAxiomsRelatedToIndivs couldn't start as referredDirectEntities is null");
            }

            if (null != referredIndirectEntities) {
                logger.info("extractAxiomsRelatedToIndivs started with referred Indirect entities size: "
                        + referredIndirectEntities.size() + " .............");
                referredIndirectEntities.forEach(owlNamedIndividual -> {
                    extractAxiomsRelatedToIndiv(owlNamedIndividual);
                    indirectIndiCounter++;
                });
                logger.info("extractAxiomsRelatedToIndivs successfull. total axioms (to keep) size : " + axiomsToKeep.size());
            } else {
                logger.info("extractAxiomsRelatedToIndivs couldn't start as referredIndirectEntities is null");
            }

            if (axiomsToKeep.size() > 0) {
                return axiomsToKeep;
            } else return axiomsToKeep;
        } catch (Exception ex) {
            ex.printStackTrace();
            return axiomsToKeep;
        }
    }

    /**
     * inputs
     * 1. Input Ontology path
     * 2. output onto path
     * 3. Entities are kept in a text file seperated by new line
     * 4. object properties list
     */
    private static String inputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode.rdf";
    private static String outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode_stripped_for_7_ifps.rdf";
    private static String entityTxtFilePath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/exprs/river_vs_other_concepts_from_r/river_45_vs_29_from_r_training_entities.txt";

    public static void main(String[] args) {

        try {

            PropertyConfigurator.configure("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/main/resources/log4j.properties");

//            initiateOntoFactory(inputOntoPath);

//            readTextFileAndConvertToOntologyEntity(entityTxtFilePath);

//            OWLObjectProperty owlObjectProperty = ontoDataFacotry.
//                    getOWLObjectProperty(IRI.create("http://www.daselab.org/ontologies/ADE20K/hcbdwsu#imageContains"));
//            HashSet<OWLObjectProperty> owlObjectProperties = new HashSet<>();
//            owlObjectProperties.add(owlObjectProperty);
//            processObjectProperties(owlObjectProperties);

//            extractAxiomsRelatedToIndivs(referredDirectEntities, referredIndirectEntities);

//            outputOntology = outputOntoManager.createOntology(IRI.create("http://www.daselab.com/residue/analysis"));
//            outputOntoManager.addAxioms(outputOntology, axiomsToKeep);

//            Utility.saveOntology(outputOntology, outputOntoPath);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
