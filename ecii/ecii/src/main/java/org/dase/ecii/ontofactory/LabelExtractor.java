package org.dase.ecii.ontofactory;

import org.semanticweb.owlapi.model.*;

import javax.annotation.Nullable;

/**
 * Simple visitor that grabs any labels on an entity.
 *
 * @author Sean Bechhofer, The University Of Manchester, Information Management Group
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class LabelExtractor implements OWLAnnotationObjectVisitor {

    protected @Nullable
    String result = null;

    @Override
    public void visit(OWLAnnotation node) {
        /*
         * If it's a label, grab it as the result. Note that if there are
         * multiple labels, the last one will be used.
         */
        if (node.getProperty().isLabel()) {
            OWLLiteral c = (OWLLiteral) node.getValue();
            result = c.getLiteral();
        }
    }

    public @Nullable
    String getResult() {
        return result;
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {


    }

    @Override
    public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {


    }

    @Override
    public void visit(OWLAnnotationPropertyDomainAxiom axiom) {


    }

    @Override
    public void visit(OWLAnnotationPropertyRangeAxiom axiom) {


    }

    @Override
    public void visit(IRI iri) {


    }

    @Override
    public void visit(OWLAnonymousIndividual individual) {


    }

    @Override
    public void visit(OWLLiteral literal) {


    }
}
