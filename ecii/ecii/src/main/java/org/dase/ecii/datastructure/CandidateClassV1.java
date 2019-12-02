package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
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
import java.util.Objects;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.AND;
import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.OR;


/**
 * <pre>
 * Candidate class is multiple conjunctive horn clause with a single Object Property.
 * 1. When we have none ObjectProperty or bare types, then horn clauses will be combined by AND/Intersection
 * 2. When we have proper ObjectProperty, then hornclauses will be combined with OR/Disjunction
 *
 * Probable Solution:
 * Solution = (A1 ¬(D1)) ⊓ (A2 ¬(D1))  ⊓  R1.((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )
 *
 * Here,
 * (A1 ¬(D1)) ⊓ (A2 ¬(D1)) can be a candidate class
 * (A1 ¬(D1)) can be a candidate class
 * ((B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) ⊔ (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk) )  can be a candidate class
 * (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)  can be a candidate class
 *
 *
 *       K2
 * C = 􏰀  ⊔  (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk))
 *      j=1
 *
 *  Here, (B1 ⊓ ... ⊓ Bn ⊓ ¬(D1 ⊔...⊔ Djk)) is a conjunctive HornClause.
 *
 *
 * There is a limit  on how many conjunctive horn clauses may be added.
 * That is called K2.
 * k2 = limit of horn clauses. = ConfigParams.hornClauseLimit.
 * </pre>
 */
public class CandidateClassV1 {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * If the object property is empty = SharedDataHolder.noneOWLObjProp then related classes are atomic class.
     */
    private OWLObjectProperty owlObjectProperty;
    /**
     * Multiple conjunctive horn clause.
     */
    private ArrayList<ConjunctiveHornClauseV1> conjunctiveHornClauses;

    /**
     * Score associated with this CandidateClass. This score is used to select best n candidateClass (limit K6), which will be used on combination.
     */
    private Score score;

    private OWLClassExpression candidateClassAsOWLClassEXpression;

    private String candidateClassAsString;

    private boolean solutionChanged = false;

    /**
     * Bad design should fix it
     */
    private final OWLOntology ontology;
    private final OWLDataFactory owlDataFactory;
    private final OWLOntologyManager owlOntologyManager;
    private OWLReasoner reasoner;

