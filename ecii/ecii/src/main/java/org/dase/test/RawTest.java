package org.dase.test;
/*
Written by sarker.
Written at 8/10/18.
*/

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.*;

public class RawTest {


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
                    System.out.println("\nSubarray [" + (value + 1) + ".." + i + "]");
                }
            }

            // insert (sum so far, current index) pair into the Multi-map
            insert(hashMap, sum, i);
        }
    }

    public static final String noneObjPropStr = "__%!dop%!__";
    public static final OWLObjectProperty noneOWLObjProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(IRI.create(noneObjPropStr));

    // main function
    public static void main(String[] args) {
//        int[] A = {1, 0, 5, -5, -1, 5};
//        // 3, 4, -7, 3, 1, 3, 1, -4, -2, -2
//        //printallSubarrays(A);
//
//        String str = "";
//        System.out.println("lenght: " + str.length());
//
//        System.out.println("noneOWLObjProp: " + noneOWLObjProp);
//
//        OWLClassExpression owlClassExpression = null;
        //owlClassExpression.getClassExpressionType()

        HashSet<String> hashSet1 = new HashSet<>();
        hashSet1.add("1");
        hashSet1.add("2");

        HashSet<String> hashSet2 = new HashSet<>();
//        hashSet2.add("2");
        hashSet2.add("3");

        hashSet2.retainAll(hashSet1);

        hashSet1.forEach(s -> {
            System.out.println(s);
        });
    }

}
