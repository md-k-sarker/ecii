package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.dase.ecii.core.HashMapUtility;
import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.Heuristics;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
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
 */
public class ConjunctiveHornClauseV0 extends ConjunctiveHornClause {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //@formatter:off
    /**
     * posObjectType can be be at most 1. or it can be empty.
     * 1. Empty
     * 2. Single positive Type.
     *      2.1. Without owlObjectProperty: in that case owlObjectProperty=SharedDataHolder.noneOWLObjProp.
     *      2.2. With owlObjectProperty:
     */
    private OWLClassExpression posObjectType;
    //@formatter:on

    /**
     * Public constructor
     */
    public ConjunctiveHornClauseV0(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        super(owlObjectProperty, _reasoner, _ontology);
    }

    /**
     * copy constructor
     *
     * @param anotherConjunctiveHornClause
     */
    public ConjunctiveHornClauseV0(ConjunctiveHornClauseV0 anotherConjunctiveHornClause, OWLOntology _ontology) {
        super(anotherConjunctiveHornClause, _ontology);

        this.posObjectType = anotherConjunctiveHornClause.posObjectType;
    }

    /**
     * posObjectTypes getter
     *
     * @return
     */
    public OWLClassExpression getPosObjectType() {
        return posObjectType;
    }

    /**
     * posObjectType setter
     *
     * @param posObjectType
     */
    public void setPosObjectType(OWLClassExpression posObjectType) {
        this.posObjectType = posObjectType;
    }

    /**
     * @return OWLClassExpression
     */
    @Override
    public OWLClassExpression getConjunctiveHornClauseAsOWLClassExpression() {

        OWLClassExpression owlClassExpression = null;
        OWLClassExpression negatedPortion = null;

        // negated portion
        if (null != this.negObjectTypes && this.negObjectTypes.size() > 0) {
            if (this.negObjectTypes.size() > 1) {
                OWLClassExpression unionsPortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(new HashSet(this.negObjectTypes));
                negatedPortion = SharedDataHolder.owlDataFactory.getOWLObjectComplementOf(unionsPortion);
            } else {
                negatedPortion = SharedDataHolder.owlDataFactory.getOWLObjectComplementOf(this.negObjectTypes.get(0));
            }
        }

        if (null != this.posObjectType) {
            if (null != negatedPortion) {
                owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(this.posObjectType, negatedPortion);
            } else {
                owlClassExpression = this.posObjectType;
            }
        } else {
            owlClassExpression = negatedPortion;
        }
        return owlClassExpression;
    }

    /**
     * Print ConjunctiveHornClauseV0  as String
     *
     * @return
     */
    @Override
    public String getHornClauseAsString(boolean includePrefix) {
        StringBuilder sb = new StringBuilder();

        boolean hasPositive = false;

        if (null != this) {

            if (null != this.getPosObjectType()) {
                sb.append(Utility.getShortName((OWLClass) this.getPosObjectType()));
                hasPositive = true;
            }
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

        return sb.toString();
    }

    transient volatile protected int nrOfTotalIndividuals;
    transient volatile protected int nrOfPositiveIndividuals;
    transient volatile protected int nrOfNegativeIndividuals;

    /**
     * Determine whether this owlnamedIndividual contained within this .
     *
     * @param owlNamedIndividual
     * @param isPosIndiv
     * @return boolean
     */
    public boolean isContainedInHornClause(OWLNamedIndividual owlNamedIndividual, boolean isPosIndiv) {

        boolean contained = false;

        if (this != null && owlNamedIndividual != null) {
            if (SharedDataHolder.individualHasObjectTypes.containsKey(owlNamedIndividual)) {
                HashMap<OWLObjectProperty, HashSet<OWLClassExpression>> objPropsMap = SharedDataHolder.
                        individualHasObjectTypes.get(owlNamedIndividual);

                if (objPropsMap.containsKey(this.getOwlObjectProperty())) {

                    if (isPosIndiv) {
                        if (null != this.getPosObjectType()) {
                            // is in positive side  and not in negative side
                            if (objPropsMap.get(this.getOwlObjectProperty()).contains(this.getPosObjectType()) &&
                                    !isContainedInAnyClassExpressions(this.getNegObjectTypes(), owlNamedIndividual, this.getOwlObjectProperty())) {
                                contained = true;
                            }
                        } else {
                            // it dont have positive. so if it is excluded by negative then it is covered. TODO: check
                        }
                    } else {
                        // negindivs
                        // if any one of the negtypes contained this type then it is contained within the negTypes.
                        if (!objPropsMap.get(this.getOwlObjectProperty()).contains(this.getPosObjectType())) {
                            for (OWLClassExpression negType : this.getNegObjectTypes()) {
                                if (objPropsMap.get(this.getOwlObjectProperty()).contains(negType)) {
                                    //totalSolPartsInThisGroupCounter++;
                                    contained = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
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
     * Calculate accuracy of a hornClause.
     * it calculate the covered individuals by using set calculation, no reasoner call
     *
     * @return Score
     */
    public Score calculateAccuracyComplexCustom() {

        /**
         * Individuals covered by this hornClause
         */
        HashMap<OWLIndividual, Integer> coveredPosIndividualsMap = new HashMap<>();
        /**
         * Individuals excluded by this hornClause
         */
        HashMap<OWLIndividual, Integer> excludedNegIndividualsMap = new HashMap<>();

        /**
         * For positive individuals, a individual must be contained within each AND section to be added as a coveredIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.posIndivs) {

            if (isContainedInHornClause(thisOwlNamedIndividual, true)) {
                HashMapUtility.insertIntoHashMap(coveredPosIndividualsMap, thisOwlNamedIndividual);
            }
        }

        /**
         * For negative individuals, a individual must be contained within any single section to be added as a excludedIndividuals.
         * I.e. each
         */
        for (OWLNamedIndividual thisOwlNamedIndividual : SharedDataHolder.negIndivs) {

            if (isContainedInHornClause(thisOwlNamedIndividual, false)) {
                HashMapUtility.insertIntoHashMap(excludedNegIndividualsMap, thisOwlNamedIndividual);
            }
        }

        nrOfPositiveClassifiedAsPositive = coveredPosIndividualsMap.size();
        /* nrOfPositiveClassifiedAsNegative = nrOfPositiveIndividuals - nrOfPositiveClassifiedAsPositive */
        nrOfPositiveClassifiedAsNegative = SharedDataHolder.posIndivs.size() - nrOfPositiveClassifiedAsPositive;
        nrOfNegativeClassifiedAsNegative = excludedNegIndividualsMap.size();
        /* nrOfNegativeClassifiedAsPositive = nrOfNegativeIndividuals - nrOfNegativeClassifiedAsNegative */
        nrOfNegativeClassifiedAsPositive = SharedDataHolder.negIndivs.size() - nrOfNegativeClassifiedAsNegative;

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


        return accScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConjunctiveHornClauseV0 that = (ConjunctiveHornClauseV0) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(posObjectType, that.posObjectType) &&
                Objects.equals(new HashSet<>(negObjectTypes), new HashSet<>(that.negObjectTypes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(owlObjectProperty, posObjectType, new HashSet<>(negObjectTypes));
    }
}
