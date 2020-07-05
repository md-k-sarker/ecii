package org.dase.ecii.core;
/*
Written by sarker.
Written at 4/23/20.
*/

import org.dase.ecii.datastructure.CandidateSolutionV2;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Class to compute the similarity,
 * V2 is implemented till now.
 */
public class Similarity {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Monitor monitor;

    /**
     * Default 0.0
     */
    private double defaultAccuracyInitialMax;

    private OWLReasoner owlReasoner;

    public Similarity(Monitor monitor, double defaultAccuracyInitialMax, OWLReasoner owlReasoner) {
        this.monitor = monitor;
        this.defaultAccuracyInitialMax = defaultAccuracyInitialMax;
        this.owlReasoner = owlReasoner;
    }


    /**
     * Find similarity score of a new IFP with respect to the solutions.
     * Steps:
     * option 1: Find all the types in that IFP.
     * If all types are subsumed by a solution then add that solution. Calculate each concept at a time, dont make new concpet by: concept1 and concept 2
     * option 2: Calculate if the ifp is subsumed by the solutions.
     * here, Subsume(i│s)=Individual i is of s type
     * If the ifp are subsumed by a solution then add the accuracy of the solutions.
     * probability(i)=  (sum of all accuracies of solutions subsuming i)/(Total no. of solutions in m)
     * probability(j)=  (sum of all accuracies of solutions subsuming j)/(Total no. of solutions in m)
     * similarity(i,j)=1-abs(probability(i)-probability(j))
     * <p>
     * option 2 is implemented
     */
    public double findSimilarityIndivWithAnotherIndiv(OWLNamedIndividual posOwlNamedIndividual, OWLNamedIndividual negOwlNamedIndividual) {

        if (null == posOwlNamedIndividual || null == negOwlNamedIndividual)
            return -1;

        logger.info("Finding similarity of posOwlNamedIndividual: " + posOwlNamedIndividual + " started...............");
        monitor.displayMessage("\nFinding similarity of posOwlNamedIndividual: " + posOwlNamedIndividual + " started............", true);

        int nullClassCounter = 0;
        if (SharedDataHolder.SortedCandidateSolutionListV2.size() > 0) {
            defaultAccuracyInitialMax = SharedDataHolder.SortedCandidateSolutionListV2.get(0).getScore().getDefaultScoreValue();
        }
        monitor.displayMessage("\nMaximum accuracy (" + ConfigParams.scoreTypeNameRaw + ") of solutions: " + defaultAccuracyInitialMax, true);

        ArrayList<CandidateSolutionV2> solutions_with_max_accuracy = new ArrayList<>(
                SharedDataHolder.SortedCandidateSolutionListV2.stream().filter(
                        candidateSolutionV1V2 -> candidateSolutionV1V2.getScore().getDefaultScoreValue() == defaultAccuracyInitialMax)
                        .collect(Collectors.toList()));
        monitor.displayMessage("\nTotal solutions with accuracy (" + ConfigParams.scoreTypeNameRaw + ") "
                + defaultAccuracyInitialMax + " are " + solutions_with_max_accuracy.size(), true);


        logger.debug("Looking subsumed for individual: " + posOwlNamedIndividual.getIRI() + " started...............");
        double accuracy_total_for_pos_indiv = 0;
        double accuracy_total_for_neg_indiv = 0;
        double accuracy_probability_for_pos_indiv = 0;
        double accuracy_probability_for_neg_indiv = 0;

        for (CandidateSolutionV2 candidateSolutionV2 : solutions_with_max_accuracy) {

            if (null != candidateSolutionV2.getSolutionAsOWLClassExpression()) {
                logger.debug("started looking subsumed by candidate solution: " + candidateSolutionV2.getSolutionAsString(true));

                HashSet<OWLNamedIndividual> namedIndividualHashSet = new HashSet<>(owlReasoner.getInstances(
                        candidateSolutionV2.getSolutionAsOWLClassExpression(), false).getFlattened());

                if (namedIndividualHashSet.contains(posOwlNamedIndividual)) {
                    logger.debug(Utility.getShortNameWithPrefix(posOwlNamedIndividual) +
                            " is subsumed by solution " + candidateSolutionV2.getSolutionAsString(true));
                    accuracy_total_for_pos_indiv += candidateSolutionV2.getScore().getDefaultScoreValue();
                }
                if (namedIndividualHashSet.contains(negOwlNamedIndividual)) {
                    logger.debug(Utility.getShortNameWithPrefix(posOwlNamedIndividual) +
                            " is subsumed by solution " + candidateSolutionV2.getSolutionAsString(true));
                    accuracy_total_for_neg_indiv += candidateSolutionV2.getScore().getDefaultScoreValue();
                }
            } else {
                nullClassCounter++;
            }
        }


        logger.debug("Subsumed total sum of accuracy for ifp: " + posOwlNamedIndividual + ": " + accuracy_total_for_pos_indiv);
        monitor.displayMessage("Subsumed sum of accuracy  (" + ConfigParams.scoreTypeNameRaw + ") for ifp: "
                + posOwlNamedIndividual + ": " + accuracy_total_for_pos_indiv, true);

        if (SharedDataHolder.SortedCandidateSolutionListV2.size() - nullClassCounter != 0) {
            accuracy_probability_for_pos_indiv = accuracy_total_for_pos_indiv / SharedDataHolder.SortedCandidateSolutionListV2.size() - nullClassCounter;
            accuracy_probability_for_neg_indiv = accuracy_total_for_neg_indiv / SharedDataHolder.SortedCandidateSolutionListV2.size() - nullClassCounter;
        } else {
            accuracy_probability_for_pos_indiv = 0;
            accuracy_probability_for_neg_indiv = 0;
        }

        double similarity = 1 - Math.abs((accuracy_probability_for_pos_indiv - accuracy_probability_for_neg_indiv));
        logger.debug("Subsumed avg of accuracy for ifp: " + posOwlNamedIndividual + ": " + accuracy_probability_for_pos_indiv);
        logger.debug("Subsumed average sum of accuracy (" + ConfigParams.scoreTypeNameRaw + ") for ifp: "
                + posOwlNamedIndividual + ": " + accuracy_probability_for_pos_indiv, true);

        logger.debug("Looking subsumed for individual " + posOwlNamedIndividual.getIRI() + " finished ");

        monitor.displayMessage("\n\n##################################", true);
        monitor.displayMessage("Similarity score of posOwlNamedIndividual " + Utility.getShortName(posOwlNamedIndividual) + " with IFP : " + Utility.getShortName(negOwlNamedIndividual) + ": " + similarity, true);
        monitor.displayMessage("##################################\n\n", true);

        logger.info("Finding similarity of posOwlNamedIndividual: " + Utility.getShortName(posOwlNamedIndividual) + " finished");
        monitor.displayMessage("Finding similarity finished. ", false);


        return similarity;
    }


