/*
Written by sarker.
Written at 8/10/18.
*/

import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.*;

public class RawTest {


    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Utility function to insert <key, value> into the Multimap
    private static <K, V> void insert(Map<K, List<V>> hashMap, K key, V value) {
        // if the key is seen for the first time, initialize the list
        if (!hashMap.containsKey(key)) {
            hashMap.put(key, new ArrayList<>());
        }

        hashMap.get(key).add(value);
        System.out.println("\nKeys: ");
        for (K kk : hashMap.keySet()) {
            System.out.print(kk);
            System.out.print("\t" + hashMap.get(kk));
        }
    }

    // Function to print all sub-arrays with 0 sum present
    // in the given array
    public static void printallSubarrays(int[] A) {
        // create an empty Multi-map to store ending index of all
        // sub-arrays having same sum
        Map<Integer, List<Integer>> hashMap = new HashMap<>();

        // insert (0, -1) pair into the map to handle the case when
        // sub-array with 0 sum starts from index 0
        insert(hashMap, 0, -1);

        int sum = 0;

        // traverse the given array
        for (int i = 0; i < A.length; i++) {
            // sum of elements so far
            sum += A[i];

            // if sum is seen before, there exists at-least one
            // sub-array with 0 sum
            if (hashMap.containsKey(sum)) {
                List<Integer> list = hashMap.get(sum);

                // find all sub-arrays with same sum
                for (Integer value : list) {
                    System.out.println("\nSubarray [" + (value + 1) + "src/test" + i + "]");
                }
            }

            // insert (sum so far, current index) pair into the Multi-map
            insert(hashMap, sum, i);
        }
    }

    public static final String noneObjPropStr = "__%!empty%!__";
    public static final OWLObjectProperty noneOWLObjProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(IRI.create(noneObjPropStr));


    public void testEntitySearcher() {
        try {
            OWLOntology owlOntology = Utility.loadOntology(
                    "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_cats_v1_non_cyclic.owl");
            OWLDataFactory owlDataFactory = owlOntology.getOWLOntologyManager().getOWLDataFactory();
            OWLClass owlClass = owlDataFactory.getOWLClass(IRI.create("http://www.daselab.com/residue/analysis#155_births"));

            logger.info("Searching super class of: " + owlClass);

            EntitySearcher.getSuperClasses(owlClass, owlOntology).forEach(owlClassExpression -> {
                logger.info("Super class: " + owlClassExpression);
            });

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // main function
    public static void main(String[] args) {

//        String a = "-Ce";
//
//        System.out.println(a.matches("-m|-e|-o|-s|-c|-M|-E|-O|-S|-C"));

        double d =0;
        int i1 = 2;
        int i2 = 3;
        d = (double)i1/i2;

        logger.info("d: "+ d);
    }

}
