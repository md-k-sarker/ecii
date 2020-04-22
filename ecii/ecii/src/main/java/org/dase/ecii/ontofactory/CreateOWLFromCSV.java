package org.dase.ecii.ontofactory;
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

public class CreateOWLFromCSV {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String csvPath;
    private OWLOntology outputOntology;
    private OWLOntologyManager owlOntologyManager;
    private OWLDataFactory owlDataFactory;
    private String ontoIRI;

    private OWLNamedIndividual baseIndividual;
    private String outputOntoPath;
    private OWLObjectProperty baseObjProp;

    public CreateOWLFromCSV(String csvPath, String objPropName, String ontoIRI) throws OWLOntologyCreationException {
        this.csvPath = csvPath;
        this.ontoIRI = ontoIRI;
        this.owlOntologyManager = OWLManager.createOWLOntologyManager();
        this.outputOntology = owlOntologyManager.createOntology(IRI.create(ontoIRI));
        this.owlDataFactory = this.outputOntology.getOWLOntologyManager().getOWLDataFactory();

        this.baseIndividual = owlDataFactory.getOWLNamedIndividual(IRI.create(ontoIRI + "#" + new File(csvPath).getName().replace(".csv", "")));

        this.outputOntoPath = csvPath.replace(".csv", ".owl");

        this.baseObjProp = createObjProp(objPropName);
    }

    private OWLObjectProperty createObjProp(String objPropName) {
        if (objPropName.contains("#"))
            return owlDataFactory.getOWLObjectProperty(IRI.create(objPropName));
        else
            return owlDataFactory.getOWLObjectProperty(IRI.create(ontoIRI + "#" + objPropName));
    }

    private OWLClass createOWLClass(String className) {
        if (className.contains("#"))
            return owlDataFactory.getOWLClass(IRI.create(className));
        else
            return owlDataFactory.getOWLClass(IRI.create(ontoIRI + "#" + className));
    }

    private OWLNamedIndividual createOWLNamedIndividual(String indivName) {
        if (indivName.contains("#"))
            return owlDataFactory.getOWLNamedIndividual(IRI.create(indivName));
        else
            return owlDataFactory.getOWLNamedIndividual(IRI.create(ontoIRI + "#" + indivName));
    }

    private OWLAxiom createTypeRelation(OWLNamedIndividual owlNamedIndividual, OWLClass owlClass) {
        return owlDataFactory.getOWLClassAssertionAxiom(owlClass, owlNamedIndividual);
    }

    public void parseCSVToCreateIndivAndTheirTypes(String indivColumnName, String typesColumnName) {
        CSVParser csvRecords = Utility.parseCSV(this.csvPath, true);

        for (CSVRecord csvRecord : csvRecords) {
            String indivName = csvRecord.get(indivColumnName);

            OWLNamedIndividual owlNamedIndividual = createOWLNamedIndividual(indivName);

            OWLAxiom owlAxiom = owlDataFactory.getOWLObjectPropertyAssertionAxiom(baseObjProp, this.baseIndividual, owlNamedIndividual);

            owlOntologyManager.addAxiom(outputOntology, owlAxiom);

            String typeNames = csvRecord.get(typesColumnName);
            String[] typeNamesArray = typeNames.split(";");
            for (String eachTypeName : typeNamesArray) {
                OWLClass owlClass = createOWLClass(eachTypeName);

                owlOntologyManager.addAxiom(outputOntology, createTypeRelation(owlNamedIndividual, owlClass));
            }
        }

        try {
            Utility.saveOntology(outputOntology, outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }


}
