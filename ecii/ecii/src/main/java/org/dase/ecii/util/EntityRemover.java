package org.dase.ecii.util;
/*
Written by sarker.
Written at 2/8/21.
*/

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;

/**
 * Remove entity from the ontology, to make the ontology beautiful.
 * Beautify the ontology by removing certain non-readable entity.
 * <p>
 * Idea behind it: Wikipedia ontology has many non-usable concepts like: Hidden_**, Tracking_** etc.
 * To get good result for emerald experiment on ade20k data, these concepts was removed programmatically.
 * <p>
 * Remove entity by matching name
 */
public class EntityRemover {


    static public boolean removeConcepts(OWLOntology owlOntology, HashSet<String> conceptNames) {
        try {
            Set<OWLAxiom> axiomsToRemove = new HashSet<>();

            // look on each axiom
            for (OWLAxiom ax : owlOntology.getAxioms()) {

                // look for each entity of this axioms
                for (OWLEntity owlEntity : ax.getSignature()) {
                    String entity = owlEntity.toString();
                    System.out.println("entity string: " + entity);
                    for (String conceptName : conceptNames) {
                        if (entity.startsWith(conceptName)) {
                            System.out.println("found concept: " + conceptName);
                            axiomsToRemove.add(ax);
                            // break looking more on the hashset
                            break;
                        }
                    }
                }
            }

            System.out.println("Before: axioms size " + owlOntology.getAxiomCount());
            System.out.println("Total axioms to delete: " + axiomsToRemove.size());
            owlOntology.getOWLOntologyManager().removeAxioms(owlOntology, axiomsToRemove);
            System.out.println("After: axioms size " + owlOntology.getAxiomCount());

        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Remove top k concepts from the ontology, starting from the immediate child of OWL:Thing
     * <p>
     * Constraints: simple entitysearcher is not finding the subclass of owl:thing as those are not asserted.
     * Using owlreasoner to solve this problem, reasoner is hardcoded: pellet
     *
     * @param owlOntology
     * @param topK
     * @return
     */
    static public boolean removeConceptsTopK(OWLOntology owlOntology, int topK) {
        try {
            Set<OWLAxiom> axiomsToRemove = new HashSet<>();
            OWLReasoner owlReasoner = Utility.initReasoner("pellet", owlOntology, null);

            OWLClass rootClass = owlOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing();

            int depth = 0;
            HashSet<OWLClass> visited = new HashSet<>(); // ideally our wiki kg is non-cyclic, but in case it's not, then this visited hashset will be useful
            LinkedList<OWLClass> queue = new LinkedList<>();
            queue.add(rootClass);
            visited.add(rootClass);

            while (!queue.isEmpty() && depth < topK) {

                int level_size = queue.size();
                while (level_size-- != 0) {
                    // take and remove the top of the queue
                    OWLClass owlClass = queue.poll();
                    System.out.println("owlClass: " + owlClass);
                    Collection<OWLClass> subClasses = owlReasoner.getSubClasses(owlClass, true).getFlattened();
                    System.out.println("subclasses size: " + subClasses.size());
                    for (OWLClassExpression subClass : subClasses) {
                        System.out.println("subclass: " + subClass.toString());
                        if (!visited.contains(subClass)) {
                            // add the subclass to queue
                            queue.add((OWLClass) subClass);

                            // process the subclass, i.e. look for this subclass in all axioms
                            for (OWLAxiom ax : owlOntology.getAxioms()) {
                                // if this entity exists in this axiom remove this axiom
                                if (ax.containsEntityInSignature((OWLEntity) subClass)) {
                                    // we can remove those axioms
                                    axiomsToRemove.add(ax);
                                }
                            }
                            visited.add(owlClass);
                        }
                    }
                }
                // increase the depth
                depth++;
            }

            System.out.println("Before: axioms size " + owlOntology.getAxiomCount());
            System.out.println("Total axioms to delete: " + axiomsToRemove.size());
            owlOntology.getOWLOntologyManager().removeAxioms(owlOntology, axiomsToRemove);
            System.out.println("After: axioms size " + owlOntology.getAxiomCount());

        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    static String inputOntoPath = "src/test/resources/expr_types/entity_remover_test/test_remove_topK.owl";
    static String outputOntoPath = "src/test/resources/expr_types/entity_remover_test/test_remove_topK_cleaned_v1.owl";

    static String[] entityPrefixes = {"<http://www.daselab.org/ontologies/wiki#Main_topic_classification",
            "<http://www.daselab.org/ontologies/wiki#Academic_discipline",
            "<http://www.daselab.org/ontologies/wiki#Applied_discipline",
            "<http://www.daselab.org/ontologies/wiki#Engineering_discipline",
            "<http://www.daselab.org/ontologies/wiki#Subfields_by_",
            "<http://www.daselab.org/ontologies/wiki#Commons_category",
            "<http://www.daselab.org/ontologies/wiki#Categories_",
            "<http://www.daselab.org/ontologies/wiki#All_categories_",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_cate",
            "<http://www.daselab.org/ontologies/wiki#Container_ca",
            "<http://www.daselab.org/ontologies/wiki#Hidden_",
            "<http://www.daselab.org/ontologies/wiki#Tracking_",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_soft_",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_redirecting",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_redirects",
            "<http://www.daselab.org/ontologies/wiki#Public_administration",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_administration",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_",
            "<http://www.daselab.org/ontologies/wiki#Wikidata",
            "<http://www.daselab.org/ontologies/wiki#Disambiguation_categories",
            "<http://www.daselab.org/ontologies/wiki#Wikipedia_disambiguation",
            "<http://www.daselab.org/ontologies/wiki#Error",
            "<http://www.daselab.org/ontologies/wiki# ",
            "<http://www.daselab.org/ontologies/wiki#1",
            "<http://www.daselab.org/ontologies/wiki#2",
            "<http://www.daselab.org/ontologies/wiki#3",
            "<http://www.daselab.org/ontologies/wiki#4",
            "<http://www.daselab.org/ontologies/wiki#5",
            "<http://www.daselab.org/ontologies/wiki#6",
            "<http://www.daselab.org/ontologies/wiki#7",
            "<http://www.daselab.org/ontologies/wiki#8",
            "<http://www.daselab.org/ontologies/wiki#9",
            "<http://www.daselab.org/ontologies/wiki#0",
            "<http://www.daselab.org/ontologies/wiki#Words_coined",
            "<http://www.daselab.org/ontologies/wiki#Years_of_the"
    };


    public static void main(String[] args) {
        try {
            OWLOntology owlOntology = Utility.loadOntology(inputOntoPath);
            HashSet<String> entityPrefixesToRemove = new HashSet<String>(Arrays.asList(entityPrefixes));

//            if (removeConcepts(owlOntology, entityPrefixesToRemove)) {
//                Utility.saveOntology(owlOntology, outputOntoPath);
//            }

            if (removeConceptsTopK(owlOntology, 2)) {
                Utility.saveOntology(owlOntology, outputOntoPath);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
