package org.dase.ecii.core;

import org.dase.ecii.datastructure.CandidateSolutionV0;
import org.dase.ecii.datastructure.CandidateSolutionV1;
import org.dase.ecii.datastructure.CandidateSolutionV2;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SharedDataHolder {

    public static String programStartingDir;
    public static Map<String, String> prefixmap;
    public static OWLDocumentFormat owlDocumentFormat;

    public static OWLDataFactory owlDataFactory;
    public static OWLOntologyManager owlOntologyManager;
    /**
     * Original loaded ontology
     */
    public static OWLOntology owlOntologyOriginal;
    /**
     * Stripped ontology, will not be saved in memory
     */
    public static OWLOntology owlOntologyStripped;
    //public static OWLObjectProperty objPropImageContains;
    public static DLSyntaxRendererExt dlSyntaxRendererExt;

    public static String confFileFullContent;

    // by default all objectproperty have same score. based on the score we may choose to use certain object properties or not.
    public static HashMap<OWLObjectProperty, Double> objProperties = new HashMap<>();
    /**
     * Combination of objectProperties.
     */
    public static ArrayList<ArrayList<OWLObjectProperty>> objPropertiesCombination;
    public static final String noneObjPropStr = "__%!empty%!__";
    public static final OWLObjectProperty noneOWLObjProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(IRI.create(noneObjPropStr));

    /**
     * Given positive individuals
     */
    public static HashSet<OWLNamedIndividual> posIndivs = new HashSet<>();
    /**
     * Given negative individuals
     */
    public static HashSet<OWLNamedIndividual> negIndivs = new HashSet<>();

    /**
     * Objects which are mentioned in the positive individuls using some object property.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLNamedIndividual, Integer>> objectsInPosIndivs = new HashMap<>();

    /**
     * Objects which are mentioned in the negative individuls using some object property.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLNamedIndividual, Integer>> objectsInNegIndivs = new HashMap<>();

    /**
     * Types of those objects which are mentioned in the positive individuals using some object property.
     * This is used to create the solution or to refine the solution.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> typeOfObjectsInPosIndivs = new HashMap<>();

    /**
     * Sorted by integer
     * This is used to create the solution or to refine the solution.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> sortedTypeOfObjectsInPosIndivs = new HashMap<>();

    /**
     * Types of those objects which are mentioned in the negative individuals using some object property.
     * This is used to create the solution or to refine the solution.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> typeOfObjectsInNegIndivs = new HashMap<>();

    /**
     * Sorted by integer
     * This is used to create the solution or to refine the solution.
     */
    public static HashMap<OWLObjectProperty, HashMap<OWLClassExpression, Integer>> sortedTypeOfObjectsInNegIndivs = new HashMap<>();


    //@formatter:off
    /**
     *
     * Types of objects contained in the given individuals.
     * i.e. : All the types (of the objects) contained in the given inidviduals (given positive or negative individuals)
     * Example:
     *      posIndivs: posIndiv1, posindiv2.
     *      negIndivs: negIndiv1, negInidv2
     *      objectProperty: imageContains
     *
     *      Objects inside posIndivs and negIndivs using objectProperty:
     *          posIndiv1 imageContains obj1,
     *                                  obj2.
     *          posIndiv2 imageContains obj1,
     *                                  obj3,
     *          negIndiv1 imageContains ob4,
     *                                  ob5.
     *          negIndiv2 imageContains ob5,
     *                                  ob6.
     *          type(obj1) = t1, t7
     *          type(obj2) = t2, t8
     *          ..............
     *          type(obj6) = t6, t9
     *
     *      then, this hashmap will contain,
     *      HashMap<posIndiv1, <t1,t2,t7>>
     *      HashMap<posIndiv2, <t1,t3,t9>>
     *      HashMap<negIndiv2, <t5,t6,t9>>
     *          etc...
     *
     *
     * r1.concept1 has indi1
     * r2.concept2 has indi2
     * solution may create  r1.concept2 which is not acceptable
     * so we need to keep track of object proerties too.
     * this is used to calculate accuracy. This includes the direct types and super class of direct types.
     */
    //@formatter:on
    public static HashMap<OWLNamedIndividual, HashMap<OWLObjectProperty, HashSet<OWLClassExpression>>> individualHasObjectTypes = new HashMap<>();


    // score is inside of a solution
    // i.e. solution class contains the score also.
    public static HashSet<CandidateSolutionV0> CandidateSolutionSetV0 = new HashSet<>();
    public static HashSet<CandidateSolutionV1> CandidateSolutionSetV1 = new HashSet<>();
    public static HashSet<CandidateSolutionV2> CandidateSolutionSetV2 = new HashSet<>();

    public static ArrayList<CandidateSolutionV0> SortedCandidateSolutionListV0 = new ArrayList<>();
    public static ArrayList<CandidateSolutionV1> SortedCandidateSolutionListV1 = new ArrayList<>();
    public static ArrayList<CandidateSolutionV2> SortedCandidateSolutionListV2 = new ArrayList<>();

    public static ArrayList<CandidateSolutionV0> SortedByReasonerCandidateSolutionListV0 = new ArrayList<>();


    // cache all reasoner calls, specifically finding individuals of an owlClassExpression must be cached.
    public static HashMap<OWLClassExpression, HashSet<OWLNamedIndividual>> IndividualsOfThisOWLClassExpressionByReasoner = new HashMap<>();

    // cache all accuracy call, by ecii
    public static HashMap<OWLClassExpression, HashSet<OWLNamedIndividual>> IndividualsOfThisOWLClassExpressionByECII = new HashMap<>();

    /**
     * To be used to calculate accuracy by reasoner, where we need to create new IRI using this function getUniqueIRI()
     */
    public static int newIRICounter = 0;

    /**
     * Size of positive types (direct+indirect)
     * Being used to calculate the size of solution,
     * so we can have an estimate of time to generate the solutions.
     */
    public static int TotalPosTypes = 0;

    /**
     * Size of negative types (direct+indirect)
     * Being used to calculate the size of solution,
     * so we can have an estimate of time to generate the solutions.
     */
    public static int TotalNegTypes = 0;

    /**
     * Size of positive types (direct+indirect) after removing the common types.
     * If we dont remove then it's size is 0
     * <p>
     * Being used to calculate the size of solution,
     * so we can have an estimate of time to generate the solutions.
     */
    public static int TotalPosTypesAfterRemoval = 0;

    /**
     * Size of negative types (direct+indirect) after removing the common types.
     * If we dont remove then it's size is 0
     * <p>
     * Being used to calculate the size of solution,
     * so we can have an estimate of time to generate the solutions.
     */
    public static int TotalNegTypesAfterRemoval = 0;

    /**
     * clean the shared data holders.
     * should be called before starting the each induction operation.
     */
    public static void cleanSharedDataHolder() {

        SharedDataHolder.objProperties.clear();
        SharedDataHolder.posIndivs.clear();
        SharedDataHolder.negIndivs.clear();

        SharedDataHolder.objectsInPosIndivs.clear();
        SharedDataHolder.objectsInNegIndivs.clear();

        SharedDataHolder.typeOfObjectsInPosIndivs = new HashMap<>();
        SharedDataHolder.typeOfObjectsInNegIndivs = new HashMap<>();

        SharedDataHolder.sortedTypeOfObjectsInNegIndivs = new HashMap<>();
        SharedDataHolder.sortedTypeOfObjectsInNegIndivs = new HashMap<>();

        SharedDataHolder.individualHasObjectTypes = new HashMap<>();

        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.clear();
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByECII.clear();

        SharedDataHolder.SortedByReasonerCandidateSolutionListV0.clear();

        SharedDataHolder.CandidateSolutionSetV0.clear();
        // HashMap<Solution:solution,Boolean:shouldTraverse> SolutionsMap
        SharedDataHolder.SortedCandidateSolutionListV0.clear();

        SharedDataHolder.CandidateSolutionSetV1.clear();
        SharedDataHolder.SortedCandidateSolutionListV1.clear();

        SharedDataHolder.CandidateSolutionSetV2.clear();
        SharedDataHolder.SortedCandidateSolutionListV2.clear();

        SharedDataHolder.newIRICounter = 0;
    }


}
