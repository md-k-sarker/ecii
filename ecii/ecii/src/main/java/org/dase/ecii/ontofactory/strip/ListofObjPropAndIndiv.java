package org.dase.ecii.ontofactory.strip;
/*
Written by sarker.
Written at 4/21/20.
*/

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.HashSet;

/**
 * class to facilitate binding
 */
public class ListofObjPropAndIndiv {
    public HashSet<OWLObjectProperty> objPropsofInterest;
    public HashSet<OWLNamedIndividual> directIndivs;
    public HashSet<OWLNamedIndividual> inDirectIndivs;

    public ListofObjPropAndIndiv() {
        this.objPropsofInterest = new HashSet<>();
        this.directIndivs = new HashSet<>();
        this.inDirectIndivs = new HashSet<>();
    }
}
