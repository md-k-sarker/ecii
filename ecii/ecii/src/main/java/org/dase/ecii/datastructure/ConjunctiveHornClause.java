package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 6/30/20.
*/

import org.dase.ecii.core.Score;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.ArrayList;

public abstract class ConjunctiveHornClause {
    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    protected OWLObjectProperty owlObjectProperty;
    /**
     * posObjectType must be at-least 1. it can not be empty.
     *  Single positive Type.
     *      1.1. Without owlObjectProperty: in that case owlObjectProperty=SharedDataHolder.noneOWLObjProp.
     *      1.2. With owlObjectProperty:
     */
    protected ArrayList<OWLClassExpression> posObjectTypes;
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
    protected ArrayList<OWLClassExpression> negObjectTypes;
    /**
     * OWLClassExpression
     */
    protected OWLClassExpression conjunctiveHornClauseAsOWLClass;
    /**
     * String
     */
    protected String conjunctiveHornClauseAsString;
    protected boolean solutionChanged = false;
    /**
     * Score associated with this hornclause. This score is used to select best n hornClause (limit K5), which will be used on combination.
     */
    protected Score score;
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
    protected OWLOntology ontology;
    protected OWLDataFactory owlDataFactory;
    protected OWLOntologyManager owlOntologyManager;
    protected OWLReasoner reasoner;

    public ConjunctiveHornClause() {
        solutionChanged = true;
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
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

    public abstract OWLClassExpression getConjunctiveHornClauseAsOWLClassExpression();

    public abstract String getHornClauseAsString(boolean includePrefix);

    public abstract Score calculateAccuracyComplexCustom();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
