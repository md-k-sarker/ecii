//package org.dase.util;
///*
//Written by sarker.
//Written at 5/23/18.
//*/
//
//import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
//import org.semanticweb.owlapi.formats.PrefixDocumentFormatImpl;
//import org.semanticweb.owlapi.model.OWLDocumentFormat;
//import org.semanticweb.owlapi.model.OWLNamedIndividual;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.PrintWriter;
//import java.lang.invoke.MethodHandles;
//import java.util.Map;
//
//public class ConfCreator {
//
//
//    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//    private
//
//    public ConfCreator(OWLOntology ontology){
//
//    }
//
//    public static PrefixDocumentFormat getPrefixOWLOntologyFormat(OWLOntology ontology) {
//        PrefixDocumentFormat prefixManager = null;
//        if (ontology != null) {
//            OWLOntologyManager manager = ontology.getOWLOntologyManager();
//            OWLDocumentFormat format = manager.getOntologyFormat(ontology);
//            if (format != null && format.isPrefixOWLDocumentFormat()) {
//                prefixManager = format.asPrefixOWLDocumentFormat();
//            }
//        }
//        if (prefixManager == null) {
//            prefixManager = new PrefixDocumentFormatImpl();
//        }
//        return prefixManager;
//    }
//
//    public void writeforConfigFiles(OWLOntology ontology, String confFileWritePath) {
//        StringBuilder sbuilderPositive = new StringBuilder();
//        StringBuilder sbuilderNegative = new StringBuilder();
//        StringBuilder sbuilderPrefixes = new StringBuilder();
//        sbuilderPositive.append("{");
//        sbuilderNegative.append("{");
//        sbuilderPrefixes.append("[");
//
//        boolean firstofPos = true;
//        boolean firstofNeg = true;
//        for (OWLNamedIndividual indiv : ontology.getIndividualsInSignature()) {
//            String name = indiv.getIRI().getRemainder().get();
//
//            if (name.startsWith(positiveIndividualInitial)) {
//                if (firstofPos) {
//                    firstofPos = false;
//                    sbuilderPositive.append("\"ex:" + name + "\"");
//                } else {
//                    sbuilderPositive.append(", " + "\"ex:" + name + "\"");
//                }
//            } else {
//                if (firstofNeg) {
//                    firstofNeg = false;
//                    sbuilderNegative.append("\"ex:" + name + "\"");
//                } else {
//                    sbuilderNegative.append(", " + "\"ex:" + name + "\"");
//                }
//            }
//        }
//
//        // write prefixes
//        boolean firstofPrefix = true;
//        PrefixDocumentFormat prefixManager = getPrefixOWLOntologyFormat(ontology);
//        Map<String, String> prefixNameMapping = prefixManager.getPrefixName2PrefixMap();
//
//        for (String prefix : prefixNameMapping.keySet()) {
//            if (firstofPrefix) {
//                firstofPrefix = false;
//                sbuilderPrefixes.append("(\"ex\"," + "\"" + prefixNameMapping.get(prefix) + "\")");
//            } else {
//                sbuilderPrefixes.append(", (\"ex\"," + "\"" + prefixNameMapping.get(prefix) + "\")");
//            }
//        }
//
//        sbuilderPositive.append("}");
//        sbuilderNegative.append("}");
//        sbuilderPrefixes.append("]");
//
//        try {
//            // write positive
//            PrintWriter writer = new PrintWriter(confFileWritePath);
//            writer.append(sbuilderPositive);
//            writer.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
//}
