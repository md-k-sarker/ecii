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
 *  *   ObjectProperty (R) is implicity kept with the hornclause.
 *  *   This is important to calculate the accuracy.
 *
 * There is a limit  on negTypes.
 *      That is called K1 or ConfigParams.conceptLimitInNegExpr.
 * There is limit on posTypes.
 *      That is called K4 or ConfigParams.conceptLimitInPosExpr.
 *
 * Atomic class is also represented by posObjectType/negObjectTypes.
 * If the object property is empty (implemented through, SharedDataHolder.noneOWLObjProp)
 * then it is atomic class.
 *
 *
 * Difference between v0 and v1v2:
 * v0 allow to make hornClause without a positive concepts, v1/v2 make sure at-least 1 positive concept exist and possibly more.
 * there can be 0 or more negTypes
 *
 *  </pre>
 */
public class ConjunctiveHornClauseV1V2 {

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
    public ConjunctiveHornClauseV1V2(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
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
    public ConjunctiveHornClauseV1V2(ConjunctiveHornClauseV1V2 anotherConjunctiveHornClause, OWLOntology _ontology) {
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
     * Get this ConjunctiveHornClauseV01/V2 as AsOWLClassExpression
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
     * Print ConjunctiveHornClauseV0  as String
     * TODO(Zaman) : need to modify the method to cope v1, v1 fixed now.
     *
     * @return
     */
    public String getHornClauseAsString(boolean includePrefix) {

//        if (!solutionChanged && null != conjunctiveHornClauseAsString)
//            return conjunctiveHornClauseAsString;

        solutionChanged = false;

        StringBuilder sb = new StringBuilder();

        boolean hasPositive = false;

        if (null != this) {

            // print postypes
            if (null != this.getPosObjectTypes()) {
                if (this.getPosObjectTypes().size() > 0) {
                    hasPositive = true;
                    if (this.getPosObjectTypes().size() == 1) {
                        if (includePrefix)
                            sb.append(Utility.getShortNameWithPrefix((OWLClass) this.getPosObjectTypes().get(0)));
                        else sb.append(Utility.getShortName((OWLClass) this.getPosObjectTypes().get(0)));
                    } else {
                        // not using parenthesis for multiple positive types.
                        if (includePrefix)
                            sb.append(Utility.getShortNameWithPrefix((OWLClass) this.getPosObjectTypes().get(0)));
                        else sb.append(Utility.getShortName((OWLClass) this.getPosObjectTypes().get(0)));
                        for (int i = 1; i < this.getPosObjectTypes().size(); i++) {
                            sb.append(" " + AND.toString());
                            if (includePrefix)
                                sb.append(" " + Utility.getShortNameWithPrefix((OWLClass) this.getPosObjectTypes().get(i)));
                            else sb.append(" " + Utility.getShortName((OWLClass) this.getPosObjectTypes().get(i)));
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
                        if (includePrefix)
                            sb.append(" " + Utility.getShortNameWithPrefix((OWLClass) this.getNegObjectTypes().get(0)));
                        else sb.append(" " + Utility.getShortName((OWLClass) this.getNegObjectTypes().get(0)));
                    } else {
                        sb.append(" (");
                        if (includePrefix)
                            sb.append(Utility.getShortNameWithPrefix((OWLClass) this.getNegObjectTypes().get(0)));
                        else sb.append(Utility.getShortName((OWLClass) this.getNegObjectTypes().get(0)));
                        for (int i = 1; i < this.getNegObjectTypes().size(); i++) {
                            sb.append(" " + OR.toString());
                            if (includePrefix)
                                sb.append(" " + Utility.getShortNameWithPrefix((OWLClass) this.getNegObjectTypes().get(i)));
                            else sb.append(" " + Utility.getShortName((OWLClass) this.getNegObjectTypes().get(i)));
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

        logger.debug("calculating covered individuals by hornClause " + this.getConjunctiveHornClauseAsOWLClassExpression() + " by reasoner.........");
        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        OWLClassExpression owlClassExpression = this.getConjunctiveHornClauseAsOWLClassExpression();

        if (null != owlClassExpression) {
            if (!this.owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                owlClassExpression = owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, owlClassExpression);
            }

            // if we have calculated previously then just retrieve it from cache and return it.
            if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
                if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {

                    coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                    logger.debug("calculating covered individuals by candidateSolution " + this.getConjunctiveHornClauseAsOWLClassExpression() + " found in cache.");
                    logger.debug("\t size: " + coveredIndividuals.size());
                    return coveredIndividuals;
                }
            }

            // not found in cache, now expensive reasoner calls.
            coveredIndividuals = (HashSet<OWLNamedIndividual>) reasoner.getInstances(owlClassExpression, false).getFlattened();


            // save it to cache
            SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

            logger.debug("calculating covered individuals by hornClause " + this.getConjunctiveHornClauseAsOWLClassExpression() + " by reasoner finished");

            logger.debug("\t covered all individuals size: " + coveredIndividuals.size());
        }
        return coveredIndividuals;
    }


    /**
     * Calculate accuracy of a hornClause.
     * Internally  calls individualsCoveredByThisHornClauseByReasoner() method
     * to calculate the accuracy.
     *
     * TODO(zaman): need to fix to make compatible with v1
     * Difference between v0 and v1v2:
     * In V0 it calculate the covered individuals by using set calculation, no reasoner call,
     * In V1V2 it call the reasoner to get the covered individuals
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
        logger.debug("candidateClass: " + this.getHornClauseAsString(true));
        logger.debug("\tcoveredPosIndividuals_by_ecii: " + coveredPosIndividualsMap.keySet());
        logger.debug("\tcoveredPosIndividuals_by_ecii size: " + coveredPosIndividualsMap.size());
        logger.debug("\texcludedNegIndividuals_by_ecii: " + excludedNegIndividualsMap.keySet());
        logger.debug("\texcludedNegIndividuals_by_ecii size: " + excludedNegIndividualsMap.size());

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
        ConjunctiveHornClauseV1V2 that = (ConjunctiveHornClauseV1V2) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(posObjectTypes), new HashSet<>(that.posObjectTypes)) &&
                Objects.equals(new HashSet<>(negObjectTypes), new HashSet<>(that.negObjectTypes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(owlObjectProperty, new HashSet<>(posObjectTypes), new HashSet<>(negObjectTypes));
    }
}
