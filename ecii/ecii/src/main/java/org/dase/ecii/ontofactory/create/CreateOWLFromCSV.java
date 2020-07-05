package org.dase.ecii.ontofactory.create;
/*
Written by sarker.
Written at 4/22/20.
*/

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Create ontology by taking the data from csv file.
 */
public class CreateOWLFromCSV {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String csvPath;
    private OWLOntology outputOntology;
    private OWLOntologyManager owlOntologyManager;
    private OWLDataFactory owlDataFactory;
    private String ontoIRI;
    /**
     * Example: # or / or :
     * Default is #
     */
    private String delimeter = "#";
    /**
     * Whether the input entity has full name or short name
     * For example it may have name
     * GO_0006511 or
     * http://purl.obolibrary.org/obo/GO_0006511
     * <p>
     * Default false
     */
    private boolean providingEntityFullName = false;

    private OWLNamedIndividual baseIndividual;
    private String outputOntoPath;
    private OWLObjectProperty baseObjProp;

    /**
     * Constructor
     *
     * @param csvPath
     * @param objPropName
     * @param ontoIRI
     * @throws OWLOntologyCreationException
     */
    public CreateOWLFromCSV(String csvPath, String objPropName, String ontoIRI, boolean providingEntityFullName, String delimeter) throws OWLOntologyCreationException {

        this(csvPath, ontoIRI, providingEntityFullName, delimeter);

        if (null != objPropName) {
            this.baseObjProp = createObjProp(objPropName);
        }
        this.baseIndividual = owlDataFactory.getOWLNamedIndividual(
                IRI.create(ontoIRI + delimeter + new File(csvPath).getName()
                        .replace(".csv", "")));
    }

    /**
     * Constructor
     *
     * @param csvPath
     * @param ontoIRI
     * @throws OWLOntologyCreationException
     */
    public CreateOWLFromCSV(String csvPath, String ontoIRI, boolean providingEntityFullName, String delimeter) throws OWLOntologyCreationException {

        this.csvPath = csvPath;
        this.providingEntityFullName = providingEntityFullName;
        // remove the delimeter from the ontoIRI
        if (ontoIRI.endsWith(delimeter)) {
            this.ontoIRI = ontoIRI.replaceAll(delimeter + "$", "");
        } else {
            this.ontoIRI = ontoIRI;
        }
        this.delimeter = delimeter;

        this.owlOntologyManager = OWLManager.createOWLOntologyManager();
        this.outputOntology = this.owlOntologyManager.createOntology(IRI.create(this.ontoIRI));
        this.owlDataFactory = this.outputOntology.getOWLOntologyManager().getOWLDataFactory();

        this.outputOntoPath = csvPath.replace(".csv", ".owl");

        logger.info("this.ontoIRI: " + this.ontoIRI);
        logger.debug("delimeter: " + delimeter);

    }

    /**
     * Create a proper IRI from the given string.
     * Entity name can be full name or short name
     * Full name example:
     * www.http://hcbd.org#indi or
     * www.http://hcbd.org/indi or
     * www.http://hcbd.org:indi
     * short name example:
     * indi
     * <p>
     * This method uses the instance variable
     * providingEntityFullName to check either full name is given or not
     * <p>
     * if providingEntityFullName is true it just create the iri.
     * if providingEntityFullName is false it create iri by combing other information
     * delimeter and ontoIRI to create full iri. ontoIRI + delimeter + entityName
     *
     * @param entityName
     * @return
     */
    private IRI getProperIRI(String entityName) {
        String new_name = "";

        if (null != entityName) {
            if (providingEntityFullName) {
                new_name = entityName;
            } else {
                if (null != ontoIRI && null != delimeter) {
                    if (entityName.startsWith(delimeter)) {
                        new_name = ontoIRI + entityName;
                    } else {
                        new_name = ontoIRI + delimeter + entityName;
                    }
                } else {
                    logger.error("ERROR!!!!!!!!entityName is null");
                    return null;
                }
            }
        } else {
            logger.error("ERROR!!!!!!!!entityName is null");
            return null;
        }
        IRI iri = IRI.create(new_name);
        return iri;
    }

