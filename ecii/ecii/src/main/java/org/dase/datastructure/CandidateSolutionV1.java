package org.dase.datastructure;
/*
Written by sarker.
Written at 8/20/18.
*/


import org.dase.core.Score;
import org.dase.core.SharedDataHolder;
import org.dase.util.Utility;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.semanticweb.owlapi.dlsyntax.renderer.DLSyntax.*;

/**
 * CandidateSolution consists of atomic classes and candidate classes.
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
public class CandidateSolutionV1 {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    private ArrayList<CandidateClassV1> candidateClasses;

    /**
     * Candidate classes as group.
     */
    private HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> groupedCandidateClasses;

    /**
     * candidate solution
     */
    private OWLClassExpression candidateSolutionAsOWLClass = null;

    /**
     * candidate solution as String
     */
    private String candidateSolutionAsString= null;

    private boolean solutionChanged = false;

    // Score associated with this solution
    private Score score;

    public CandidateSolutionV1() {
        solutionChanged = true;
        this.candidateClasses = new ArrayList<>();
    }

    public CandidateSolutionV1(CandidateSolutionV1 anotherCandidateSolution) {
        solutionChanged = true;
        this.candidateClasses = new ArrayList<>(anotherCandidateSolution.candidateClasses);
    }


    public ArrayList<CandidateClassV1> getCandidateClasses() {
        return candidateClasses;
    }

    public void setCandidateClasses(ArrayList<CandidateClassV1> candidateClasses) {
        solutionChanged = true;
        this.candidateClasses = candidateClasses;
    }

    public void addCandidateClass(CandidateClassV1 candidateClass) {
        solutionChanged = true;
        this.candidateClasses.add(candidateClass);
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
    public HashMap<OWLObjectProperty, ArrayList<CandidateClassV1>> getGroupedCandidateClasses() {
        if (null == groupedCandidateClasses) {
            createGroup();
        }
        return groupedCandidateClasses;
    }

    /**
     * @return
     */
    public OWLClassExpression getSolutionAsOWLClassExpression() {

        if(!solutionChanged && null != candidateSolutionAsOWLClass)
            return candidateSolutionAsOWLClass;

        solutionChanged = false;
        if (null == groupedCandidateClasses) {
            createGroup();
        }

        // bare portion
        OWLClassExpression directTypePortion = null;
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV1> candidateClasses = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);

            if (null != candidateClasses) {
                if (candidateClasses.size() > 0) {
                    HashSet<OWLClassExpression> directCandidateClassesAsOWLClassExpression = new HashSet<>();
                    for (CandidateClassV1 candidateClass : candidateClasses) {
                        directCandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                    }
                    // convert to list to get the single item, so that we don't need to make union
                    ArrayList<OWLClassExpression> directCandidateClassesAsOWLClassExpressionAList = new ArrayList<>(directCandidateClassesAsOWLClassExpression);
                    if (directCandidateClassesAsOWLClassExpressionAList.size() > 0) {
                        if (directCandidateClassesAsOWLClassExpressionAList.size() == 1) {
                            directTypePortion = directCandidateClassesAsOWLClassExpressionAList.get(0);
                        } else {
                            // make OR between candidate classes for direct Type.
                            // TODO(zaman): this is conflicting, need to verify, why we are using union for multiple bare types.
                            // the thing is we should not have multiple candidate class for bare types.
                            logger.info("This code should not be executed, need to check CandidateSolutionV1.getSolutionAsOWLClassExpression() ");
                            directTypePortion = SharedDataHolder.owlDataFactory.getOWLObjectUnionOf(directCandidateClassesAsOWLClassExpression);
                        }
                    }
                }
            }
        }

        // rFilled portion
        OWLClassExpression rFilledPortion = null;
        HashSet<OWLClassExpression> rFilledPortionForAllGroups = new HashSet<>();
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV1> candidateClasses = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClasses) {
                    if (candidateClasses.size() > 0) {

                        OWLClassExpression rFilledPortionForThisGroup = null;
                        HashSet<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpression = new HashSet<>();

                        for (CandidateClassV1 candidateClass : candidateClasses) {
                            rFilledcandidateClassesAsOWLClassExpression.add(candidateClass.getCandidateClassAsOWLClassExpression());
                        }

                        // convert to list to get the single item, so that we don't need to make union
                        ArrayList<OWLClassExpression> rFilledcandidateClassesAsOWLClassExpressionAList = new ArrayList<>(rFilledcandidateClassesAsOWLClassExpression);
                        if (rFilledcandidateClassesAsOWLClassExpressionAList.size() > 0) {
                            if (rFilledcandidateClassesAsOWLClassExpressionAList.size() == 1) {
                                rFilledPortionForThisGroup = rFilledcandidateClassesAsOWLClassExpressionAList.get(0);
                            } else {
                                // make OR between candidate classes
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
                // bug-fix : it should be owlObjectIntersectionOf instead of owlObjectUnionOf
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

        this.candidateSolutionAsOWLClass = complexClassExpression;
        return complexClassExpression;
    }

    /**
     * Get the solution as String
     *
     * @return
     */
    public String getSolutionAsString() {

        if(!solutionChanged && null != candidateSolutionAsString)
            return candidateSolutionAsString;

        solutionChanged = false;

        if (null == groupedCandidateClasses) {
            createGroup();
        }

        StringBuilder sb = new StringBuilder();
        int bareTypeSize = 0;


        // print bare type at first
        if (groupedCandidateClasses.containsKey(SharedDataHolder.noneOWLObjProp)) {
            ArrayList<CandidateClassV1> candidateClasses = groupedCandidateClasses.get(SharedDataHolder.noneOWLObjProp);
            if (null != candidateClasses) {
                if (candidateClasses.size() > 0) {
                    // we expect atomic class size will be one but it is not the case always.
                    bareTypeSize = candidateClasses.size();
                    if (candidateClasses.size() == 1) {
                        sb.append(getCandidateClassAsString(candidateClasses.get(0)));
                    } else {
                        sb.append("(");
                        sb.append(getCandidateClassAsString(candidateClasses.get(0)));

                        for (int i = 1; i < candidateClasses.size(); i++) {
                            // ecii extension-- making it AND instead of OR
                            sb.append(" " + AND.toString() + " ");
                            sb.append(getCandidateClassAsString(candidateClasses.get(i)));
                        }
                        sb.append(")");
                    }
                }
            }
        }

        int rFilledSize = 0;
        // print r filled type then
        for (Map.Entry<OWLObjectProperty, ArrayList<CandidateClassV1>> entry : groupedCandidateClasses.entrySet()) {

            // each group will be concatenated by AND.
            OWLObjectProperty owlObjectProperty = entry.getKey();
            ArrayList<CandidateClassV1> candidateClasses = entry.getValue();

            if (null != owlObjectProperty && !owlObjectProperty.equals(SharedDataHolder.noneOWLObjProp)) {
                if (null != candidateClasses) {
                    if (candidateClasses.size() > 0) {
                        rFilledSize++;

                        if (bareTypeSize > 0 || rFilledSize > 1) {
                            sb.append(" " + AND.toString() + " ");
                        }
                        sb.append(EXISTS + Utility.getShortName(owlObjectProperty) + ".");
                        if (candidateClasses.size() == 1) {
                            sb.append(getCandidateClassAsString(candidateClasses.get(0)));
                        } else {
                            sb.append("(");
                            sb.append(getCandidateClassAsString(candidateClasses.get(0)));

                            for (int i = 1; i < candidateClasses.size(); i++) {
                                sb.append(" " + OR.toString() + " ");
                                sb.append(getCandidateClassAsString(candidateClasses.get(i)));
                            }
                            sb.append(")");
                        }
                    }
                }
            }
        }

        this.candidateSolutionAsString = sb.toString();
        return this.candidateSolutionAsString;
    }

    /**
     * Print candidate class as String
     *
     * @return
     */
    private String getCandidateClassAsString(CandidateClassV1 candidateClass) {

        StringBuilder sb = new StringBuilder();

        if (null != candidateClass) {

            if (candidateClass.getConjunctiveHornClauses().size() > 0) {
                if (candidateClass.getConjunctiveHornClauses().size() == 1) {
                    sb.append("(");
                    sb.append(getHornClauseAsString(candidateClass.getConjunctiveHornClauses().get(0)));
                    sb.append(")");
                } else {

                    sb.append("(");

                    sb.append("(");
                    sb.append(getHornClauseAsString(candidateClass.getConjunctiveHornClauses().get(0)));
                    sb.append(")");

                    for (int i = 1; i < candidateClass.getConjunctiveHornClauses().size(); i++) {
                        // should we use OR or AND between multiple hornClauses of same object property ?
                        // This is especially important when we have only bare type, i.e. no R-Filled type.
                        // If changed here changes must reflect in accuracy measure too.
                        // TODO:  check with Pascal.
                        sb.append(" " + OR.toString() + " ");
                        sb.append("(");
                        sb.append(getHornClauseAsString(candidateClass.getConjunctiveHornClauses().get(i)));
                        sb.append(")");
                    }

                    sb.append(")");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Print ConjunctiveHornClause  as String
     * TODO(Zaman) : need to modify the method to cope v1
     * @return
     */
    private String getHornClauseAsString(ConjunctiveHornClauseV1 conjunctiveHornClause) {
        StringBuilder sb = new StringBuilder();

        boolean hasPositive = false;

        if (null != conjunctiveHornClause) {

            // print postypes
            if(null != conjunctiveHornClause.getPosObjectTypes()){
                if(conjunctiveHornClause.getPosObjectTypes().size() > 0){
                    hasPositive = true;
                    if(conjunctiveHornClause.getPosObjectTypes().size() == 1){
                        sb.append(Utility.getShortName((OWLClass) conjunctiveHornClause.getPosObjectTypes().get(0)));
                    }else{
                        // not using parenthesis for multiple positive types.
                        sb.append(Utility.getShortName((OWLClass) conjunctiveHornClause.getPosObjectTypes().get(0)));
                        for(int i=1;i<conjunctiveHornClause.getPosObjectTypes().size(); i++){
                            sb.append(" "+ AND.toString());
                            sb.append(Utility.getShortName((OWLClass) conjunctiveHornClause.getPosObjectTypes().get(i)));
                        }
                    }
                }
            }

            // print negtypes
            if (null != conjunctiveHornClause.getNegObjectTypes()) {
                if (conjunctiveHornClause.getNegObjectTypes().size() > 0) {
                    if (hasPositive) {
                        sb.append(" " + AND.toString());
                    }
                    sb.append(" " + NOT.toString());
                    if (conjunctiveHornClause.getNegObjectTypes().size() == 1) {
                        sb.append(" " + Utility.getShortName((OWLClass) conjunctiveHornClause.getNegObjectTypes().get(0)));
                    } else {
                        sb.append(" (");
                        sb.append(Utility.getShortName((OWLClass) conjunctiveHornClause.getNegObjectTypes().get(0)));
                        for (int i = 1; i < conjunctiveHornClause.getNegObjectTypes().size(); i++) {
                            sb.append(" " + OR.toString());
                            sb.append(" " + Utility.getShortName((OWLClass) conjunctiveHornClause.getNegObjectTypes().get(i)));
                        }
                        sb.append(")");
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

        candidateClasses.forEach(candidateClass -> {
            if (groupedCandidateClasses.containsKey(candidateClass.getOwlObjectProperty())) {
                groupedCandidateClasses.get(candidateClass.getOwlObjectProperty()).add(candidateClass);
            } else {
                ArrayList<CandidateClassV1> candidateClassArrayList = new ArrayList<>();
                candidateClassArrayList.add(candidateClass);
                groupedCandidateClasses.put(candidateClass.getOwlObjectProperty(), candidateClassArrayList);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateSolutionV1 that = (CandidateSolutionV1) o;
        return Objects.equals(new HashSet<>(candidateClasses), new HashSet<>(that.candidateClasses));
    }

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(candidateClasses));
    }
}