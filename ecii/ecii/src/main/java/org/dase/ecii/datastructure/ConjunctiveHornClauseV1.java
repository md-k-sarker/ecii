package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.Heuristics;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.*;

/**
 * <pre>
 * A conjunctive Horn clause is a class expression of the form  (R).(B ⊓ C ...⊓ ¬(D1⊔···⊔Dk)),
 *  i.e. posTypes are conjuncted and negTypes are disjuncted and complemented.
 *
 *  * Implementation note:
 *  * ObjectProperty (R) is implicity kept with the hornclause. This is important to calculate the accuracy.
 *
 * There is a limit  on negTypes.
 *      That is called K1 or ConfigParams.conceptLimitInNegExpr.
 * There is limit on posTypes.
 *      That is called K4 or ConfigParams.conceptLimitInPosExpr.
 *
 * Atomic class is also represented by posObjectType/negObjectTypes.
 * If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
 *
 *
 * Difference between v0 and v1:
 * v0 allow to make hornClause without a positive concepts, v1 make sure at-least 1 positive concept exist and possibly more. there can be 0 or more negTypes
 *
 *
 *   V0 --- old stuff
 *  * where B is an atomic class (positive) and
 *  * D is a negated disjunct of negative classes, D = ¬(D1⊔···⊔Dk).
 *  * So it's form will be: B ⊓ ¬(D1⊔···⊔Dk)
 *
 *  </pre>
 */
public class ConjunctiveHornClauseV1 {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    private OWLObjectProperty owlObjectProperty;

    //@formatter:off
    /**
     * posObjectType must be at-least 1. it can not be empty.
     *  Single positive Type.
     *      1.1. Without owlObjectProperty: in that case owlObjectProperty=SharedDataHolder.noneOWLObjProp.
     *      1.2. With owlObjectProperty:
     */
    private ArrayList<OWLClassExpression> posObjectTypes;
    /**
     * negObjectTypes represents conjuncted negated form of the horn clause.
     * it can be:
     * <li>
     *  <ol>Empty</ol>
     *  <ol>Single owlClass</ol>
     *      <li>
     *          <ol>With Object Property</ol>
     *          <ol>Without Object Property. in that case owlObjectProperty=SharedDataHolder.noneOWLObjProp</ol>
     *      </li>
     *  <ol>Multiple owlClass (concatenated using OR)</ol>
     *      <li>
     *          <ol>With Object Property</ol>
     *          <ol>Without Object Property. in that case owlObjectProperty=SharedDataHolder.noneOWLObjProp</ol>
     *      </li>
     * </li>
     *
     *
     * We need to put negation sign when printing the class expression.
     * It will be printed as: ¬(D1⊔···⊔Dk)
     *
     * There is a limit on disjunctions. That is ConfigParams.conceptLimitInNegExpr.
     */
    private ArrayList<OWLClassExpression> negObjectTypes;
    //@formatter:on

    /**
     * OWLClassExpression
     */
    private OWLClassExpression conjunctiveHornClauseAsOWLClass;

    /**
     * String
     */
    private String conjunctiveHornClauseAsString;

    private boolean solutionChanged = false;

    /**
     * Score associated with this hornclause. This score is used to select best n hornClause (limit K5), which will be used on combination.
     */
    private Score score;
    // use double to ensure when dividing we are getting double result not integer.
    transient volatile protected double nrOfPositiveClassifiedAsPositive;
    /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
    transient volatile protected double nrOfPositiveClassifiedAsNegative;
    transient volatile protected double nrOfNegativeClassifiedAsNegative;
    /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
    transient volatile protected double nrOfNegativeClassifiedAsPositive;


    /**
     * Bad design should fix it
     */
    private final OWLOntology ontology;
    private final OWLDataFactory owlDataFactory;
    private final OWLOntologyManager owlOntologyManager;
    private OWLReasoner reasoner;

    /**
     * Public constructor
     */
    public ConjunctiveHornClauseV1(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        if (null == owlObjectProperty) {
            this.owlObjectProperty = SharedDataHolder.noneOWLObjProp;
        } else {
            this.owlObjectProperty = owlObjectProperty;
        }
        this.posObjectTypes = new ArrayList<>();
        this.negObjectTypes = new ArrayList<>();
        solutionChanged = true;

        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
    }

