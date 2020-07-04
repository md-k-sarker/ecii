package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 7/3/20.
*/

import org.dase.ecii.core.Score;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


public abstract class CandidateSolution implements ICandidateSolution {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * candidate solution
     */
    public OWLClassExpression candidateSolutionAsOWLClass = null;

    /**
     * candidate solution as String
     */
    public String candidateSolutionAsString = null;

    public boolean solutionChanged = true;

    // Score associated with this solution
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
    protected OWLReasoner reasoner;

    public CandidateSolution(OWLReasoner _reasoner, OWLOntology _ontology) {
        solutionChanged = true;

        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
    }

    /**
     * Copy constructor
     *
     * @param anotherCandidateSolution
     */
    public CandidateSolution(CandidateSolution anotherCandidateSolution, OWLOntology _ontology) {

        if (null != anotherCandidateSolution.candidateSolutionAsOWLClass) {
            this.candidateSolutionAsOWLClass = anotherCandidateSolution.candidateSolutionAsOWLClass;
        }
        if (null != anotherCandidateSolution.candidateSolutionAsString) {
            this.candidateSolutionAsString = anotherCandidateSolution.candidateSolutionAsString;
        }
        if (null != anotherCandidateSolution.score) {
            this.score = anotherCandidateSolution.score;
        }
        solutionChanged = false;

        this.reasoner = anotherCandidateSolution.reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
    }

    /**
     * Getter
     *
     * @return
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

    public abstract OWLClassExpression getSolutionAsOWLClassExpression();

    public abstract String getSolutionAsString(boolean includePrefix);

    public abstract Score calculateAccuracyComplexCustom();

    public abstract void calculateAccuracyByReasoner();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
