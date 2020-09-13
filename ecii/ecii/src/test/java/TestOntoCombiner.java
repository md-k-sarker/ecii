/*
Written by sarker.
Written at 6/25/20.
*/

import org.dase.ecii.ontofactory.OntoCombiner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;

public class TestOntoCombiner {

    /**
     *
     */
    public void testEqualityOf2Entitys() {
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory owlDataFactory = owlManager.getOWLDataFactory();

        OWLNamedIndividual owlNamedIndividual1 = owlDataFactory.getOWLNamedIndividual(IRI.create("indiv1"));

        OWLNamedIndividual owlNamedIndividual2 = owlDataFactory.getOWLNamedIndividual(IRI.create("indiv1"));
    }

    /**
     * test public void combineOntologies(String outputPath, HashSet<String> inputOntoPaths)
     */
    public void testCombineOntologies() {
        // test1
        String outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/combined_1_2.owl";

        HashSet<String> inputOntos = new HashSet<>();
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input1.owl");
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input2.owl");

        OntoCombiner ontoCombiner = new OntoCombiner(null);
        // this method worked for class combiner, when the input was
        // onto1:
        //      classF subClassOf owl:Thing and
        //      obj1 type classF
        // onto2:
        //      classF subClassOf classE,
        //      obj1 type classF
        // it combined them into single class, output
        //      classF subClass of classE
        ontoCombiner.combineOntologies(outputOntoPath, inputOntos);

        // test2
        outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/combined_3_4.owl";

        inputOntos = new HashSet<>();
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input3.owl");
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input4.owl");

        ontoCombiner = new OntoCombiner(null);
        // this method worked for class combiner, when the input was
        // onto3:
        //      classF subClassOf owl:Thing and
        //      obj1 type classF
        // onto4:
        //      classF subClassOf classE,
        //      //obj1 type classF
        // it combined them into single class, output
        //      classF subClass of classE
        ontoCombiner.combineOntologies(outputOntoPath, inputOntos);

        // test3
        outputOntoPath = "/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/combined_5_6.owl";

        inputOntos = new HashSet<>();
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input5.owl");
        inputOntos.add("/Users/sarker/Workspaces/Jetbrains/ecii/ecii/ecii/src/test/resources/expr_types/onto_combiner_test/onto_combiner_input6.owl");

        ontoCombiner = new OntoCombiner(null);
        // this method worked for class combiner, when the input was
        // onto5:
        //      classF subClassOf owl:Thing and
        //      obj1 type classF
        // onto6:
        //      classF subClassOf classE,
        //      obj1 type classC
        // it combined them into single class, output
        //      classF subClass of classE
        ontoCombiner.combineOntologies(outputOntoPath, inputOntos);


    }

    public static void main(String[] args) {

        TestOntoCombiner testOntoCombiner = new TestOntoCombiner();
        testOntoCombiner.testCombineOntologies();
    }
}
