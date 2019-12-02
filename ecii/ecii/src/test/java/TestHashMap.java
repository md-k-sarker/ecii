package org.dase.test;
/*
Written by sarker.
Written at 6/5/18.
*/


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Objects;

public class TestHashMap {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

//    private static void testHashMapEqualsMethod() {
//
//        SolutionComplex solutionComplex = new SolutionComplex();
//
//        for (int i = 0; i < 3; i++) {
//            solutionComplex = new SolutionComplex();
//
//            SolutionPartComplex solutionPartComplex = new SolutionPartComplex();
//            HashSet<OWLClassExpression> posExpr = new HashSet<>();
//            if (i == 0) {
//                OWLClass posClass1 = OWLManager.getOWLDataFactory().getOWLClass("posClass1" + i);
//                posExpr.add(posClass1);
//                OWLClass posClass2 = OWLManager.getOWLDataFactory().getOWLClass("posClass2");
//                posExpr.add(posClass2);
//            } else {
//                OWLClass posClass2 = OWLManager.getOWLDataFactory().getOWLClass("posClass2" + i);
//                posExpr.add(posClass2);
//                OWLClass posClass1 = OWLManager.getOWLDataFactory().getOWLClass("posClass1");
//                posExpr.add(posClass1);
//            }
//            solutionPartComplex.setPosObjectTypes(posExpr);
//
//
//            ArrayList<OWLClassExpression> negTypeArrayList = new ArrayList<>(SharedDataHolder.typeOfObjectsInNegIndivs.keySet());
//            HashSet<OWLClassExpression> negExpr = new HashSet<>();
//            if (i == 0) {
//                OWLClass negClass1 = OWLManager.getOWLDataFactory().getOWLClass("negClass1" + i);
//                posExpr.add(negClass1);
//                OWLClass negClass2 = OWLManager.getOWLDataFactory().getOWLClass("negClass2");
//                posExpr.add(negClass2);
//            } else {
//                OWLClass negClass2 = OWLManager.getOWLDataFactory().getOWLClass("negClass2" + i);
//                posExpr.add(negClass2);
//                OWLClass negClass1 = OWLManager.getOWLDataFactory().getOWLClass("negClass1");
//                posExpr.add(negClass1);
//            }
//
//            solutionPartComplex.setNegObjectTypes(negExpr);
//            solutionComplex.addPartToSolutionComplex(solutionPartComplex);
//
//
//            // save to solutions.
//            SharedDataHolder.SolutionsSetAfterMeeting.add(solutionComplex);
//        }
//
//        System.out.println("HashMap size: " + SharedDataHolder.SolutionsSetAfterMeeting.size());
//
//        Integer[] integerArr1 = new Integer[]{1, 2, 3, 4};
//        System.out.println("hashValue1: " + Objects.hash(integerArr1));
//
//        Integer[] integerArr2 = new Integer[]{2, 1, 3, 4};
//        System.out.println("hashValue2: " + Objects.hash(integerArr2));
//
//        // int h1 =
//
//        if (Objects.hash(integerArr1) == Objects.hash(integerArr2)) {
//            System.out.println("equals: True");
//        } else {
//            System.out.println("equals: False");
//        }
//
//    }

    public static void testHashMapNullInsrtion() {
        HashMap<Integer, String> hm = new HashMap<>();

        hm.put(null, "null__");
        hm.put(1, null);

        logger.debug("size: "+hm.size());
        hm.keySet().forEach(integer -> {
            logger.debug("key: "+ integer + " \t value: "+ hm.get(integer));
        });

        logger.debug("hash of null: "+ Objects.hash(null));

    }

    public static void main(String[] args) {
        // test hashmap
        //testHashMapEqualsMethod();
        testHashMapNullInsrtion();
    }

}
