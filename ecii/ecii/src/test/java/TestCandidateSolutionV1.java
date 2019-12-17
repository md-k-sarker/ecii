/*
Written by sarker.
Written at 12/16/19.
*/

import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.datastructure.CandidateClassV1;
import org.dase.ecii.datastructure.CandidateSolutionV1;
import org.dase.ecii.datastructure.ConjunctiveHornClauseV1;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class TestCandidateSolutionV1 {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        try {
            SharedDataHolder.owlOntology = Utility.loadOntology("/Users/sarker/Desktop/concept_or_individual.owl");
            SharedDataHolder.owlOntologyManager = SharedDataHolder.owlOntology.getOWLOntologyManager();
            SharedDataHolder.owlDataFactory = SharedDataHolder.owlOntologyManager.getOWLDataFactory();

            OWLReasoner owlReasoner = Utility.initReasoner("pellet", SharedDataHolder.owlOntology, null);

            CandidateSolutionV1 candidateSolutionV1 = new CandidateSolutionV1(owlReasoner, SharedDataHolder.owlOntology);

            CandidateClassV1 candidateClassV1 = new CandidateClassV1(SharedDataHolder.noneOWLObjProp, owlReasoner, SharedDataHolder.owlOntology);

            ConjunctiveHornClauseV1 conjunctiveHornClauseV1 = new ConjunctiveHornClauseV1(SharedDataHolder.noneOWLObjProp, owlReasoner, SharedDataHolder.owlOntology);

            OWLClass classHuman = SharedDataHolder.owlDataFactory.getOWLClass(IRI.create("http://www.daselab.com/sarker/mock#Human"));

            OWLNamedIndividual indivZaman = SharedDataHolder.owlDataFactory.getOWLNamedIndividual(IRI.create("http://www.daselab.com/sarker/mock#Zaman"));

            OWLNamedIndividual indivChina = SharedDataHolder.owlDataFactory.getOWLNamedIndividual(IRI.create("http://www.daselab.com/sarker/mock#China"));

            SharedDataHolder.posIndivs.clear();
            SharedDataHolder.negIndivs.clear();
            SharedDataHolder.IndividualsOfThisOWLClassExpressionByReasoner.clear();
            SharedDataHolder.posIndivs.add(indivZaman);
            SharedDataHolder.negIndivs.add(indivChina);

            conjunctiveHornClauseV1.addPosObjectType(classHuman);
            candidateClassV1.addConjunctiveHornClauses(conjunctiveHornClauseV1);
            candidateSolutionV1.addCandidateClass(candidateClassV1);

            // print solution
            logger.info("Concept by ecii "+ candidateSolutionV1.getSolutionAsString());
            logger.info("Concept by resoner "+ candidateSolutionV1.getSolutionAsOWLClassExpression().toString());
            // calculate accuracy
           Score accScore = candidateSolutionV1.calculateAccuracyComplexCustom();
           logger.info("precision: "+ accScore.getPrecision());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
