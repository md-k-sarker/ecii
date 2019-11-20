package org.dase.datastructure;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.dase.core.Score;
import org.dase.core.SharedDataHolder;
import org.dase.util.Utility;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.*;

/**
 * A conjunctive Horn clause is a class expression of the form B ⊓ D,
 * where B is an atomic class (positive) and
 * D is a negated disjunct of negative classes, D = ¬(D1⊔···⊔Dk).
 * So it's form will be: B ⊓ ¬(D1⊔···⊔Dk)
 * <p>
 * There is a limit  on disjunctions.
 * That is called K1 or ConfigParams.conceptLimitInNegExpr.
 * <p>
 * Implementation note:
 * Atomic class is also represented by posObjectType/negObjectTypes.
 * If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
 * <p>
 * <p>
 * <p>
 * For conjunctive horn clause at_most 1 positive atomic class (here B) can exist and
 * any number of negative class can exist.
 * <p>
 * Difference between v0 and v1:
 * v0 allow to make hornClause without a positive concepts, v1 make sure at-least 1 positive concept exist and possibly more.
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
     * Score associated with this CandidateClass. This score is used to select best n hornClause (limit K5), which will be used on combination.
     */
    private Score score;

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
     * Public constructor
     */
    public ConjunctiveHornClauseV1(OWLObjectProperty owlObjectProperty) {
        if (null == owlObjectProperty) {
            this.owlObjectProperty = SharedDataHolder.noneOWLObjProp;
        } else {
            this.owlObjectProperty = owlObjectProperty;
        }
        this.posObjectTypes = new ArrayList<>();
        this.negObjectTypes = new ArrayList<>();
        solutionChanged = true;
    }

    /**
     * copy constructor
     *
     * @param anotherSolutionPart
     */
    public ConjunctiveHornClauseV1(ConjunctiveHornClauseV1 anotherSolutionPart) {
        this.posObjectTypes = new ArrayList<>();
        this.negObjectTypes = new ArrayList<>();
        this.owlObjectProperty = anotherSolutionPart.owlObjectProperty;
        this.posObjectTypes = anotherSolutionPart.posObjectTypes;
        this.negObjectTypes.addAll(anotherSolutionPart.negObjectTypes);
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
     * @param negObjectTypes
     */
    public void setPosObjectTypes(ArrayList<OWLClassExpression> negObjectTypes) {
        this.negObjectTypes = negObjectTypes;
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
                            sb.append(Utility.getShortName((OWLClass) this.getPosObjectTypes().get(i)));
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
     * Determine whether this owlnamedIndividual contained within  this hornclause.
     * Our v1 hornclause is of this formula: B1 ⊓ B2 ⊓ B3 …. ⊓  ¬(D1 ⊔...⊔Djk))
     * So, to satisfy, this individual must be in
     * 1. all posTypes and
     * 2. not on the negativeSide.
     * verified/unit tested for single posType without negTypes -- this function is totally okay.
     * @param owlNamedIndividual
     * @return
     */
    public boolean isContainedInHornClause(OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {

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
     *
     * @param classExpressions
     * @param owlNamedIndividual
     * @param owlObjectProperty
     * @return
     */
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
     * most probably not being used now.
     *
     * @return
     */
//    private OWLClassExpression getConjunctiveHornClauseAsClassExpression() {
//        // make disjunction of all posTypes.
//        OWLClassExpression unionOfAllPosTypeObjects = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(posObjectTypes);
//        // make disjunction of all negTypes.
//        OWLClassExpression unionOfAllNegTypeObjects = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(new HashSet<>(negObjectTypes));
//        // make complementOf the disjuncted negTypes.
//        OWLClassExpression negateduUionOfAllNegTypeObjects = SharedDataHolder.owlDataFactory.getOWLObjectComplementOf(unionOfAllNegTypeObjects);
//
//        // make conjunction of disjunctedPos and negatedDisjunctedNeg
//        OWLClassExpression conjunction = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(unionOfAllPosTypeObjects, negateduUionOfAllNegTypeObjects);
//
//        if (owlObjectProperty == SharedDataHolder.noneOWLObjProp) {
//            return conjunction;
//        } else {
//            // add r filler using r=owlObjectProperty.
//            OWLClassExpression solClass = SharedDataHolder.owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, conjunction);
//            //logger.info("solClass: " + solClass);
//            return solClass;
//        }
//    }
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
