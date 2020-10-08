package org.dase.ecii.ontofactory.strip;
/*
Written by sarker.
Written at 3/7/20.
*/

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.PropertyConfigurator;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * Prune the ontology.
 * Input:
 * 1. Ontology, (possibly a large ontology, in our case the wikipedia hierarchy)
 * 2. Entities (providing by csv files)
 * 3. Object properties (in most case we have indiv1 imgContains obj1 and then we look for the type of obj1)
 * <p>
 * Output:
 * 1. Pruned ontology (will be saved on the given file location)
 * Invariant:
 * Input entities and their corresponding axioms (entity types and super types and associated obj properties) we need to keep
 * <p>
 * General information:
 * We should use extractAxiomsRelatedToIndivs(...) this function instead of extractAxiomsRelatedToOWLClasses(...) whenever possible.
 * See documentation of function extractAxiomsRelatedToIndivs(...) for details.
 * <p>
 * ----
 * Date: 08/12/2020
 * Minor bug found, when stripping Dbpedia (combined with ade20k mountains, workroom ontos) ontology!!. Although concept island was referred by
 * some object in mountain image, those were completely being stripped from the ontology.
 * ---
 */
public class StripDownOntology {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * axioms to keep
     */
    private HashSet<OWLAxiom> axiomsToKeep = new HashSet<>();

    private String inputOntoPath;
    private String entityCSVFilePath;

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

    /**
     * public Constructor
     *
     * @param inputOntoPath
     */
    public StripDownOntology(String inputOntoPath) {
        // ontology loading is deferred until the csv is read
        // need to call function loadOntologyRelatedResources()
        if (null != inputOntoPath) {
            this.inputOntoPath = inputOntoPath;
        }else {
            logger.error("Input ontology path is null!!!, program exiting.");
            System.exit(-1);
        }
    }

    /**
     * Public constructor
     *
     * @param owlOntology
     */
    public StripDownOntology(OWLOntology owlOntology) {
        try {
            if (null != owlOntology) {
                inputOntology = owlOntology;
                inputOntoManager = inputOntology.getOWLOntologyManager();
                ontoDataFacotry = inputOntoManager.getOWLDataFactory();
            }
        } catch (Exception ex) {
            logger.error("Initiating ontology related resources failed!!!!!!!!!!!!!");
            ex.printStackTrace();
        }
    }

