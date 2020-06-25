/*
Written by sarker.
Written at 4/21/20.
*/

import org.dase.ecii.ontofactory.StripDownOntology;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndiv;
import org.dase.ecii.ontofactory.strip.ListofObjPropAndIndivTextualName;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestStripDownOntology {


    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    //constructor
    public TestStripDownOntology() {

    }

    /**
     *
     */
    public void processIndivsWithObjProps(String inputOntoPath, String entityCsvFilePath, String objPropColumnName, String indivColumnName,  String outputOntoIRI, String outputOntoPath) {
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
            Utility.saveOntology(outputOntology, outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    public void processIndivsWithTypes(String inputOntoPath, String entityCsvFilePath, String indivColumnName, String typeColumnName, String outputOntoIRI, String outputOntoPath) {

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

        logger.info("saving pruned onto in: "+ outputOntoPath);
        try {
            Utility.saveOntology(outputOntology, outputOntoPath);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }


    private static String inputOntoPath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/strip_down_test/prune_test.owl";
    private static String outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/strip_down_test/prune_test_pruned1.owl";
    private static String entityCsvFilePath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/strip_down_test/prune_test_entities1.csv";

    public static void main(String[] args) {

        logger.info("Stripping started...............");
        long startTime = System.currentTimeMillis();

        TestStripDownOntology testStripDownOntology = new TestStripDownOntology();
        testStripDownOntology.processIndivsWithObjProps(inputOntoPath, entityCsvFilePath, "objprops", "indivs", "http://www.daselab.com/sarker/prune", outputOntoPath);

        long endTime = System.currentTimeMillis();
        logger.info("stripping finished.");
        logger.info("Total time to strip: " + (endTime - startTime) / 1000 + " seconds");
    }
}
