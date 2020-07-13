package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 6/30/20.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class ConjunctiveHornClause implements IConjunctiveHornClause {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    public OWLObjectProperty owlObjectProperty;

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
     * <p>
     * <p>
     * We need to put negation sign when printing the class expression.
     * It will be printed as: ¬(D1⊔···⊔Dk)
     * <p>
     * There is a limit on disjunctions. That is ConfigParams.conceptLimitInNegExpr.
     */
    public ArrayList<OWLClassExpression> negObjectTypes;

    /**
     * OWLClassExpression
     */
    public OWLClassExpression conjunctiveHornClauseAsOWLClass;

    /**
     * String
     */
    public String conjunctiveHornClauseAsString;

    /**
     * Checker to check whether solution changed or not,
     * so we can use cached solution score and solutionAsString
     */
    public boolean solutionChanged = false;

    /**
     * Score associated with this hornclause. This score is used to select best n hornClause (limit K5), which will be used on combination.
     */
    public Score score;

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

    /**
     * Public constructor
     *
     * @param owlObjectProperty
     * @param _reasoner
     * @param _ontology
     */
    public ConjunctiveHornClause(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        if (null == owlObjectProperty) {
            this.owlObjectProperty = SharedDataHolder.noneOWLObjProp;
        } else {
            this.owlObjectProperty = owlObjectProperty;
        }

        this.negObjectTypes = new ArrayList<>();

        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();

        solutionChanged = true;
    }

    /**
     * copy constructor
     *
     * @param anotherConjunctiveHornClause
     */
    public ConjunctiveHornClause(ConjunctiveHornClause anotherConjunctiveHornClause, OWLOntology _ontology) {

        this.negObjectTypes = new ArrayList<>();
        this.owlObjectProperty = anotherConjunctiveHornClause.owlObjectProperty;
        this.negObjectTypes = anotherConjunctiveHornClause.negObjectTypes;
        if (null != anotherConjunctiveHornClause.getScore()) {
            this.score = anotherConjunctiveHornClause.getScore();
        }
        this.reasoner = anotherConjunctiveHornClause.reasoner;
        this.ontology = _ontology;
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
    @Override
    public Score getScore() {
        return score;
    }

    /**
     * Score setter
     *
     * @param score
     */
    @Override
    public void setScore(Score score) {
        this.score = score;
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

}
