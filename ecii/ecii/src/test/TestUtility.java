package org.dase.test;
/*
Written by sarker.
Written at 8/12/18.
*/

import org.dase.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        result.forEach(characters  -> {
            characters.forEach(character -> {
                System.out.print( character);
            });
            System.out.println();
        });
    }

    public static void main(String[] args) {
        TestUtility tu = new TestUtility();
        tu.testRestrictedPermutations();

    }
}