    /**
     * Create object property using the name
     *
     * @param objPropName
     * @return OWLObjectProperty
     */
    private OWLObjectProperty createObjProp(String objPropName) {
        logger.debug("objPropName-Raw: " + objPropName);
        IRI iri = getProperIRI(objPropName);
        logger.debug("objPropName-Proper: " + objPropName);

        if (null != iri) {
            OWLObjectProperty owlObjectProperty = owlDataFactory.getOWLObjectProperty(iri);
            return owlObjectProperty;
        } else {
            logger.error("ERROR!!!!!!!!!!!iri is null");
            return null;
        }
    }

    /**
     * Create owlClass using the name
     *
     * @param className
     * @return OWLClass
     */
    private OWLClass createOWLClass(String className) {
        logger.debug("className-Raw: " + className);
        IRI iri = getProperIRI(className);
        logger.debug("className-Proper: " + className);

        if (null != iri) {
            OWLClass owlClass = owlDataFactory.getOWLClass(iri);
            return owlClass;
        } else {
            logger.error("ERROR!!!!!!!!!!!iri is null");
            return null;
        }
    }

    /**
     * Create OWLNamedIndividual using the name
     * When creating individual we may have extra prefix like indiv_
     *
     * @param indivName
     * @return OWLNamedIndividual
     */
    private OWLNamedIndividual createOWLNamedIndividual(String indivName) {
        logger.debug("indivName-Raw: " + indivName);
        IRI iri = getProperIRI(indivName);
        logger.debug("indivName-Proper: " + indivName);

        if (null != iri) {
            OWLNamedIndividual owlNamedIndividual = owlDataFactory.getOWLNamedIndividual(iri);
            return owlNamedIndividual;
        } else {
            logger.error("ERROR!!!!!!!!!!!iri is null");
            return null;
        }

//        if (indivName.startsWith(delimeter)) {
//            OWLNamedIndividual owlNamedIndividual = owlDataFactory.getOWLNamedIndividual(IRI.create(indivName));
//            return owlNamedIndividual;
//        } else {
//            OWLNamedIndividual owlNamedIndividual = owlDataFactory.getOWLNamedIndividual(IRI.create(ontoIRI + delimeter + indivName));
//            logger.info("ontoIRI: " + ontoIRI);
//            logger.info("owlNamedIndividual else: " + owlNamedIndividual);
//            return owlNamedIndividual;
//        }
    }

    /**
     * Create axiom: owlIndiv rdf:Type owlClass
     *
     * @param owlNamedIndividual
     * @param owlClass
     * @return OWLAxiom
     */
    private OWLAxiom createTypeRelation(OWLNamedIndividual owlNamedIndividual, OWLClass owlClass) {
        return owlDataFactory.getOWLClassAssertionAxiom(owlClass, owlNamedIndividual);
    }

