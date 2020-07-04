package org.dase.ecii.core;
/*
Written by sarker.
Written at 12/13/19.
*/

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;

public class HashMapUtility {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to enter data into:
     *  1. SharedDataHolder.objectsInPosIndivs or
     *  2. SharedDataHolder.objectsInNegIndivs or
     *  3. SharedDataHolder.typeOfObjectsInPosIndivs or
     *  4. SharedDataHolder.typeOfObjectsInNegIndivs
     * @param hashMap : HashMap<OWLObjectProperty, HashMap<T, Integer>>
     * @param objProp
     * @param data
     * @param <T>
     */
    //@formatter:on
    public static <T> void insertIntoHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data) {
        if (null == hashMap || null == objProp || null == data) {
            logger.error("Null key or value is now allowed in hashMap in insertIntoHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data)");
            return;
        }
        if (hashMap.containsKey(objProp)) {
            if (hashMap.get(objProp).containsKey(data)) {
                hashMap.get(objProp).put(data, hashMap.get(objProp).get(data) + 1);
            } else {
                hashMap.get(objProp).put(data, 1);
            }
        } else {
            HashMap<T, Integer> tmpHashMap = new HashMap<>();
            tmpHashMap.put(data, 1);
            hashMap.put(objProp, tmpHashMap);
        }
    }


    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to insert data into:
     *  SharedDataHolder.individualHasObjectTypes
     * @param hashMap : HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>>
     * @param individual
     * @param owlObjectProperty
     * @param owlClassExpression
     */
    //@formatter:on
    public static void insertIntoHashMap(HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>> hashMap,
                                         OWLNamedIndividual individual, OWLObjectProperty owlObjectProperty, OWLClassExpression owlClassExpression) {
        if (null == hashMap || null == individual || null == owlObjectProperty || null == owlClassExpression) {
            logger.error("Null key or value is now allowed in hashMap in insertIntoHashMap(HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>> hashMap, OWLNamedIndividual individual, OWLObjectProperty owlObjectProperty, OWLClassExpression owlClassExpression)");
            return;
        }
        if (hashMap.containsKey(individual)) {
            if (hashMap.get(individual).containsKey(owlObjectProperty)) {
                hashMap.get(individual).get(owlObjectProperty).add(owlClassExpression);
            } else {
                HashSet<OWLClassExpression> tmpHashSet = new HashSet<>();
                tmpHashSet.add(owlClassExpression);
                hashMap.get(individual).put(owlObjectProperty, tmpHashSet);
            }
        } else {
            HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> tmpHashMap = new HashMap<>();
            HashSet<OWLClassExpression> tmpHashSet = new HashSet<>();
            tmpHashSet.add(owlClassExpression);
            tmpHashMap.put(owlObjectProperty, tmpHashSet);
            hashMap.put(individual, tmpHashMap);
        }
    }


    /**
     * Utility/Helper method to insert entry into HashMap
     * Used for coveredPosIndividualsMap/excludedNegIndividualsMap
     *
     * @param hashMap
     * @param data
     */
    public static <T> void insertIntoHashMap(HashMap<T, Integer> hashMap, T data) {
        if (null == hashMap || null == data) {
            logger.error("Null key or value is now allowed in hashMap in insertIntoHashMap(HashMap<T, Integer> hashMap, T data)");
            return;
        }
        if (hashMap.containsKey(data)) {
            hashMap.put(data, hashMap.get(data) + 1);
        } else {
            hashMap.put(data, 1);
        }
    }

    /**
     * Utility/Helper method to insert entry into HashSet
     * Used for hornClauseHashMap
     *
     * @param hashMap
     * @param <T1>
     * @param <T2>
     */
    public static <T1, T2> void insertIntoHashMap(HashMap<T1, HashSet<T2>> hashMap, T1 key, T2 data) {
        if (null == hashMap || null == key || null == data) {
            logger.error("Null key or value is now allowed in hashMap in insertIntoHashMap(HashMap<T1, HashSet<T2>> hashMap, T1 key, T2 data)");
            return;
        }
        if (hashMap.containsKey(key)) {
            hashMap.get(key).add(data);
        } else {
            HashSet<T2> tmpHashSet = new HashSet<>();
            tmpHashSet.add(data);
            hashMap.put(key, tmpHashSet);
        }
    }


    //@formatter:off
    /**
     * Utility/Helper method to insert entry into HashMap.
     * Will be used to modify data of the:
     *  1. SharedDataHolder.objectsInPosIndivs or
     *  2. SharedDataHolder.objectsInNegIndivs or
     *  3. SharedDataHolder.typeOfObjectsInPosIndivs or
     *  4. SharedDataHolder.typeOfObjectsInNegIndivs
     * @param hashMap : HashMap<OWLObjectProperty, HashMap<T, Integer>>
     * @param objProp
     * @param data
     * @param <T>
     */
    //@formatter:on
    public static <T> void modifyHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data) {
        if (null == hashMap || null == objProp || null == data) {
            logger.error("Null key or value is now allowed in hashmap in modifyHashMap(HashMap<OWLObjectProperty, HashMap<T, Integer>> hashMap, OWLObjectProperty objProp, T data)");
            return;
        }
        if (hashMap.containsKey(objProp)) {
            if (hashMap.get(objProp).containsKey(data)) {
                hashMap.get(objProp).put(data, hashMap.get(objProp).get(data) + 1);
            } else {
                hashMap.get(objProp).put(data, 1);
            }
        } else {
            HashMap<T, Integer> tmpHashMap = new HashMap<>();
            tmpHashMap.put(data, 1);
            hashMap.put(objProp, tmpHashMap);
        }
    }

}
