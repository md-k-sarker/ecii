package org.dase.test;
/*
Written by sarker.
Written at 8/12/18.
*/

import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestUtility {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void testcombinationHelper() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(3);

        ArrayList<ArrayList<Integer>> listCombination;
        //listCombination = Utility.combinationHelper(arrayList, 1);
        listCombination = (Utility.combinationHelper(arrayList, 2));

        listCombination.forEach(integers -> {
            System.out.println("\nList: ");
            integers.forEach(integer -> {
                System.out.print(" " + integer);
            });
        });
    }

    public void testRestrictedPermutations() {

        List<Character> c1 = new ArrayList<>();
        List<Character> c2 = new ArrayList<>();

        c1.add('a');
        c1.add('b');

        c2.add('1');
        c2.add('2');

        List<Collection<Character>> origList = new ArrayList<>();
        origList.add(c1);
        origList.add(c2);

        Collection<List<Character>> result = new ArrayList<>();

        result = Utility.restrictedCombinationHelper(origList);

        result.forEach(characters -> {
            characters.forEach(character -> {
                System.out.print(character);
            });
            System.out.println();
        });
    }

    public void testLoadOntology() {

        Long startTime = System.currentTimeMillis();

        try {
            Utility.loadOntology("/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/automated_wiki/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode.rdf");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Long endTime = System.currentTimeMillis();
        Long loadTime = endTime - startTime;

        System.out.println("Ontology load time:  " + loadTime / 1000 + " seconds");
    }

    public static void testWriteToCSV() {

        ArrayList<String> colNames = new ArrayList<>();
        colNames.add("A");
        colNames.add("B");
        colNames.add("C");

        ArrayList<String> col1Vals = new ArrayList<>();
        col1Vals.add("a1");
        col1Vals.add("a2");

        ArrayList<String> col2Vals = new ArrayList<>();
        col2Vals.add("b1");
        col2Vals.add("b2");
        col2Vals.add("b3");

        ArrayList<String> col3Vals = new ArrayList<>();
        col3Vals.add("c1");
        col3Vals.add("c2");

        Utility.writeToCSV(
                "/Users/sarker/Workspaces/Jetbrains/residue-emerald/emerald/" +
                        "data/ade20k_images_and_owls/matched_objects_with_wiki_pages.csv",
                colNames, col1Vals, col2Vals, col3Vals);


    }


    public static void main(String[] args) {
        TestUtility tu = new TestUtility();
        tu.testWriteToCSV();

    }
}

