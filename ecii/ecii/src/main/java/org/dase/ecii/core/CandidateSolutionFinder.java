package org.dase.ecii.core;
/*
Written by sarker.
Written at 6/29/20.
*/

import org.dase.ecii.datastructure.CandidateSolution;
import org.dase.ecii.datastructure.CandidateSolutionV0;
import org.dase.ecii.datastructure.CandidateSolutionV1;
import org.dase.ecii.datastructure.CandidateSolutionV2;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Monitor;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Super class of many different versions of CandidateSolutionFinder
 */
public class CandidateSolutionFinder implements ICandidateSolutionFinder {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public OWLOntology ontology;
    public OWLDataFactory owlDataFactory;
    public OWLOntologyManager owlOntologyManager;
    public OWLReasoner reasoner;
    public PrintStream out;
    public Monitor monitor;
    protected transient volatile int o1Length = 0;
    protected transient volatile int o2Length = 0;
    transient volatile protected int nrOfTotalIndividuals;
    transient volatile protected int nrOfPositiveIndividuals;
    transient volatile protected int nrOfNegativeIndividuals;


    /**
     * public constructor
     */
    public CandidateSolutionFinder(OWLReasoner _reasoner, OWLOntology _ontology, PrintStream _printStream, Monitor _monitor) {
        this.reasoner = _reasoner;
        this.ontology = _ontology;
        this.owlOntologyManager = this.ontology.getOWLOntologyManager();
        this.owlDataFactory = this.owlOntologyManager.getOWLDataFactory();
        this.out = _printStream;
        this.monitor = _monitor;
    }

    /**
     * Remove common types which appeared both in positive types and negative types.
     * Specifically, if removeCommonTypesFromOneSideOnly== true
     * remove from either pos-set or neg-set which have less individuals
     * not removing from both
     * else
     * remove from both side
     *
     * @param owlObjectProperty
     */
    public void removeCommonTypesFromPosAndNeg(OWLObjectProperty owlObjectProperty) {

        logger.info("Before removing types (types which appeared in both pos and neg images): ");
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            logger.info("pos types: ");
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            logger.info("neg types: ");
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }

        logger.debug("Removing types which appeared both in pos and neg: ");
        HashSet<OWLClassExpression> removeFromPosType = new HashSet<>();
        HashSet<OWLClassExpression> removeFromNegType = new HashSet<>();
        HashSet<OWLClassExpression> removeFromBothType = new HashSet<>();

        //HashSet<OWLClassExpression> typesBothInPosAndNeg = new HashSet<>();
        // remove those posTypes which also appeared in negTypes using some kind of accuracy/tolerance measure.
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            for (OWLClassExpression owlClassExpr : SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet()) {
                // use tolerance measure
                // need to exclude types which appear in neg images
                if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
                    if (SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).containsKey(owlClassExpr)) {

                        // remove from that which have less individuals
                        double posRatio = SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).
                                get(owlClassExpr) / SharedDataHolder.posIndivs.size();
                        double negRatio = SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).
                                get(owlClassExpr) / SharedDataHolder.negIndivs.size();

                        if (posRatio >= negRatio) {
                            // remove from negative
                            removeFromNegType.add(owlClassExpr);
                            logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpr) + " will be removed from negativeTypes");
                        } else {
                            // remove from positive
                            removeFromPosType.add(owlClassExpr);
                            logger.debug("\t" + Utility.getShortName((OWLClass) owlClassExpr) + " will be removed from positiveTypes");
                        }
                    }
                }
            }
        }

        if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
            removeFromBothType.addAll(removeFromPosType);
            removeFromBothType.addAll(removeFromNegType);
        }

        // remove those and owl:Thing and owl:Nothing
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
                SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().removeAll(removeFromBothType);
            } else
                SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().removeAll(removeFromPosType);
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLThing());
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLNothing());
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            if (!ConfigParams.removeCommonTypesFromOneSideOnly) {
                SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().removeAll(removeFromBothType);
            } else
                SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().removeAll(removeFromNegType);
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLThing());
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).remove(owlDataFactory.getOWLNothing());
        }

        logger.info("After removing types (types which appeared in both pos and neg images): ");
        if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlObjectProperty)) {
            logger.info("pos types: ");
            SharedDataHolder.typeOfObjectsInPosIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
        if (SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlObjectProperty)) {
            logger.info("neg types: ");
            SharedDataHolder.typeOfObjectsInNegIndivs.get(owlObjectProperty).keySet().forEach(type -> {
                logger.info("\t" + Utility.getShortName((OWLClass) type));
            });
        }
    }

    /**
     * Init the variables
     */
    public void initVariables() {

        nrOfPositiveIndividuals = SharedDataHolder.posIndivs.size();
        nrOfNegativeIndividuals = SharedDataHolder.negIndivs.size();
        nrOfTotalIndividuals = nrOfPositiveIndividuals + nrOfNegativeIndividuals;
    }
}
