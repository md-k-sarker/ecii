package org.dase.test;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;

public class TestCloning {
    public ArrayList<Integer> aList;
    public String str;
    OWLClassExpression owlClassExpression;

    public TestCloning(TestCloning another) {
        this.aList = new ArrayList<>();
        this.aList.addAll(another.aList);
        //this.aList = another.aList;
        this.str = another.str;
        this.owlClassExpression = another.owlClassExpression;
    }

    public TestCloning() {
        aList = new ArrayList<>();

    }
}