    /**
     * ontology loader and intiator. This function should be called after reading csv
     *
     * @return
     */
    public boolean loadOntologyRelatedResources() {
        try {
            if (null != inputOntoPath) {
                logger.info("Initiating ontology related resources...............");
                long ontoLoadStartTime = System.currentTimeMillis();
                inputOntology = Utility.loadOntology(inputOntoPath);
                inputOntoManager = inputOntology.getOWLOntologyManager();
                ontoDataFacotry = inputOntoManager.getOWLDataFactory();

                // outputOntoManager = OWLManager.createOWLOntologyManager();
                logger.info("Initiating ontology related resources successfull");
                long ontoLoadEndTime = System.currentTimeMillis();
                logger.info("Onto load time: " + (ontoLoadEndTime - ontoLoadStartTime) / 1000 + " seconds");
                return true;
            } else {
                logger.error("Can't construct the StripDownOntology constructor");
                logger.error("Initiating ontology related resources failed!!!!!!!!!!!!! because inputOntoPath is null");
                return false;
            }
        } catch (Exception ex) {
            logger.error("Initiating ontology related resources failed!!!!!!!!!!!!!");
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Read entity from csv file,
     * <p>
     * objectProperty name and Individual names must be on different column of the csv.
     *
     * @param csvFilePath
     * @param objPropColumnName: must remain on csv file
     * @param indivColumnName:   must remain on csv file
     * @return ListofObjPropAndIndivTextualName
     * @ exception: java.lang.IllegalArgumentException: Mapping for not found for any header name not found on csv file
     */
    public ListofObjPropAndIndivTextualName readEntityFromCSVFile(String csvFilePath, String objPropColumnName, String indivColumnName) {
        this.entityCSVFilePath = csvFilePath;
        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = null;
        try {
            if (null != csvFilePath) {
                if (null != objPropColumnName) {
                    if (null != indivColumnName) {
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
                        logger.info("Total namedIndividuals row found in csv: " + listofObjPropAndIndivTextualName.indivNames.size());
                        logger.info("Total objProps row found in csv: " + listofObjPropAndIndivTextualName.objPropNames.size());
                        return listofObjPropAndIndivTextualName;
                    } else {
                        logger.error("readEntityFromCSVFile starting failed!!!!!!!!!!!!! indivColumnName is null!!!!!");
                        return listofObjPropAndIndivTextualName;
                    }
                } else {
                    logger.error("readEntityFromCSVFile starting failed!!!!!!!!!!!!! objPropColumnName is null!!!!!");
                    return listofObjPropAndIndivTextualName;
                }
            } else {
                logger.error("readEntityFromCSVFile starting failed!!!!!!!!!!!!! input entity file name is null!!!!!");
                return listofObjPropAndIndivTextualName;
            }
        } catch (Exception ex) {
            logger.error("readEntityFromCSVFile failed!!!! Reading entity file error, entity file name: " + entityCSVFilePath);
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Read individual names and their types from csv file.
     *
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
     * @return ListofObjPropAndIndiv
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
     * Process/search indirect indivs from the ontology.
     * <p>
     * Essentially this function takes:
     * ListofObjPropAndIndiv.objPropsofInterest
     * ListofObjPropAndIndiv.directIndivs
     * and search for indirectIndiv and save it inside
     * ListofObjPropAndIndiv.inDirectIndivs
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

    static int reccounter = 0;
    static int indirectIndiCounter = 0;

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
        logger.debug("indirectIndiCounter: " + indirectIndiCounter + " \t reccounter: " + reccounter);
        logger.debug("Counter " + reccounter + " ### findSuperTypesRecursive started with: " + owlClass);

        if (ontoDataFacotry.getOWLThing().equals(owlClass))
            return;

        // initial call
        Collection<OWLClassExpression> superClasses = EntitySearcher.
                getSuperClasses(owlClass, inputOntology);

        logger.debug("### findSuperTypesRecursive running with superClasses.size: " + superClasses.size());

        if (superClasses.size() < 1)
            return;

        for (OWLClassExpression owlClassExpression : superClasses) {

            logger.debug("### findSuperTypesRecursive running with super-owlClassExpression: " + owlClassExpression);

            if (owlClassExpression instanceof OWLClass) {
                OWLClass owlClassNew = (OWLClass) owlClassExpression;
                if (!owlClassNew.equals(owlClass)) {
                    owlAxiomsToKeep.addAll(inputOntology.getAxioms(owlClassNew));
                    findSuperTypesRecursive(owlClassNew, owlAxiomsToKeep);
                }
            }
        }

        logger.debug("### findSuperTypesRecursive finished with: " + owlClass);
    }

    /**
     * extract the axioms related to a single owl class.
     * This keeps all superclass of the owl class.
     * but if a class is just subclass of owlThing but not subclass of other class then that class is discarded!!!
     * <p>
     * constraint: this function do the inplace modification of owlAxiomsRelatedToClass so not returning any value.
     *
     * @param owlClass
     * @param owlAxiomsRelatedToClass
     */
    private void extractAxiomsRelatedToOWLClass(OWLClass owlClass, HashSet<OWLAxiom> owlAxiomsRelatedToClass) {
        logger.debug("\towlAxiomsRelatedToClass size before processing this class: " + owlAxiomsRelatedToClass.size());

        if (null != owlClass && null != owlAxiomsRelatedToClass) {
            Set<OWLClassAxiom> axiomSet = inputOntology.getAxioms(owlClass);
            logger.debug("\t\tRelated axioms size using inputOntology.getAxioms(owlClass).size: " + axiomSet.size());
            owlAxiomsRelatedToClass.addAll(axiomSet);
            findSuperTypesRecursive(owlClass, owlAxiomsRelatedToClass);
        }

        logger.debug("\t\towlAxiomsRelatedToClass size after processing this class: " + owlAxiomsRelatedToClass.size());
    }

    /**
     * keep all superclasses of owlClass by using the recursive call,
     * constraint: if an owlclass is only subclass of owl:Thing then and has some individual as instance, then
     * inputOntology.getAxioms(owlClass) doesn't return any axioms!!!!
     *
     * @param owlClasses
     * @return
     * @deprecated
     */
    public HashSet<OWLAxiom> extractAxiomsRelatedToOWLClasses(HashSet<OWLClass> owlClasses) {
        HashSet<OWLAxiom> owlAxiomsRelatedToClasses = new HashSet<>();

        logger.info("Processing extractAxiomsRelatedToOWLClasses started........... ");

        for (OWLClass owlClass : owlClasses) {
            logger.debug("Processing owlclass: " + owlClass);
            extractAxiomsRelatedToOWLClass(owlClass, owlAxiomsRelatedToClasses);
        }

        logger.info("Processing extractAxiomsRelatedToOWLClasses finished. owlAxiomsRelatedToClasses size: " + owlAxiomsRelatedToClasses.size());
        return owlAxiomsRelatedToClasses;
    }

    /**
     * Extract axioms related to an individual and stores those axioms to the
     * axiomsToKeep field of this class.
     * <p>
     * Steps to extract:
     * 1. Extract axioms related to this individual
     * 2. Find types of this individual
     * 3. Extract axioms related to that types
     * 4. Find super types of that type recursively and extract axioms related to them.
     *
     * @param owlNamedIndividual
     * @return
     */
    private boolean extractAxiomsRelatedToIndiv(OWLNamedIndividual owlNamedIndividual) {
        try {
            axiomsToKeep.addAll(inputOntology.getAxioms(owlNamedIndividual));

            // find the types of this owlNamedIndividual
            Collection<OWLClassExpression> owlClasses = new HashSet<>();
            owlClasses = EntitySearcher.getTypes(owlNamedIndividual, inputOntology);
            logger.debug("Entity " + owlNamedIndividual + " has initial types total: " + owlClasses.size());

            // call recursive function to find all class hierarchy
            owlClasses.stream().filter(OWLClass.class::isInstance).forEach(owlClassExpression -> {
                OWLClass owlClass = (OWLClass) owlClassExpression;
                logger.debug("Entity " + owlNamedIndividual + " has initial types: " + owlClass);
                logger.debug("\tProcessing owlclass: " + owlClass);
                extractAxiomsRelatedToOWLClass(owlClass, axiomsToKeep);
            });
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * constraint: if an owlclass is only subclass of owl:Thing then and has some individual as instance, then
     * inputOntology.getAxioms(owlClass) doesn't return any axioms!!!!
     * but inputOntology.getAxioms(owlNamedIndividual) returns axioms
     * <p>
     * so we should use this function instead of function extractAxiomsRelatedToOWLClasses() whenever possible.
     *
     * @param referredDirectEntities
     * @param referredIndirectEntities
     * @return
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
     * Removes concepts which are direct subclassof owl:Thing and don't have any subclass.
     * TODO:
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
    public static OWLOntology removeNonRelatedConceptsV1(OWLOntology combinedOntology, OWLReasoner owlReasoner) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getClassesInSignature().forEach(owlClass -> {
            Optional<OWLClass> supClass = owlReasoner.getSuperClasses(owlClass, true).getFlattened().stream().findFirst();
            if (supClass.isPresent()) {
                OWLClass superClass = supClass.get();
                if (superClass.isOWLThing()) {
                    if (owlReasoner.getSubClasses(owlClass, true).getFlattened().size() < 2) {
                        // logger.info("removing owlclass: "+ Utility.getShortName(owlClass));
                        entityRemover.visit(owlClass);
                    } else {
                        logger.info("can not remove owlclass: " + Utility.getShortName(owlClass));
                    }
                }
            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * keep those concepts which are type of positive or negative objects.
     * alternatively keep
     * those concepts which have at-least a single instance.
     * TODO: FIX
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
    public static OWLOntology removeNonRelatedConceptsV2(OWLOntology combinedOntology, OWLReasoner owlReasoner) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getClassesInSignature().forEach(owlClass -> {
            if (owlReasoner.getInstances(owlClass, false).getFlattened().size() < 1) {
                entityRemover.visit(owlClass);
            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * Removes concepts which are not
     * 1. class type of positive objects or negative objects or
     * 2. subClass type of positive objects or negative objects or
     * 3. superclass type of positive objects or negative objects .
     * <p>
     * TODO: FIX
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
//    public static OWLOntology removeNonRelatedConceptsV3(OWLOntology combinedOntology, OWLReasoner owlReasoner) {
//
//        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
//
//        combinedOntology.classesInSignature().forEach(owlClass -> {
//            if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlClass) || SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlClass) || (owlClass.isOWLThing())) {
//
//            } else {
//
//            }
//        });
//
//        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
//        if (logger != null)
//            logger.info("Removing " + ca.toString());
//
//        return combinedOntology;
//    }


    /**
     * Removes non related properties.
     *
     * @param combinedOntology
     * @return
     */
    public static OWLOntology removeNonRelatedProperties(OWLOntology combinedOntology) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getObjectPropertiesInSignature().forEach(owlObjectProperty -> {
            if (!SharedDataHolder.objProperties.containsKey(owlObjectProperty)) {
                entityRemover.visit(owlObjectProperty);
            }
//            if (!owlObjectProperty.equals(SharedDataHolder.objPropImageContains)) {
//
//            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * inputs
     * 1. Input Ontology path
     * 2. output onto path
     * 3. Entities are kept in a text file seperated by new line
     * 4. object properties list
     */
//    private static String inputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode.rdf";
//    private static String outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode_stripped_for_7_ifps.rdf";
//    private static String entityTxtFilePath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/river_vs_other_concepts_from_r/river_45_vs_29_from_r_training_entities.txt";

    /**
     * Being used to test different methods easily
     *
     * @param args
     */
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
