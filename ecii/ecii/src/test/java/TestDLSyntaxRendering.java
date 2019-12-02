package org.dase.test;
/*
Written by sarker.
Written at 5/18/18.
*/

import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.ontofactory.DLSyntaxRendererExt;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class TestDLSyntaxRendering {

    public static void testCloning() {
        TestCloning tc1 = new TestCloning();
        tc1.aList.add(1);
        tc1.aList.add(2);
        tc1.aList.add(3);
        tc1.aList.add(4);
        tc1.str = "hello";
        tc1.owlClassExpression = OWLManager.getOWLDataFactory().getOWLClass(IRI.create( "testClass1"));

        TestCloning tc2 = new TestCloning(tc1);
        //tc2 = tc1;
        tc2.aList.remove(0);
        tc2.aList.remove(0);
        tc2.str = "world";
        tc2.owlClassExpression = OWLManager.getOWLDataFactory().getOWLClass(IRI.create( "testClass2"));

        System.out.println("tc1");
        tc1.aList.forEach(i -> {
            System.out.print(i + " ");
        });
        System.out.println(tc1.str);
        System.out.println(tc1.owlClassExpression.toString());


        System.out.println("\ntc2");
        tc2.aList.forEach(i -> {
            System.out.print(i + " ");
        });
        System.out.println(tc2.str);
        System.out.println(tc2.owlClassExpression.toString());
    }

    public static void testDlSyntax() {
        DLSyntaxRendererExt dlRenderer = new DLSyntaxRendererExt();

       OWLDataFactory odf = OWLManager.getOWLDataFactory();
        
        OWLClass cls1 = odf.getOWLClass(IRI.create( "Class1"));

        OWLClass cls2 = odf.getOWLClass(IRI.create( "Class2"));

        OWLClass cls3 = odf.getOWLClass(IRI.create( "Class3"));

        // make disjunction of all posTypes.
        OWLClassExpression unionOfAllPosTypeObjects = odf.getOWLObjectUnionOf(cls1, cls2);
        // make disjunction of all negTypes.
        OWLClassExpression unionOfAllNegTypeObjects = odf.getOWLObjectUnionOf(cls3);
        // make complementOf the disjuncted negTypes.
        OWLClassExpression negateduUionOfAllNegTypeObjects = odf.getOWLObjectComplementOf(unionOfAllNegTypeObjects);

        // make conjunction of disjunctedPos and negatedDisjunctedNeg
        OWLClassExpression conjunction = odf.getOWLObjectIntersectionOf(unionOfAllPosTypeObjects, negateduUionOfAllNegTypeObjects);

        IRI objectPropIri = IRI.create(ConfigParams.namespace, "imageContains");
        OWLObjectProperty imgContains = odf.getOWLObjectProperty(objectPropIri);

        OWLClassExpression solClass = odf.getOWLObjectSomeValuesFrom(imgContains, conjunction);

        System.out.println("DL Syntax: " + dlRenderer.render(solClass));
        System.out.println("non dl: "+ solClass);

    }

    public static void main(String[] args) {
        testDlSyntax();
    }
}