    /**
     * Create axioms and save the ontology
     * <p>
     * Axioms created:
     * 1. indivEntity-------owlNamedIndividual
     * here, indivEntity are the entities of column indivColumnName
     * <p>
     * 2. this.baseObjProp-------this.baseIndividual------owlNamedIndividual
     * here, if baseObjProp or baseIndividual is null then this axiom is not created.
     * <p>
     * 3. typeEntity--------owlClass
     * here, typeEntity are the entities of column typesColumnName
     * <p>
     * 4. owlNamedIndividual-----rdf:Type-----------------owlClass
     * <p>
     * owlNamedIndividual-------
     *
     * @param indivColumnName
     * @param typesColumnName
     */
    public void parseCSVToCreateIndivAndTheirTypes(String indivColumnName, String typesColumnName) {

        logger.info("Parsing csv and creating ontology from the data starting..........");
        if (null == indivColumnName || null == this.csvPath) {
            logger.error("indivColumnName or csvPath is null!!!!!\n Parsing csv and creating ontology from the data can't continue. \n\t returning.");
            return;
        }

        CSVParser csvRecords = Utility.parseCSV(this.csvPath, true);
        logger.info("Parsing CSV finished");

        logger.info("Iterating over csv data and creating onto entity started............");
        int counter = 0;
        for (CSVRecord csvRecord : csvRecords) {
            String indivName = csvRecord.get(indivColumnName);

            OWLNamedIndividual owlNamedIndividual = createOWLNamedIndividual(indivName);
            owlOntologyManager.addAxiom(outputOntology, createTypeRelation(owlNamedIndividual, owlDataFactory.getOWLThing()));
            counter++;

            // property assertion
            if (null != this.baseObjProp && null != this.baseIndividual) {
                OWLAxiom owlAxiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(this.baseObjProp, this.baseIndividual, owlNamedIndividual);
                owlOntologyManager.addAxiom(outputOntology, owlAxiom);
            }

            // type assertion
            if (null != typesColumnName) {
                String typeNames = csvRecord.get(typesColumnName);
                String[] typeNamesArray = typeNames.split(";");
                for (String eachTypeName : typeNamesArray) {
                    OWLClass owlClass = createOWLClass(eachTypeName);
                    owlOntologyManager.addAxiom(outputOntology, createTypeRelation(owlNamedIndividual, owlClass));
                }
            }
        }
        logger.info("Iterating over csv data and creating onto entity finished.");
        logger.info("Total row found in CSV: " + counter);

        // save the ontology
        try {
            logger.info("Saving ontology..........");
            Utility.saveOntology(outputOntology, outputOntoPath);
            logger.info("Saving ontology finished.");
            logger.info("Ontology saved at: " + outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        logger.info("Parsing csv and creating ontology from the data finished.");
    }


    /**
     * Create axioms and save the ontology
     * <p>
     * Axioms created:
     * 1. indivEntity-------owlNamedIndividual
     * here, indivEntity are the entities of column entityColumnName
     * <p>
     * 2. this.baseObjProp-------this.baseIndividual------owlNamedIndividual
     * here, if baseObjProp or baseIndividual is null then this axiom is not created.
     * <p>
     * 3. typeEntity--------owlClass
     * here, typeEntity are the entities of column typesColumnName
     * <p>
     * 4. owlNamedIndividual-----rdf:Type-----------------owlClass
     * <p>
     * owlNamedIndividual-------
     *
     * @param entityColumnName
     * @param typesColumnName
     */

    /**
     * Create axioms and save the ontology
     * <p>
     * Axioms created:
     * 1. indivEntity-------owlNamedIndividual
     * here, indivEntity are the entities of column entityColumnName
     * <p>
     * if assignTypeUsingSameEntity== True
     * 2. typeEntity--------owlClass
     * here, typeEntity are the same entities as like the individual
     * 3. owlNamedIndividual-----rdf:Type-----------------owlClass
     * <p>
     *
     * @param entityColumnName
     * @param usePrefixForIndivCreation
     * @param indivPrefix
     * @param assignTypeUsingSameEntity
     */
    public void parseCSVToCreateIndivAndTheirTypes(String entityColumnName, boolean usePrefixForIndivCreation, String indivPrefix, boolean assignTypeUsingSameEntity) {

        logger.info("Parsing csv and creating ontology from the data starting..........");
        if (null == entityColumnName || null == this.csvPath) {
            logger.error("entityColumnName or csvPath is null!!!!!\n Parsing csv and creating ontology from the data can't continue. \n\t returning.");
            return;
        }

        CSVParser csvRecords = Utility.parseCSV(this.csvPath, true);
        logger.info("Parsing CSV finished");

        logger.info("Iterating over csv data and creating onto entity started............");
        int counter = 0;
        for (CSVRecord csvRecord : csvRecords) {
            String indivName = csvRecord.get(entityColumnName);
            if (indivName.length() > 0) {
                String indivNameWithPrefix = indivName;

                if (usePrefixForIndivCreation) {
                    indivNameWithPrefix = indivPrefix + indivName;
                }
                OWLNamedIndividual owlNamedIndividual = createOWLNamedIndividual(indivNameWithPrefix);
                if (counter == 2) {
                    logger.info("owlNamedIndividual: " + owlNamedIndividual);
                }
                owlOntologyManager.addAxiom(outputOntology, createTypeRelation(owlNamedIndividual, owlDataFactory.getOWLThing()));
                counter++;

                // type assertion
                if (assignTypeUsingSameEntity) {
                    OWLClass owlClass = createOWLClass(indivName);
                    owlOntologyManager.addAxiom(outputOntology, createTypeRelation(owlNamedIndividual, owlClass));
                }
            }
        }

        // save the ontology
        try {
            logger.info("Saving ontology..........");
            Utility.saveOntology(outputOntology, outputOntoPath);
            logger.info("Saving ontology finished.");
            logger.info("Ontology saved at: " + outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        logger.info("Parsing csv and creating ontology from the data finished.");
    }


}