    /**
     * copy constructor
     *
     * @param anotherConjunctiveHornClause
     */
    public ConjunctiveHornClauseV1(ConjunctiveHornClauseV1 anotherConjunctiveHornClause, OWLOntology _ontology) {
        this.posObjectTypes = new ArrayList<>();
        this.negObjectTypes = new ArrayList<>();
        this.owlObjectProperty = anotherConjunctiveHornClause.owlObjectProperty;
        this.posObjectTypes = anotherConjunctiveHornClause.posObjectTypes;
        this.negObjectTypes = anotherConjunctiveHornClause.negObjectTypes;
        if (null != anotherConjunctiveHornClause.getScore()) {
            this.score = anotherConjunctiveHornClause.getScore();
        }
        this.reasoner = anotherConjunctiveHornClause.reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
        solutionChanged = true;
    }

    /**
     * posObjectTypes getter
     *
     * @return
     */
    public ArrayList<OWLClassExpression> getPosObjectTypes() {
        return posObjectTypes;
    }

    /**
     * posObjectTypes setter
     *
     * @param posObjectTypes
     */
    public void setPosObjectTypes(HashSet<OWLClassExpression> posObjectTypes) {
        setPosObjectTypes(new ArrayList<OWLClassExpression>(posObjectTypes));
        solutionChanged = true;
    }

    /**
     * posObjectTypes getter
     *
     * @param posObjectTypes
     */
    public void setPosObjectTypes(ArrayList<OWLClassExpression> posObjectTypes) {
        this.posObjectTypes = posObjectTypes;
        solutionChanged = true;
    }

    /**
     * negObjectTypes getter
     *
     * @return
     */
    public ArrayList<OWLClassExpression> getNegObjectTypes() {
        return negObjectTypes;
    }

    /**
     * negObjectTypes setter
     *
     * @param negObjectTypes
     */
    public void setNegObjectTypes(HashSet<OWLClassExpression> negObjectTypes) {
        setNegObjectTypes(new ArrayList<OWLClassExpression>(negObjectTypes));
        solutionChanged = true;
    }

    /**
     * negObjectTypes setter
     *
     * @param negObjectTypes
     */
    public void setNegObjectTypes(ArrayList<OWLClassExpression> negObjectTypes) {
        this.negObjectTypes = negObjectTypes;
        solutionChanged = true;
    }

    /**
     * add negObjectTypes
     *
     * @param posObjectType
     */
    public void addPosObjectType(OWLClassExpression posObjectType) {
        this.posObjectTypes.add(posObjectType);
        solutionChanged = true;
    }

    /**
     * add negObjectTypes
     *
     * @param negObjectType
     */
    public void addNegObjectType(OWLClassExpression negObjectType) {
        this.negObjectTypes.add(negObjectType);
        solutionChanged = true;
    }

    /**
     * owlObjectProperty getter
     *
     * @return OWLObjectProperty
     */
    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    /**
     * Score getter
     *
     * @return Score
     */
    public Score getScore() {
        return score;
    }

    /**
     * Score setter
     *
     * @param score
     */
    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * Get this ConjunctiveHornClause as AsOWLClassExpression
     * Not filling the r filler/owlObjectProperty here.
     * V1 - fixed
     *
     * @return OWLClassExpression
     */
    public OWLClassExpression getConjunctiveHornClauseAsOWLClassExpression() {

        if (!solutionChanged && null != conjunctiveHornClauseAsOWLClass)
            return conjunctiveHornClauseAsOWLClass;

        solutionChanged = false;

        OWLClassExpression posPortion = null;
        OWLClassExpression negatedPortion = null;
        OWLClassExpression owlClassExpression = null;

        // negated portion
        if (null != this.negObjectTypes && this.negObjectTypes.size() > 0) {
            if (this.negObjectTypes.size() > 1) {
                OWLClassExpression unionsPortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(new HashSet(this.negObjectTypes));
                negatedPortion = SharedDataHolder.owlDataFactory.getOWLObjectComplementOf(unionsPortion);
            } else {
                negatedPortion = SharedDataHolder.owlDataFactory.getOWLObjectComplementOf(this.negObjectTypes.get(0));
            }
        }

        // pos portion
        if (null != this.posObjectTypes && this.posObjectTypes.size() > 0) {

            if (this.posObjectTypes.size() > 1) {
                posPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(new HashSet(this.posObjectTypes));
            } else {
                posPortion = this.posObjectTypes.get(0);
            }

            if (null != negatedPortion) {
                owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(posPortion, negatedPortion);
            } else {
                owlClassExpression = posPortion;
            }
        } else {
            owlClassExpression = negatedPortion;
        }

        conjunctiveHornClauseAsOWLClass = owlClassExpression;
        return conjunctiveHornClauseAsOWLClass;
    }