    /**
     * Find similarity score of a new IFP with respect to the solutions.
     * <p>
     * Solutions are made by the set of IFPs.
     * To compare ifp i with a set s use set s (ifps) as positive and other set n as negative.
     * to compare ifp i with other set n, use set n (ifps) as positive and other set s as negative
     * <p>
     * Steps:
     * option 1: Find all the types in that IFP.
     * If all types are subsumed by a solution then add that solution. Calculate each concept at a time, dont make new concpet by: concept1 and concept 2
     * option 2: Calculate if the ifp is subsumed by the solutions.
     * here, Subsume(i│s)=Individual i is of s type
     * If the ifp are subsumed by a solution then add the accuracy of the solutions.
     * probability(i)=  (sum of all accuracies of solutions subsuming i)/(Total no. of solutions in m)
     * probability(j)=  (sum of all accuracies of solutions subsuming j)/(Total no. of solutions in m)
     * similarity(i,j)=1-abs(probability(i)-probability(j))
     * <p>
     * option 2 is implemented
     */
    public void findSimilarityMultipleIFPsWithASetOfIFPs(String new_indivs_path) throws IOException {

        logger.info("Finding similarity started...............");

        monitor.displayMessage("\nFinding similarity of individuals ", true);
        FileReader fileReader = new FileReader(new_indivs_path);
        BufferedReader bf = new BufferedReader(fileReader);

        String[] strs = bf.readLine().split(",");

        ArrayList<OWLNamedIndividual> owlNamedIndividualHashSet = new ArrayList<>();

        for (String str : strs) {
            IRI iri = IRI.create(str);
            OWLNamedIndividual owlNamedIndividual = SharedDataHolder.owlDataFactory.getOWLNamedIndividual(iri);
            owlNamedIndividualHashSet.add(owlNamedIndividual);
        }

        if (SharedDataHolder.SortedCandidateSolutionListV2.size() > 0) {
            defaultAccuracyInitialMax = SharedDataHolder.SortedCandidateSolutionListV2.get(0).getScore().getDefaultScoreValue();
        }
        monitor.displayMessage("\nMaximum accuracy (" + ConfigParams.scoreTypeNameRaw + ") of solutions: " + defaultAccuracyInitialMax, true);

        ArrayList<CandidateSolutionV2> solutions_with_max_accuracy = new ArrayList<>(
                SharedDataHolder.SortedCandidateSolutionListV2.stream().filter(
                        CandidateSolutionV2 -> CandidateSolutionV2.getScore().getDefaultScoreValue() == defaultAccuracyInitialMax)
                        .collect(Collectors.toList()));
        monitor.displayMessage("\nTotal solutions with accuracy (" + ConfigParams.scoreTypeNameRaw + ") "
                + defaultAccuracyInitialMax + " are " + solutions_with_max_accuracy.size(), true);

        double accuracy_total_for_all_indiv = 0;
        double accuracy_avg_for_all_indiv = 0;

        if (owlNamedIndividualHashSet.size() <= 0)
            return;

        logger.info("Caching of subsumed Indivs started...........");
        // cache the subsumed individuals
        HashMap<CandidateSolutionV2, HashSet<OWLNamedIndividual>> subsumedIndivsCache = new HashMap<>();
        for (CandidateSolutionV2 candidateSolutionV2 : solutions_with_max_accuracy) {
            HashSet<OWLNamedIndividual> subsumedIndivs = new HashSet<>(owlReasoner.getInstances(
                    candidateSolutionV2.getSolutionAsOWLClassExpression(), false).getFlattened());
            subsumedIndivsCache.put(candidateSolutionV2, subsumedIndivs);
        }
        logger.info("Caching of subsumed Indivs finished");

        monitor.displayMessage("Total IFP: " + owlNamedIndividualHashSet.size(), true);
        for (OWLNamedIndividual individual : owlNamedIndividualHashSet) {
            logger.debug("Started looking subsumed for individual: " + individual.getIRI());
            double accuracy_total_for_single_indiv = 0;
            double accuracy_avg_for_single_indiv = 0;
            for (CandidateSolutionV2 candidateSolutionV2 : solutions_with_max_accuracy) {
                logger.debug("started looking subsumed by candidate solution: " + candidateSolutionV2.getSolutionAsString(true));

                if (subsumedIndivsCache.get(candidateSolutionV2).contains(individual)) {
                    logger.debug(Utility.getShortNameWithPrefix(individual) +
                            " is subsumed by solution " + candidateSolutionV2.getSolutionAsString(true));
                    accuracy_total_for_single_indiv += candidateSolutionV2.getScore().getDefaultScoreValue();
                }

            }
            logger.debug("accuracy_total_for_single_indiv: " + accuracy_total_for_single_indiv);
            accuracy_avg_for_single_indiv = accuracy_total_for_single_indiv / SharedDataHolder.SortedCandidateSolutionListV2.size();
            logger.debug("accuracy_avg_for_single_indiv: " + accuracy_avg_for_single_indiv);

            accuracy_total_for_all_indiv += accuracy_avg_for_single_indiv;
            logger.debug("started looking subsumed for individual " + individual.getIRI() + " finished ");

            monitor.displayMessage(" Similarity score of IFP " + Utility.getShortName(individual) + " with respect to group 2: " + accuracy_avg_for_single_indiv, true);
        }

        accuracy_avg_for_all_indiv = accuracy_total_for_all_indiv / owlNamedIndividualHashSet.size();
        monitor.displayMessage("\nSimilarity score of all IFPs with respect to group 2 : " + accuracy_avg_for_all_indiv, true);

        logger.info("Finding similarity finished");
        monitor.displayMessage("Finding similarity finished. ", true);
    }
}
