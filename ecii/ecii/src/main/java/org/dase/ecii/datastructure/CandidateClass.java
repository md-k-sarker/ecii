package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 7/1/20.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public abstract class CandidateClass implements ICandidateClass {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    public OWLObjectProperty owlObjectProperty;

    /**
     * OWLClassExpression
     */
    public OWLClassExpression candidateClassAsOWLClassExpression;

    /**
     * String
     */
    public String candidateClassAsString;

    /**
     * Checker to check whether solution changed or not,
     * so we can use cached solution score and solutionAsString
     */
    public boolean solutionChanged = false;

    /**
     * Score associated with this CandidateClassV0. This score is used to select best n candidateClass (limit K6), which will be used on combination.
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
    public final OWLOntology ontology;
    public final OWLDataFactory owlDataFactory;
    public final OWLOntologyManager owlOntologyManager;
    public OWLReasoner reasoner;

    /**
     * Public constructor
     *
     * @param owlObjectProperty
     * @param _reasoner
     * @param _ontology
     */
    public CandidateClass(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        if (null == owlObjectProperty) {
            this.owlObjectProperty = SharedDataHolder.noneOWLObjProp;
        } else {
            this.owlObjectProperty = owlObjectProperty;
        }

        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();

        solutionChanged = true;
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateClass
     * @param _ontology
     */
    public CandidateClass(CandidateClass anotherCandidateClass, OWLOntology _ontology) {

        this.owlObjectProperty = anotherCandidateClass.owlObjectProperty;
        this.reasoner = anotherCandidateClass.reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();

        solutionChanged = true;
    }

    /**
     * owlObjectProperty getter
     *
     * @return OWLObjectProperty
     */
    @Override
    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }

    /**
     * @return
     */
    @Override
    public Score getScore() {
        return score;
    }

    /**
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