    /**
     * Print ConjunctiveHornClause  as String
     * TODO(Zaman) : need to modify the method to cope v1, v1 fixed now.
     *
     * @return
     */
    public String getHornClauseAsString() {

        if (!solutionChanged && null != conjunctiveHornClauseAsString)
            return conjunctiveHornClauseAsString;

        solutionChanged = false;

        StringBuilder sb = new StringBuilder();

        boolean hasPositive = false;

        if (null != this) {

            // print postypes
            if (null != this.getPosObjectTypes()) {
                if (this.getPosObjectTypes().size() > 0) {
                    hasPositive = true;
                    if (this.getPosObjectTypes().size() == 1) {
                        sb.append(Utility.getShortName((OWLClass) this.getPosObjectTypes().get(0)));
                    } else {
                        // not using parenthesis for multiple positive types.
                        sb.append(Utility.getShortName((OWLClass) this.getPosObjectTypes().get(0)));
                        for (int i = 1; i < this.getPosObjectTypes().size(); i++) {
                            sb.append(" " + AND.toString());
                            sb.append(" " + Utility.getShortName((OWLClass) this.getPosObjectTypes().get(i)));
                        }
                    }
                }
            }

            // print negtypes
            if (null != this.getNegObjectTypes()) {
                if (this.getNegObjectTypes().size() > 0) {
                    if (hasPositive) {
                        sb.append(" " + AND.toString());
                    }
                    sb.append(" " + NOT.toString());
                    if (this.getNegObjectTypes().size() == 1) {
                        sb.append(" " + Utility.getShortName((OWLClass) this.getNegObjectTypes().get(0)));
                    } else {
                        sb.append(" (");
                        sb.append(Utility.getShortName((OWLClass) this.getNegObjectTypes().get(0)));
                        for (int i = 1; i < this.getNegObjectTypes().size(); i++) {
                            sb.append(" " + OR.toString());
                            sb.append(" " + Utility.getShortName((OWLClass) this.getNegObjectTypes().get(i)));
                        }
                        sb.append(")");
                    }
                }
            }
        }

        conjunctiveHornClauseAsString = sb.toString();
        return conjunctiveHornClauseAsString;
    }

    /**
     * This will return all individuals covered by this complex concept from the ontology using reasoner,
     * large number of individuals may be returned.
     *
     * @return
     */
    public HashSet<OWLNamedIndividual> individualsCoveredByThisHornClauseByReasoner() {

        logger.info("calculating covered individuals by hornClause " + this.getConjunctiveHornClauseAsOWLClassExpression() + " by reasoner.........");
        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        OWLClassExpression owlClassExpression = this.getConjunctiveHornClauseAsOWLClassExpression();

        if (!this.owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
            owlClassExpression = owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, owlClassExpression);
        }

