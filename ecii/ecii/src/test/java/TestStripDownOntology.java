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

    private static String inputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_cats_v1_non_cyclic.owl";
    private static String outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_cats_v1_non_cyclic_stripped_for_7_IFP.owl";
    private static String entityCsvFilePath = "/Users/sarker/Workspaces/Jetbrains/residue/experiments/7_IFP/Entities_With_Ontology/Entities_types_to_strip_down_onto.csv";


    //constructor
    public TestStripDownOntology() {

    }

    /**
     *
     */
    public void processIndivsWithObjProps() {
        StripDownOntology stripDownOntology = new StripDownOntology(inputOntoPath);

        ListofObjPropAndIndivTextualName listofObjPropAndIndivTextualName = stripDownOntology.
                readEntityFromCSVFile(entityCsvFilePath, "objprops", "indivs");

        ListofObjPropAndIndiv listofObjPropAndIndiv = stripDownOntology.
                convertToOntologyEntity(listofObjPropAndIndivTextualName);

        listofObjPropAndIndiv = stripDownOntology.processIndirectIndivsUsingObjProps(listofObjPropAndIndiv);

        HashSet<OWLAxiom> axiomsToKeep = stripDownOntology.extractAxiomsRelatedToIndivs(listofObjPropAndIndiv.directIndivs, listofObjPropAndIndiv.inDirectIndivs);

        OWLOntologyManager outputOntoManager = OWLManager.createOWLOntologyManager();

        OWLOntology outputOntology = null;
        try {
            outputOntology = outputOntoManager.createOntology(IRI.create("http://www.daselab.com/residue/analysis"));
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

        HashMap<String, HashSet<String>> namesHashMap = stripDownOntology.readIndivTypesFromCSVFile(entityCsvFilePath, "indivs", "indivtypes");

        HashMap<OWLNamedIndividual, HashSet<OWLClass>> entityHashMap = stripDownOntology.convertToOntologyEntity(namesHashMap);

        HashSet<OWLAxiom> axiomsToKeep = new HashSet<>();

        for (Map.Entry<OWLNamedIndividual, HashSet<OWLClass>> indivTypesHashMap : entityHashMap.entrySet()) {
            axiomsToKeep.addAll(stripDownOntology.extractAxiomsRelatedToOWLClasses(indivTypesHashMap.getValue()));
        }

        OWLOntologyManager outputOntoManager = OWLManager.createOWLOntologyManager();

        OWLOntology outputOntology = null;
        try {
            outputOntology = outputOntoManager.createOntology(IRI.create("http://www.daselab.com/residue/analysis"));
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


    public static void main(String[] args) {

        logger.info("Stripping started...............");
        long startTime = System.currentTimeMillis();

        TestStripDownOntology testStripDownOntology = new TestStripDownOntology();
//        testStripDownOntology.processIndivsWithTypes();

        long endTime = System.currentTimeMillis();
        logger.info("stripping finished.");
        logger.info("Total time to strip: " + (endTime - startTime) / 1000 + " seconds");
    }
}
