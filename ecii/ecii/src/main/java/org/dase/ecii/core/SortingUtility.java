package org.dase.ecii.core;
/*
Written by sarker.
Written at 7/4/20.
*/

import org.dase.ecii.datastructure.*;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SortingUtility {
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Select top k5 hornCluases from the hornClausesMap.
     * It' does the sorting and filtering in memory.
     * Specifically it converts hashmap to arraylist and sort and filter it,
     * clear the original hashmap and
     * insert the filtered arraylist items into the original hashmap.
     *
     * @param hornClausesMap : HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>>
     * @param limit
     * @return
     */
    public static void sortAndFilterHornClauseV1V2Map(
            HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> hornClausesMap,
            int limit) {

        //make a list
        List<ConjunctiveHornClauseV1V2> conjunctiveHornClausesList = new ArrayList<>();
        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();


        for (Map.Entry<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>> entry : hornClausesMap.entrySet()) {
            OWLObjectProperty owlObjectProperty = entry.getKey();
            HashSet<ConjunctiveHornClauseV1V2> conjunctiveHornClauses = entry.getValue();
            conjunctiveHornClausesList.addAll(conjunctiveHornClauses);
            logger.debug("conjunctiveHornClauses size:  " + conjunctiveHornClauses.size());
        }

        if (conjunctiveHornClausesList.size() > 0) {

            logger.info("horn clauses map  will be filtered, initial size: " + conjunctiveHornClausesList.size());
            conjunctiveHornClausesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    if (null != o1.getPosObjectTypes())
                        o1Length.addAndGet(o1.getPosObjectTypes().size());
                    if (null != o1.getNegObjectTypes()) {
                        o1Length.addAndGet(o1.getNegObjectTypes().size());
                    }
                    if (null != o2.getPosObjectTypes())
                        o2Length.addAndGet(o2.getPosObjectTypes().size());
                    if (null != o2.getNegObjectTypes()) {
                        o2Length.addAndGet(o2.getNegObjectTypes().size());
                    }
                    if (o1Length.get() - o2Length.get() > 0) {
                        return 1;
                    }
                    if (o1Length.get() == o2Length.get()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // todo(zaman): there exist a possibility of error in coverage score of a conjunctivehornclause.
            //  for china vs syria experiment developedAsia(China) shows coverage score of 0.5, but it must be 1.0 -- fixed
            // need to test the sorting
            logger.info("Score of first hornClause:  " + conjunctiveHornClausesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last hornClause:  " + conjunctiveHornClausesList.get(conjunctiveHornClausesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (conjunctiveHornClausesList.size() > limit + 1) {
                conjunctiveHornClausesList = conjunctiveHornClausesList.subList(0, limit + 1);
            }

            // make group again.
            hornClausesMap.clear();
            conjunctiveHornClausesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(hornClausesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure conjunctivehornclausemap size is upto limit.
            if (conjunctiveHornClausesList.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + conjunctiveHornClausesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!, Program exiting!!");
                System.exit(-1);
            }
        } else {
            logger.info("No filtering done. hornClause map empty.");
        }
    }

    /**
     * Select top k5 hornCluases from the hornClausesMap.
     * It' does the sorting and filtering in memory.
     * Specifically it converts hashmap to arraylist and sort and filter it,
     * clear the original hashmap and
     * insert the filtered arraylist items into the original hashmap.
     *
     * @param hornClausesMap : HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV1V2>>
     * @param limit
     * @return
     */
    public static void sortAndFilterHornClauseV0Map(
            HashMap<OWLObjectProperty, HashSet<ConjunctiveHornClauseV0>> hornClausesMap,
            int limit) {

        //make a list
        List<ConjunctiveHornClauseV0> conjunctiveHornClausesList = new ArrayList<>();
        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();


        for (Map.Entry<OWLObjectProperty, HashSet<ConjunctiveHornClauseV0>> entry : hornClausesMap.entrySet()) {
            OWLObjectProperty owlObjectProperty = entry.getKey();
            HashSet<ConjunctiveHornClauseV0> conjunctiveHornClauses = entry.getValue();
            conjunctiveHornClausesList.addAll(conjunctiveHornClauses);
            logger.debug("conjunctiveHornClauses size:  " + conjunctiveHornClauses.size());
        }

        if (conjunctiveHornClausesList.size() > 0) {

            // sort the list
            // todo(zaman): need to use the unified sorting algorithm, instead of this local one
            // unifying
            logger.info("horn clauses map  will be filtered, initial size: " + conjunctiveHornClausesList.size());
            conjunctiveHornClausesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    if (null != o1.getPosObjectType())
                        o1Length.addAndGet(1);
                    if (null != o1.getNegObjectTypes()) {
                        o1Length.addAndGet(o1.getNegObjectTypes().size());
                    }
                    if (null != o2.getPosObjectType())
                        o2Length.addAndGet(1);
                    if (null != o2.getNegObjectTypes()) {
                        o2Length.addAndGet(o2.getNegObjectTypes().size());
                    }
                    if (o1Length.get() - o2Length.get() > 0) {
                        return 1;
                    }
                    if (o1Length.get() == o2Length.get()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // todo(zaman): there is significant error in coverage score of a conjunctivehornclause. for china vs syria experiment developedAsia(China) shows coverage score of 0.5, but it must be 1.0 -- fixed
            // test sorting
            logger.info("Score of first hornClause:  " + conjunctiveHornClausesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last hornClause:  " + conjunctiveHornClausesList.get(conjunctiveHornClausesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (conjunctiveHornClausesList.size() > limit + 1) {
                conjunctiveHornClausesList = conjunctiveHornClausesList.subList(0, limit + 1);
            }

            // make group again.
            hornClausesMap.clear();
            conjunctiveHornClausesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(hornClausesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure conjunctivehornclausemap size is upto limit.
            if (conjunctiveHornClausesList.size() <= limit + 1) {
                logger.info("horn clauses map filtered and now size: " + conjunctiveHornClausesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!, Program exiting!!");
                System.exit(-1);
            }
        } else {
            logger.info("No filtering done. hornClause map empty.");
        }

    }

    /**
     * Select top k6 CandidateClassV2 from the candidateClassMap.
     * It does the sorting and filtering in memory.
     * Specifically it converts hashmap to arraylist and sort and filter it,
     * clear the original hashmap and
     * insert the filtered arraylist items into the original hashmap.
     *
     * @param limit
     * @return
     */
    public static void sortAndFilterCandidateClassV2Map(HashMap<OWLObjectProperty, HashSet<CandidateClassV2>> candidateClassesMap, int limit) {
        // make a list
        List<CandidateClassV2> candidateClassesList = new ArrayList<>();
        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        for (Map.Entry<OWLObjectProperty, HashSet<CandidateClassV2>> entry : candidateClassesMap.entrySet()) {
            OWLObjectProperty owlObjectProperty = entry.getKey();
            HashSet<CandidateClassV2> candidateClasses = entry.getValue();
            candidateClassesList.addAll(candidateClasses);
        }

        if (candidateClassesList.size() > 0) {
            // sort the list
            logger.info("candidate classes map  will be filtered. initial size: " + candidateClassesList.size());
            candidateClassesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    o1.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o1Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    o2.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o2Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    if (o1Length.get() - o2Length.get() > 0) {
                        return 1;
                    }
                    if (o1Length.get() == o2Length.get()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // test sorting
            logger.info("Score of first candidate class:  " + candidateClassesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last candidate class:  " + candidateClassesList.get(candidateClassesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (candidateClassesList.size() > limit + 1) {
                candidateClassesList = candidateClassesList.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesList.size() <= limit + 1) {
                logger.info("candidate classes map filtered and now size: " + candidateClassesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!, Program exiting!!");
                System.exit(-1);
            }
        } else {
            logger.info("No filtering done. candidateClasses map empty");
        }
    }

    /**
     * Select top k6 CandidateClassV1 from the candidateClassMap.
     * It does the sorting and filtering in memory.
     * Specifically it converts hashmap to arraylist and sort and filter it,
     * clear the original hashmap and
     * insert the filtered arraylist items into the original hashmap.
     *
     * @param limit
     * @return
     */
    public static void sortAndFilterCandidateClassV1Map(HashMap<OWLObjectProperty, HashSet<CandidateClassV1>> candidateClassesMap, int limit) {
        // make a list
        List<CandidateClassV1> candidateClassesList = new ArrayList<>();
        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        for (Map.Entry<OWLObjectProperty, HashSet<CandidateClassV1>> entry : candidateClassesMap.entrySet()) {
            OWLObjectProperty owlObjectProperty = entry.getKey();
            HashSet<CandidateClassV1> candidateClasses = entry.getValue();
            candidateClassesList.addAll(candidateClasses);
        }

        if (candidateClassesList.size() > 0) {
            // sort the list
            logger.info("candidate classes map  will be filtered. initial size: " + candidateClassesList.size());
            candidateClassesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    o1.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o1Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    o2.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectTypes())
                            o2Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    if (o1Length.get() - o2Length.get() > 0) {
                        return 1;
                    }
                    if (o1Length.get() == o2Length.get()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // test sorting
            logger.info("Score of first candidate class:  " + candidateClassesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last candidate class:  " + candidateClassesList.get(candidateClassesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (candidateClassesList.size() > limit + 1) {
                candidateClassesList = candidateClassesList.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesList.size() <= limit + 1) {
                logger.info("candidate classes map filtered and now size: " + candidateClassesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!, Program exiting!!");
                System.exit(-1);
            }
        } else {
            logger.info("No filtering done. candidateClasses map empty");
        }
    }

    /**
     * Select top k6 CandidateClassV0 from the candidateClassMap.
     * It does the sorting and filtering in memory.
     * Specifically it converts hashmap to arraylist and sort and filter it,
     * clear the original hashmap and
     * insert the filtered arraylist items into the original hashmap.
     *
     * @param limit
     * @return
     */
    public static void sortAndFilterCandidateClassV0Map(HashMap<OWLObjectProperty, HashSet<CandidateClassV0>> candidateClassesMap, int limit) {
        // make a list
        List<CandidateClassV0> candidateClassesList = new ArrayList<>();
        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        for (Map.Entry<OWLObjectProperty, HashSet<CandidateClassV0>> entry : candidateClassesMap.entrySet()) {
            OWLObjectProperty owlObjectProperty = entry.getKey();
            HashSet<CandidateClassV0> candidateClasses = entry.getValue();
            candidateClassesList.addAll(candidateClasses);
        }

        if (candidateClassesList.size() > 0) {
            // sort the list
            logger.info("candidate classes map  will be filtered. initial size: " + candidateClassesList.size());
            candidateClassesList.sort((o1, o2) -> {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                } else if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    o1.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectType())
                            o1Length.addAndGet(1);
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    o2.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                        if (null != conjunctiveHornClause.getPosObjectType())
                            o2Length.addAndGet(1);
                        if (null != conjunctiveHornClause.getNegObjectTypes())
                            o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                    });

                    if (o1Length.get() - o2Length.get() > 0) {
                        return 1;
                    }
                    if (o1Length.get() == o2Length.get()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            });

            // test sorting
            logger.info("Score of first candidate class:  " + candidateClassesList.get(0).getScore().getDefaultScoreValue());
            logger.info("Score of last candidate class:  " + candidateClassesList.get(candidateClassesList.size() - 1).getScore().getDefaultScoreValue());

            // filter/select top n (upto limit)
            if (candidateClassesList.size() > limit + 1) {
                candidateClassesList = candidateClassesList.subList(0, limit + 1);
            }

            // make group again.
            candidateClassesMap.clear();
            candidateClassesList.forEach(conjunctiveHornClause -> {
                HashMapUtility.insertIntoHashMap(candidateClassesMap, conjunctiveHornClause.getOwlObjectProperty(), conjunctiveHornClause);
            });

            // make sure cconjunctivehornclausemap size is upto limit.
            if (candidateClassesList.size() <= limit + 1) {
                logger.info("candidate classes map filtered and now size: " + candidateClassesList.size());
            } else {
                logger.error("!!!!!!!!!!!!!horn clause map didn't filter perfectly. !!!!!!!!!!!!!, Program exiting!!");
                System.exit(-1);
            }
        } else {
            logger.info("No filtering done. candidateClasses map empty");
        }
    }

    /**
     * Sort the solutions,
     *
     * @param ascendingOfStringLength
     * @return ArrayList<CandidateSolutionV2>
     */
    public static ArrayList<CandidateSolutionV2> sortSolutionsV2Custom(HashSet<CandidateSolutionV2> solutionSet, boolean ascendingOfStringLength) {

        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        ArrayList<CandidateSolutionV2> solutionList = new ArrayList<>(solutionSet);

        solutionList.sort(new Comparator<CandidateSolutionV2>() {
            @Override
            public int compare(CandidateSolutionV2 o1, CandidateSolutionV2 o2) {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                }
                if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    //o1Length += o1.getAtomicPosOwlClasses().size();
                    o1.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o1Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    //o2Length += o2.getAtomicPosOwlClasses().size();
                    o2.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o2Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    // small to large of solution length
                    if (ascendingOfStringLength) {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return 1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return -1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                } else {
                    return 1;
                }
            }
        });

        return solutionList;
    }


    /**
     * Sort the solutions,
     *
     * @param ascendingOfStringLength
     * @return ArrayList<CandidateSolutionV1>
     */
    public static ArrayList<CandidateSolutionV1> sortSolutionsV1Custom(HashSet<CandidateSolutionV1> solutionSet, boolean ascendingOfStringLength) {

        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        ArrayList<CandidateSolutionV1> solutionList = new ArrayList<>(solutionSet);

        solutionList.sort(new Comparator<CandidateSolutionV1>() {
            @Override
            public int compare(CandidateSolutionV1 o1, CandidateSolutionV1 o2) {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                }
                if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    //o1Length += o1.getAtomicPosOwlClasses().size();
                    o1.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o1Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    //o2Length += o2.getAtomicPosOwlClasses().size();
                    o2.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectTypes())
                                o2Length.addAndGet(conjunctiveHornClause.getPosObjectTypes().size());
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    // small to large of solution length
                    if (ascendingOfStringLength) {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return 1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return -1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                } else {
                    return 1;
                }
            }
        });

        return solutionList;
    }


    /**
     * Sort the solutions,
     *
     * @param ascendingOfStringLength
     * @return ArrayList<CandidateSolutionV0>
     */
    public static ArrayList<CandidateSolutionV0> sortSolutionsV0Custom(HashSet<CandidateSolutionV0> solutionSet, boolean ascendingOfStringLength) {

        AtomicInteger o1Length = new AtomicInteger();
        AtomicInteger o2Length = new AtomicInteger();

        ArrayList<CandidateSolutionV0> solutionList = new ArrayList<>(solutionSet);

        solutionList.sort(new Comparator<CandidateSolutionV0>() {
            @Override
            public int compare(CandidateSolutionV0 o1, CandidateSolutionV0 o2) {
                if (o1.getScore().getDefaultScoreValue() - o2.getScore().getDefaultScoreValue() > 0) {
                    return -1;
                }
                if (o1.getScore().getDefaultScoreValue() == o2.getScore().getDefaultScoreValue()) {
                    // compare length
                    o1Length.set(0);
                    o2Length.set(0);

                    //o1Length += o1.getAtomicPosOwlClasses().size();
                    o1.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectType())
                                o1Length.addAndGet(1);
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o1Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    //o2Length += o2.getAtomicPosOwlClasses().size();
                    o2.getCandidateClasses().forEach(candidateClass -> {
                        candidateClass.getConjunctiveHornClauses().forEach(conjunctiveHornClause -> {
                            if (null != conjunctiveHornClause.getPosObjectType())
                                o2Length.addAndGet(1);
                            if (null != conjunctiveHornClause.getNegObjectTypes())
                                o2Length.addAndGet(conjunctiveHornClause.getNegObjectTypes().size());
                        });
                    });
                    // small to large of solution length
                    if (ascendingOfStringLength) {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return 1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else {
                        if (o1Length.get() - o2Length.get() > 0) {
                            return -1;
                        }
                        if (o1Length.get() == o2Length.get()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                } else {
                    return 1;
                }
            }
        });


        return solutionList;
    }


}
