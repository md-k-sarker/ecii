package org.dase.ecii.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/


import org.dase.ecii.core.Score;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.Utility;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.*;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.*;

/**
 * CandidateSolutionV0 consists of atomic classes and candidate classes.
 * Atomic Class and candidate class will be concatenated by AND/Conjunction.
 * Atomic Class must be printed first.
 * Some ontology dont have any object property, so they dont have any candidateClass.
 *
 * <pre>
 *   Candidate solution is of the form:
 *
 *   l
 * A ⊓ 􏰃∃Ri.Ci,
 *   i=1
 *
 *    which can  also be written as:
 *
 *   k3       k2
 * A ⊓ 􏰃∃Ri. 􏰀(⊔(Bji ⊓¬(D1 ⊔...⊔Dji)))
 *   i=1    j=1
 *
 *   here,
 *
 *   k3 = limit of object properties considered. = ConfigParams.objPropsCombinationLimit
 *   k2 = limit of horn clauses. = ConfigParams.hornClauseLimit.
 * </pre>
 * For simplicity, we are using a single direct type to form s single solution now.
 * <p>
 * * Implementation note:
 * * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
 */
public class CandidateSolutionV0 {

    /**
     * This is direct type/bare type, i.e. without object property.
     * This is usually an array, but for simplicity use it as a single atomic class. Like A or B or C.
     */
    //private ArrayList<OWLClassExpression> atomicPosOwlClasses;

    /**
     * This is direct type/bare type, i.e. without object property.
     * This is usually an array, but for simplicity use it as a single atomic class. Like A or B or C.
     */

    //private ArrayList<OWLClassExpression> atomicNegOwlClasses;
    /**
     * ArrayList of Candidate Class.
     * Limit K3 = limit of object properties considered = ConfigParams.objPropsCombinationLimit.
     * <p>
     * Implementation note:
     * Atomic class is also added using candidate class. If the object property is empty = SharedDataHolder.noneOWLObjProp then it is atomic class.
     */
    private ArrayList<CandidateClassV0> candidateClassV0s;

    /**
     * Candidate classes as group.
     */
    private HashMap<OWLObjectProperty, ArrayList<CandidateClassV0>> groupedCandidateClasses;

    // Score associated with this solution
    private Score score;

    public CandidateSolutionV0() {
        this.candidateClassV0s = new ArrayList<>();
    }

    public CandidateSolutionV0(CandidateSolutionV0 anotherCandidateSolutionV0) {
        this.candidateClassV0s = new ArrayList<>(anotherCandidateSolutionV0.candidateClassV0s);
    }


    public ArrayList<CandidateClassV0> getCandidateClassV0s() {
        return candidateClassV0s;
    }

    public void setCandidateClassV0s(ArrayList<CandidateClassV0> candidateClassV0s) {
        this.candidateClassV0s = candidateClassV0s;
    }

    public void addCandidateClass(CandidateClassV0 candidateClassV0) {
        this.candidateClassV0s.add(candidateClassV0);
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


    /**
     * Getter. It dont't have any public setter.
     *
     * @return
     */
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV0>> getGroupedCandidateClasses() {
        if (null == groupedCandidateClasses) {
            createGroup();
        }
        return groupedCandidateClasses;
    }

    /**
     * @return
     */
    public OWLClassExpression getSolutionAsOWLClassExpression() {

        if (null == groupedCandidateClasses) {
            createGroup();
        }

        OWLClassExpression directTypePortion = null;
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV0> candidateClassV0s = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);

            if (null != candidateClassV0s) {
                if (candidateClassV0s.size() > 0) {
                    HashSet<OWLClassExpression> directCandidateClassesAsOWLClassExpression = new HashSet<>();
                    for (CandidateClassV0 candidateClassV0 : candidateClassV0s) {
                        directCandidateClassesAsOWLClassExpression.add(candidateClassV0.getCandidateClassAsOWLClassExpression());
                    }
                    // convert to list to get the single item, so that we don't need to make union
                    ArrayList<OWLClassExpression> directCandidateClassesAsOWLClassExpressionAList = new ArrayList<>(directCandidateClassesAsOWLClassExpression);
                    if (directCandidateClassesAsOWLClassExpressionAList.size() > 0) {
                        if (directCandidateClassesAsOWLClassExpressionAList.size() == 1) {
                            directTypePortion = directCandidateClassesAsOWLClassExpressionAList.get(0);
                        } else {
                            // make OR between candidate classes for direct Type.
                            directTypePortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(directCandidateClassesAsOWLClassExpression);
                        }
                    }
                }
            }
        }