        // if we have calculated previously then just retrieve it from cache and return it.
        if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
            if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {

                coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                logger.info("calculating covered individuals by candidateSolution " + this.getConjunctiveHornClauseAsOWLClassExpression() + " found in cache.");
                logger.info("\t size: " + coveredIndividuals.size());
                return coveredIndividuals;
            }
        }

        // not found in cache, now expensive reasoner calls.
        coveredIndividuals = (HashSet<OWLNamedIndividual>) reasoner.getInstances(owlClassExpression, false).getFlattened();


        // save it to cache
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

        logger.info("calculating covered individuals by hornClause " + this.getConjunctiveHornClauseAsOWLClassExpression() + " by reasoner finished");

        logger.info("\t covered all individuals size: " + coveredIndividuals.size());

        return coveredIndividuals;
    }


    /**
     * This will return all individuals covered by this complex concept from the ontology using reasoner, by taking consideration of only the positive and negative individuals.
     * large number of individuals may be returned.
     * todo(zaman): not implemented yet.
     *
     * @return
     */
    public HashSet<OWLNamedIndividual> individualsCoveredByThisHornClauseByReasonerInstanceCheck() {

        logger.info("calculating covered individuals by hornClause " + this.getHornClauseAsString() + " by reasoner InstanceCheck.........");
        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        OWLClassExpression owlClassExpression = this.getConjunctiveHornClauseAsOWLClassExpression();

        if (!this.owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
            owlClassExpression = owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, owlClassExpression);
        }

        // if we have calculated previously then just retrieve it from cache and return it.
        if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
            if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {

                coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                logger.info("calculating covered individuals by candidateSolution " + this.getConjunctiveHornClauseAsOWLClassExpression() + " found in cache.");
                logger.info("\t size: " + coveredIndividuals.size());
                return coveredIndividuals;
            }
        }

        HashSet<OWLNamedIndividual> allPosNegIndivs = new HashSet<>();
        allPosNegIndivs.addAll(SharedDataHolder.posIndivs);
        allPosNegIndivs.addAll(SharedDataHolder.negIndivs);

        allPosNegIndivs.forEach(owlNamedIndividual -> {
            // not found by reasoner, whether an indivs is covered by this concept or not.
            // todo(zaman): not implemented yet.
        });
        // not found in cache, now expensive reasoner calls.
        coveredIndividuals = (HashSet<OWLNamedIndividual>) reasoner.getInstances(owlClassExpression, false).getFlattened();

        // save it to cache
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

        logger.info("calculating covered individuals by hornClause " + this.getHornClauseAsString() + " by reasoner InstanceCheck finished");
        logger.info("\t size: " + coveredIndividuals.size());

        return coveredIndividuals;
    }


    /**
     * This will return all individuals covered by this complex concept from the ontology using ECII system,
     * large number of individuals may be returned.
     * todo(zaman):
     *
     * @return
     */
    @Deprecated
    private HashSet<OWLNamedIndividual> individualsCoveredByThisHornClauseByECII() {

        //
        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();

        return coveredIndividuals;
    }


    /**
     * Determine whether this owlnamedIndividual contained within  this hornclause.
     * Our v1 hornclause is of this formula: R1.(B1 ⊓ B2 ⊓ B3 …. ⊓  ¬(D1 ⊔...⊔Djk)), where R1 can be empty
     * So, to satisfy, this individual must be in
     * 1. all posTypes and
     * 2. not on the negativeSide.
     * <p>
     * this is implemented using ecii system, not using reasoner.
     *
     * @param owlNamedIndividual
     * @return
     */
    @Deprecated
    public boolean isContainedInHornClause(OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {


        logger.info("Calculating isContainedInHornClause by reasoner of this hornclause: " + this.getHornClauseAsString());
        Set<OWLNamedIndividual> indivsByReasoner = reasoner.getInstances(this.getConjunctiveHornClauseAsOWLClassExpression(), false).getFlattened();
        logger.info("Calculating isContainedInHornClause by reasoner of this hornclause: " + this.getHornClauseAsString() + " finished");

        boolean contained = false;

        // if an individual exists in both pos part and neg part then it is not a valid conjunctive horn clause.
        // TODO(Zaman): need to verify our candidate solution.
        // when this condition is meeting we are still saying contained=false.

        if (this != null && owlNamedIndividual != null) {
            if (SharedDataHolder.individualHasObjectTypes.containsKey(owlNamedIndividual)) {
                HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                        individualHasObjectTypes.get(owlNamedIndividual);

                if (objPropsMap.containsKey(this.getOwlObjectProperty())) {

                    if (isPosIndiv) {
                        // is in positive side  and not in negative side
                        if (null != this.getPosObjectTypes()) {
                            // must be in allpostypes
                            int containedInPosTypeCounter = 0;
                            for (OWLClassExpression posType : this.getPosObjectTypes()) {
                                if (objPropsMap.get(this.getOwlObjectProperty()).contains(posType)) {
                                    containedInPosTypeCounter++;
                                }
                            }
                            if (this.getPosObjectTypes().size() == containedInPosTypeCounter) {
                                // make sure it is also not caintained in the negative portions
                                if (!isContainedInAnyClassExpressions(this.getNegObjectTypes(), owlNamedIndividual, this.getOwlObjectProperty())) {
                                    contained = true;
                                }
                            }
                        } else {
                            // it dont have positive. so if it is excluded by negative then it is covered. TODO: check
                        }
                    } else {
                        // negindivs : is in negative side and not in positive side
                        // if any one of the negtypes contained this type then it is contained within the negTypes.
                        boolean containedInNegPortion = false;
                        for (OWLClassExpression negType : this.getNegObjectTypes()) {
                            if (objPropsMap.get(this.getOwlObjectProperty()).contains(negType)) {
                                //totalSolPartsInThisGroupCounter++;
                                containedInNegPortion = true;
                                break;
                            }
                        }

                        if (containedInNegPortion) {
                            // need to make sure it is not in the posPortion.
                            int containedInPosTypeCounter = 0;
                            for (OWLClassExpression posType : this.getPosObjectTypes()) {
                                if (objPropsMap.get(this.getOwlObjectProperty()).contains(posType)) {
                                    containedInPosTypeCounter++;
                                }
                            }
                            // some postype may cover this individual but at-least 1 postype need to exclude this neg individual.
                            if (this.getPosObjectTypes().size() > containedInPosTypeCounter) {
                                contained = true;
                            } else {
                                // TODO(Zaman): if individual contained in both negative portion and in Positive portion then, actually we should exclude this solution.
                                logger.info("individual contained in both negative portion and in Positive portion, so we should exclude this solution.");
                            }
                        }
                    }
                }
            }
        }
        if (isPosIndiv) {
            logger.info("PosIndiv " + Utility.getShortName(owlNamedIndividual) + " is contained in this " + this.getHornClauseAsString() + ": " + contained);
        } else {
            logger.info("NegIndiv " + Utility.getShortName(owlNamedIndividual) + " is contained in this " + this.getHornClauseAsString() + ": " + contained);
        }
        return contained;
    }


    /**
     * Check whether this individual contained within any of the class expressions.
     * This is used to check positive type exclusions in negative part.
     * classes: ¬(D1⊔···⊔Dk)
     * this is implemented using ecii system, not using reasoner.
     *
     * @param classExpressions
     * @param owlNamedIndividual
     * @param owlObjectProperty
     * @return
     */
    @Deprecated
    private boolean isContainedInAnyClassExpressions(ArrayList<OWLClassExpression> classExpressions,
                                                     OWLNamedIndividual owlNamedIndividual,
                                                     OWLObjectProperty owlObjectProperty) {
        boolean contained = false;

        if (SharedDataHolder.individualHasObjectTypes.containsKey(owlNamedIndividual)) {
            HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                    individualHasObjectTypes.get(owlNamedIndividual);

            if (objPropsMap.containsKey(owlObjectProperty)) {
                for (OWLClassExpression owlClassExpression : classExpressions) {
                    if (objPropsMap.get(owlObjectProperty).contains(owlClassExpression)) {
                        contained = true;
                        break;
                    }
                }
            }
        }


        return contained;
    }


    /**
     * Calculate accuracy of a hornClause.
     * TODO(zaman): need to fix to make compatible with v1
     *
     * @return
     */
    public Score calculateAccuracyComplexCustom() {

        /**
         * Individuals covered by all parts of solution
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by all parts of solution
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        HashSet<OWLNamedIndividual> allCoveredIndividuals = this.individualsCoveredByThisHornClauseByReasoner();
        // calculate how many positive individuals covered.
        int posCoveredCounter = 0;
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {
            if (allCoveredIndividuals.contains(thisOwlNamedIndividual)) {
                posCoveredCounter++;
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }
        nrOfPositiveClassifiedAsPositive = posCoveredCounter;
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;

        // calculate how many negative individuals covered.
        int negCoveredCounter = 0;
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {
            if (allCoveredIndividuals.contains(thisOwlNamedIndividual))
                negCoveredCounter++;
            else {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
            }
        }
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsNegative = SharedDataHolder.negIndivs.size() - negCoveredCounter;
        nrOfNegativeClassifiedAsPositive = negCoveredCounter;

//        assert 2 == 1;
        assert excludedNegIndividualsMap.size() == nrOfNegativeClassifiedAsNegative;
        assert coveredPosIndividualsMap.size() == nrOfPositiveClassifiedAsPositive;

        // TODO(zaman): it should be logger.debug instead of logger.info
        logger.info("candidateClass: " + this.getHornClauseAsString());
        logger.info("\tcoveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.info("\tcoveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.info("\texcludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.info("\texcludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());

        double precision = Heuristics.getPrecision(nrOfPositiveClassifiedAsPositive, nrOfNegativeClassifiedAsPositive);
        double recall = Heuristics.getRecall(nrOfPositiveClassifiedAsPositive, nrOfPositiveClassifiedAsNegative);
        double f_measure = Heuristics.getFScore(recall, precision);
        double coverage = Heuristics.getCoverage(nrOfPositiveClassifiedAsPositive, SharedDataHolder.posIndivs.size(),
                nrOfNegativeClassifiedAsNegative, SharedDataHolder.negIndivs.size());

        Score accScore = new Score();
        accScore.setPrecision(precision);
        accScore.setRecall(recall);
        accScore.setF_measure(f_measure);
        accScore.setCoverage(coverage);

        this.score = accScore;
        return this.score;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConjunctiveHornClauseV1 that = (ConjunctiveHornClauseV1) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(posObjectTypes), new HashSet<>(that.posObjectTypes)) &&
                Objects.equals(new HashSet<>(negObjectTypes), new HashSet<>(that.negObjectTypes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(owlObjectProperty, new HashSet<>(posObjectTypes), new HashSet<>(negObjectTypes));
    }
}