    /**
     * Public constructor
     *
     * @param owlObjectProperty
     */
    public CandidateClassV1(OWLObjectProperty owlObjectProperty, OWLReasoner _reasoner, OWLOntology _ontology) {
        if (null == owlObjectProperty) {
            this.owlObjectProperty = SharedDataHolder.noneOWLObjProp;
        } else {
            this.owlObjectProperty = owlObjectProperty;
        }
        this.conjunctiveHornClauses = new ArrayList<>();

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
     */
    public CandidateClassV1(CandidateClassV1 anotherCandidateClass, OWLOntology _ontology) {
        this.owlObjectProperty = anotherCandidateClass.owlObjectProperty;
        this.conjunctiveHornClauses = new ArrayList<>(anotherCandidateClass.conjunctiveHornClauses);

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
    public OWLObjectProperty getOwlObjectProperty() {
        return owlObjectProperty;
    }


    /**
     * @return ArrayList<ConjunctiveHornClauseV1>
     */
    public ArrayList<ConjunctiveHornClauseV1> getConjunctiveHornClauses() {
        return conjunctiveHornClauses;
    }

    /**
     * @param conjunctiveHornClauses
     */
    public void setConjunctiveHornClauses(ArrayList<ConjunctiveHornClauseV1> conjunctiveHornClauses) {
        this.conjunctiveHornClauses = conjunctiveHornClauses;
        solutionChanged = true;
    }

    /**
     * @param conjunctiveHornClause
     */
    public void addConjunctiveHornClauses(ConjunctiveHornClauseV1 conjunctiveHornClause) {
        this.conjunctiveHornClauses.add(conjunctiveHornClause);
        solutionChanged = true;
    }

    /**
     * @return
     */
    public Score getScore() {
        return score;
    }

    /**
     * @param score
     */
    public void setScore(Score score) {
        this.score = score;
    }

    /**
     * multiple horclauses are conjuncted when we have hare object property, and unioned when we have proper object property.
     * Not filling the r filler/owlObjectProperty here.
     * v0,v1 both okay.
     *
     * @return OWLClassExpression
     */
    public OWLClassExpression getCandidateClassAsOWLClassExpression() {

        if (!solutionChanged && null != candidateClassAsOWLClassEXpression)
            return candidateClassAsOWLClassEXpression;

        solutionChanged = false;

        OWLClassExpression owlClassExpression = null;

        HashSet<OWLClassExpression> conjunctiveHornClausesClassExpression = new HashSet<>();

        if (null != this.conjunctiveHornClauses) {
            // get all hornclause as class expression and make a hashset.
            for (ConjunctiveHornClauseV1 conjunctiveHornClause : this.conjunctiveHornClauses) {
                conjunctiveHornClausesClassExpression.add(conjunctiveHornClause.getConjunctiveHornClauseAsOWLClassExpression());
            }
            ArrayList<OWLClassExpression> conjunctiveHornClausesClassExpressionAList = new ArrayList<>(conjunctiveHornClausesClassExpression);

            if (conjunctiveHornClausesClassExpressionAList.size() > 0) {
                if (conjunctiveHornClausesClassExpressionAList.size() == 1) {
                    owlClassExpression = conjunctiveHornClausesClassExpressionAList.get(0);
                } else {
                    // bug-fix: for multiple conjunctivehornClause
                    // todo: (zaman): how do we handle the solution of ((SouthAsia) ⊔ (DevelopedAsia)) !!!!!!!!!
                    if (this.owlObjectProperty != SharedDataHolder.noneOWLObjProp) {
                        // make union of them
                        owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(conjunctiveHornClausesClassExpression);
                    } else {
                        // make conjunction of them
                        // todo(zaman): verify!!!!!!
                        owlClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(conjunctiveHornClausesClassExpression);
                    }
                }
            }
        }

        candidateClassAsOWLClassEXpression = owlClassExpression;
        return candidateClassAsOWLClassEXpression;
    }

    /**
     * get candidate class as String
     *
     * @return String
     */
    public String getCandidateClassAsString() {

        if (!solutionChanged && null != candidateClassAsString)
            return candidateClassAsString;

        solutionChanged = false;

        StringBuilder sb = new StringBuilder();

        if (null != this) {
            if (this.getConjunctiveHornClauses().size() > 0) {

                // as we are counting it differently for bare type and r filled type.
                String ANDOR = "";
                if (this.getOwlObjectProperty().equals(SharedDataHolder.noneOWLObjProp)) {
                    ANDOR = AND.toString();
                } else {
                    ANDOR = OR.toString();
                }

                if (this.getConjunctiveHornClauses().size() == 1) {
                    sb.append("(");
                    sb.append(this.getConjunctiveHornClauses().get(0).getHornClauseAsString());
                    sb.append(")");
                } else {

                    sb.append("(");

                    sb.append("(");
                    sb.append(this.getConjunctiveHornClauses().get(0).getHornClauseAsString());
                    sb.append(")");

                    for (int i = 1; i < this.getConjunctiveHornClauses().size(); i++) {
                        // should we use OR or AND between multiple hornClauses of same object property ?
                        // This is especially important when we have only bare type, i.e. no R-Filled type.
                        // If changed here changes must reflect in accuracy measure too.
                        // TODO:  check with Pascal.
                        sb.append(" " + ANDOR + " ");
                        sb.append("(");
                        sb.append(this.getConjunctiveHornClauses().get(i).getHornClauseAsString());
                        sb.append(")");
                    }
                    sb.append(")");
                }
            }
        }

        this.candidateClassAsString = sb.toString();
        return candidateClassAsString;
    }


    /**
     * This will return all individuals covered by this complex concept from the ontology using reasoner,
     * large number of individuals may be returned.
     *
     * @return
     */
    public HashSet<OWLNamedIndividual> individualsCoveredByThisCandidateClassByReasoner() {

        logger.info("calculating covered individuals by candidateClass " + this.getCandidateClassAsOWLClassExpression() + " by reasoner.........");

        HashSet<OWLNamedIndividual> coveredIndividuals = new HashSet<>();
        OWLClassExpression owlClassExpression = this.getCandidateClassAsOWLClassExpression();

        if (!this.owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
            owlClassExpression = owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, owlClassExpression);
        }


        // if we have calculated previously then just retrieve it from cache and return it.
        if (null != SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner) {
            if (SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.containsKey(owlClassExpression)) {
                coveredIndividuals = SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.get(owlClassExpression);
                logger.info("calculating covered individuals by candidateSolution " + this.getCandidateClassAsOWLClassExpression() + " found in cache.");
                logger.info("\t size: " + coveredIndividuals.size());
                return coveredIndividuals;
            }
        }


        // not found in cache, now expensive reasoner calls through the conjunctiveHornClauses.
        if(owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)){
            // all hornClause are conjuncted, need to make set intersection
            coveredIndividuals = conjunctiveHornClauses.get(0).individualsCoveredByThisHornClauseByReasoner();
            for(int i=1;i<conjunctiveHornClauses.size(); i++){
                coveredIndividuals.retainAll(conjunctiveHornClauses.get(i).individualsCoveredByThisHornClauseByReasoner());
            }
        }else {
            // all hornClauses are unined
            // we don't need to add r filler as r filler is being added by the conjunctiveHornClause and
            // ∃R.A ⊔ ∃R.B == ∃R.(A ⊔ B)  is true.
            for(ConjunctiveHornClauseV1 conjunctiveHornClause: conjunctiveHornClauses){
                coveredIndividuals.addAll( conjunctiveHornClause.individualsCoveredByThisHornClauseByReasoner());
            }
        }

        // save it to cache
        SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.put(owlClassExpression, coveredIndividuals);

        logger.info("calculating covered individuals by candidateClass " + this.getCandidateClassAsOWLClassExpression() + " by reasoner finished");
        logger.info("\t size: " + coveredIndividuals.size());

        return coveredIndividuals;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateClassV1 that = (CandidateClassV1) o;
        return Objects.equals(owlObjectProperty, that.owlObjectProperty) &&
                Objects.equals(new HashSet<>(conjunctiveHornClauses), new HashSet<>(that.conjunctiveHornClauses));
    }

    @Override
    public int hashCode() {
        return Objects.hash(owlObjectProperty, new HashSet<>(conjunctiveHornClauses));
    }
}