        OWLClassExpression rFilledPortion = null;
        HashSet<OWLClassExpression> rFilledPortionForAllGroups = new HashSet<>();
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV0> candidateClassV0s = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClassV0s) {
                    if (candidateClassV0s.size() > 0) {

                        OWLClassExpression rFilledPortionForThisGroup = null;
                        HashSet<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpression = new HashSet<>();

                        for (CandidateClassV0 candidateClassV0 : candidateClassV0s) {
                            rFilledcandidateClassesAsOWLClassExpression.add(candidateClassV0.getCandidateClassAsOWLClassExpression());
                        }

                        // convert to list to get the single item, so that we don't need to make union
                        ArrayList<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpressionAList = new ArrayList<>(rFilledcandidateClassesAsOWLClassExpression);
                        if (rFilledcandidateClassesAsOWLClassExpressionAList.size() > 0) {
                            if (rFilledcandidateClassesAsOWLClassExpressionAList.size() == 1) {
                                rFilledPortionForThisGroup = rFilledcandidateClassesAsOWLClassExpressionAList.get(0);
                            } else {
                                // make OR between candidate classes for direct Type.
                                rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(rFilledcandidateClassesAsOWLClassExpression);
                            }
                        }

                        // make OR between candidate classes for same object property.
                        //rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(rFilledcandidateClassesAsOWLClassExpression);

                        // use rFill
                        if (null != rFilledPortionForThisGroup) {
                            rFilledPortionForThisGroup = SharedDataHolder.owlDataFactory.getOWLObjectSomeValuesFrom(owlObjectProperty, rFilledPortionForThisGroup);
                            if (null != rFilledPortionForThisGroup) {

                                rFilledPortionForAllGroups.add(rFilledPortionForThisGroup);
                            }
                        }
                    }
                }
            }
        }

        // convert to list to get the single item, so that we don't need to make union
        ArrayList<OWLClassExpression> rFilledPortionForAllGroupsAList = new ArrayList<>(rFilledPortionForAllGroups);
        if (rFilledPortionForAllGroupsAList.size() > 0) {
            if (rFilledPortionForAllGroupsAList.size() == 1) {
                rFilledPortion = rFilledPortionForAllGroupsAList.get(0);
            } else {
                // make AND between multiple object Property.
                // bug-fix : it should be owlObjectIntersectionOf
                rFilledPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(rFilledPortionForAllGroups);
            }
        }

        // make AND between multiple object Property.
        //rFilledPortion = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(rFilledPortionForAllGroups);

        OWLClassExpression complexClassExpression = null;

        // make AND between direct type and rFilled type.
        if (null != directTypePortion && null != rFilledPortion) {
            complexClassExpression = SharedDataHolder.owlDataFactory.getOWLObjectIntersectionOf(directTypePortion, rFilledPortion);
        } else if (null != directTypePortion) {
            complexClassExpression = directTypePortion;
        } else if (null != rFilledPortion) {
            complexClassExpression = rFilledPortion;
        }

        return complexClassExpression;
    }

    /**
     * Get the solution as String
     *
     * @return
     */
    public String getSolutionAsString(boolean includePrefix) {
        if (null == groupedCandidateClasses) {
            createGroup();
        }

        StringBuilder sb = new StringBuilder();
        int bareTypeSize = 0;


        // print bare type at first
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV0> candidateClassV0s = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);
            if (null != candidateClassV0s) {
                if (candidateClassV0s.size() > 0) {
                    // we expect atomic class size will be one but it is not the case always.
                    bareTypeSize = candidateClassV0s.size();
                    if (candidateClassV0s.size() == 1) {
                        sb.append(candidateClassV0s.get(0).getCandidateClassAsString(includePrefix));
                    } else {
                        sb.append("(");
                        sb.append(candidateClassV0s.get(0).getCandidateClassAsString(includePrefix));

                        for (int i = 1; i < candidateClassV0s.size(); i++) {
                            sb.append(" " + OR.toString() + " ");
                            sb.append(candidateClassV0s.get(i).getCandidateClassAsString(includePrefix));
                        }
                        sb.append(")");
                    }
                }
            }
        }

        int rFilledSize = 0;
        // print r filled type then
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV0>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV0> candidateClassV0s = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClassV0s) {
                    if (candidateClassV0s.size() > 0) {
                        rFilledSize++;

                        if (bareTypeSize > 0 || rFilledSize > 1) {
                            sb.append(" " + AND.toString() + " ");
                        }
                        sb.append(EXISTS + Utility.getShortName(owlObjectProperty) + ".");
                        if (candidateClassV0s.size() == 1) {
                            sb.append(candidateClassV0s.get(0).
                                    getCandidateClassAsString(includePrefix));
                        } else {
                            sb.append("(");
                            sb.append(candidateClassV0s.get(0).
                                    getCandidateClassAsString(includePrefix));

                            for (int i = 1; i < candidateClassV0s.size(); i++) {
                                sb.append(" " + OR.toString() + " ");
                                sb.append(candidateClassV0s.get(i).
                                        getCandidateClassAsString(includePrefix));
                            }
                            sb.append(")");
                        }
                    }
                }
            }
        }

        return sb.toString();
    }


    /**
     * Create group
     */
    private void createGroup() {
        groupedCandidateClasses = new HashMap<>();

        candidateClassV0s.forEach(candidateClassV0 -> {
            if (groupedCandidateClasses.containsKey(candidateClassV0.getOwlObjectProperty())) {
                groupedCandidateClasses.get(candidateClassV0.getOwlObjectProperty()).add(candidateClassV0);
            } else {
                ArrayList<CandidateClassV0> candidateClassV0ArrayList = new ArrayList<>();
                candidateClassV0ArrayList.add(candidateClassV0);
                groupedCandidateClasses.put(candidateClassV0.getOwlObjectProperty(), candidateClassV0ArrayList);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateSolutionV0 that = (CandidateSolutionV0) o;
        return Objects.equals(new HashSet<>(candidateClassV0s), new HashSet<>(that.candidateClassV0s));
    }

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(candidateClassV0s));
    }
}
