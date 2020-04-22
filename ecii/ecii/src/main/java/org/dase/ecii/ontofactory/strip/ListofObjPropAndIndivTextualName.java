package org.dase.ecii.ontofactory.strip;
/*
Written by sarker.
Written at 4/21/20.
*/

import java.util.HashSet;

/**
 * class to facilitate binding
 */
public class ListofObjPropAndIndivTextualName {
    public HashSet<String> objPropNames;
    public HashSet<String> indivNames;

    public ListofObjPropAndIndivTextualName() {
        this.objPropNames = new HashSet<>();
        this.indivNames = new HashSet<>();
    }
}
