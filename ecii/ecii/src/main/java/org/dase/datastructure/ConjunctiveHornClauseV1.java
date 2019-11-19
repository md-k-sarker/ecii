package org.dase.datastructure;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.dase.core.Score;
import org.dase.core.SharedDataHolder;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

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
 *
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
    }

    /**
     * posObjectTypes getter
     *
     * @return
     */
    public ArrayList<OWLClassExpression> getPosObjectTypes() {
        return posObjectTypes;
    }

    public void setPosObjectTypes(HashSet<OWLClassExpression> posObjectTypes) {
        setPosObjectTypes(new ArrayList<OWLClassExpression>( posObjectTypes));
    }

    public void setPosObjectTypes(ArrayList<OWLClassExpression> negObjectTypes) {
        this.negObjectTypes = negObjectTypes;
    }

    public ArrayList<OWLClassExpression> getNegObjectTypes() {
        return negObjectTypes;
    }

    public void setNegObjectTypes(HashSet<OWLClassExpression> negObjectTypes) {
        setNegObjectTypes(new ArrayList<OWLClassExpression>(negObjectTypes));
    }

    public void setNegObjectTypes(ArrayList<OWLClassExpression> negObjectTypes) {
        this.negObjectTypes = negObjectTypes;
    }

    public void addPosObjectType(OWLClassExpression posObjectType) {
        this.posObjectTypes.add(posObjectType);
    }

    public void addNegObjectType(OWLClassExpression negObjectType) {
        this.negObjectTypes.add(negObjectType);
    }

    /**
     * @return
     */
    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * V1 - fixed
     * @return
     */
    public OWLClassExpression getConjunctiveHornClauseAsOWLClassExpression() {

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
        return owlClassExpression;
    }


    /**
     * most probably not being used now.
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
